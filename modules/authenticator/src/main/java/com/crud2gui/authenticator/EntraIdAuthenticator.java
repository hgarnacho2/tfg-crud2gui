package com.crud2gui.authenticator;

import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.OpenBrowserAction;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.SystemBrowserOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Autenticacion interactiva de escritorio con Entra ID mediante navegador y MSAL4J. */
public final class EntraIdAuthenticator {

    private static final int CALLBACK_TIMEOUT_SECONDS = 300;
    private static final String SUCCESS_PAGE = loadTemplate("/templates/auth_success_page.html");
    private static final String ERROR_PAGE = loadTemplate("/templates/auth_error_page.html");

    private final AuthenticatorConfig config;
    private final PublicClientApplication application;
    private final OpenBrowserAction embeddedBrowserAction;

    /**
     * Crea un cliente publico para el tenant configurado; no usa client secret.
     *
     * @param config parametros publicos de la aplicacion registrada
     * @throws MalformedURLException si la autoridad configurada no es valida
     */
    public EntraIdAuthenticator(AuthenticatorConfig config) throws MalformedURLException {
        this(config, null);
    }

    /**
     * @param config parametros publicos de la aplicacion registrada
     * @param embeddedBrowserAction accion que presenta la URL de autorizacion en una UI embebida
     * @throws MalformedURLException si la autoridad configurada no es valida
     */
    public EntraIdAuthenticator(AuthenticatorConfig config, OpenBrowserAction embeddedBrowserAction)
            throws MalformedURLException {
        this.config = Objects.requireNonNull(config, "config");
        if (config.browserMode() == AuthBrowserMode.EMBEDDED && embeddedBrowserAction == null) {
            throw new IllegalArgumentException("El modo embedded requiere una accion de navegador embebido.");
        }
        this.embeddedBrowserAction = embeddedBrowserAction;
        this.application = PublicClientApplication.builder(config.clientId())
                .authority(config.authority())
                .build();
    }

    /**
     * Abre el inicio de sesion interactivo en el navegador predeterminado.
     * La llamada es asincrona para que una GUI no bloquee su hilo de interfaz.
     *
     * @return futuro con la identidad y los roles declarados en el ID token
     */
    public CompletableFuture<AuthenticatedUser> authenticate() {
        SystemBrowserOptions.SystemBrowserOptionsBuilder browserOptions = SystemBrowserOptions.builder()
            .htmlMessageSuccess(SUCCESS_PAGE)
            .htmlMessageError(ERROR_PAGE);
        if (config.browserMode() == AuthBrowserMode.EMBEDDED) {
            browserOptions.openBrowserAction(embeddedBrowserAction);
        }
        InteractiveRequestParameters request = InteractiveRequestParameters.builder(config.redirectUri())
                .scopes(config.scopes())
                .httpPollingTimeoutInSeconds(CALLBACK_TIMEOUT_SECONDS)
            .systemBrowserOptions(browserOptions.build())
                .build();
        return application.acquireToken(request)
                .thenApply(result -> IdTokenClaims.parse(result.idToken()));
    }

    static String loadTemplate(String resourcePath) {
        try (InputStream input = EntraIdAuthenticator.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("No se encontro la plantilla HTML: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la plantilla HTML: " + resourcePath, e);
        }
    }
}

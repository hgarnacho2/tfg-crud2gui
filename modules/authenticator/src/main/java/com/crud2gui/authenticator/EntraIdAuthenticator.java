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

/**
 * Servicio de autenticación interactiva para una aplicación JavaFX de escritorio.
 * Configura MSAL4J como cliente público y presenta la autorización de Entra ID mediante la acción embebida
 * proporcionada por la interfaz; el flujo usa el redirect URI loopback configurado y no expone tokens al llamador.
 */
public final class EntraIdAuthenticator {

    private static final int CALLBACK_TIMEOUT_SECONDS = 300;
    private static final String SUCCESS_PAGE = loadTemplate("/templates/auth_success_page.html");
    private static final String ERROR_PAGE = loadTemplate("/templates/auth_error_page.html");

    private final AuthenticatorConfig config;
    private final PublicClientApplication application;
    private final OpenBrowserAction browserAction;

    /**
    * Crea un cliente público que delega la presentación del login en la vista JavaFX.
     *
    * @param config configuración pública de Entra ID con client ID, tenant, redirect URI y scopes
    * @param browserAction acción que carga la URL de autorización en el WebView de la interfaz
    * @throws NullPointerException si la configuración o la acción de navegador son {@code null}
    * @throws MalformedURLException si la autoridad configurada no es válida
     */
    public EntraIdAuthenticator(AuthenticatorConfig config, OpenBrowserAction browserAction)
            throws MalformedURLException {
        this.config = Objects.requireNonNull(config, "config");
        this.browserAction = Objects.requireNonNull(browserAction, "browserAction");
        this.application = PublicClientApplication.builder(config.clientId())
                .authority(config.authority())
                .build();
    }

    /**
        * Inicia el flujo interactivo de MSAL4J y muestra la autorización mediante la acción embebida configurada.
        * La operación es asíncrona para no bloquear el hilo de interfaz; al completarse, interpreta el ID token
        * y devuelve únicamente la identidad y los roles de aplicación.
     *
        * @return futuro que se completa con la identidad autenticada, o excepcionalmente si falla el login,
        *         el callback o la lectura de claims
     */
    public CompletableFuture<AuthenticatedUser> authenticate() {
        SystemBrowserOptions.SystemBrowserOptionsBuilder browserOptions = SystemBrowserOptions.builder()
            .htmlMessageSuccess(SUCCESS_PAGE)
            .htmlMessageError(ERROR_PAGE);
        browserOptions.openBrowserAction(browserAction);
        InteractiveRequestParameters request = InteractiveRequestParameters.builder(config.redirectUri())
                .scopes(config.scopes())
                .httpPollingTimeoutInSeconds(CALLBACK_TIMEOUT_SECONDS)
            .systemBrowserOptions(browserOptions.build())
                .build();
        return application.acquireToken(request)
                .thenApply(result -> IdTokenClaims.parse(result.idToken()));
    }

    /**
     * Lee como UTF-8 una página local que MSAL muestra tras completar o fallar el callback.
     *
     * @param resourcePath ruta absoluta del recurso HTML dentro del classpath
     * @return contenido HTML de la plantilla
     * @throws IllegalStateException si el recurso no existe o no puede leerse
     */
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

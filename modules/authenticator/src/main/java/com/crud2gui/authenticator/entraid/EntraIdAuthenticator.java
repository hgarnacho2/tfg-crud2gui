package com.crud2gui.authenticator.entraid;

import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.OpenBrowserAction;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.SystemBrowserOptions;
import com.microsoft.aad.msal4j.Prompt;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio de autenticación interactiva para una aplicación JavaFX de escritorio.
 * Configura MSAL4J como cliente público y presenta la autorización de Entra ID mediante la acción embebida
 * proporcionada por la interfaz; el flujo usa el redirect URI loopback configurado y no expone tokens al llamador.
 */
public final class EntraIdAuthenticator {

    private static final int CALLBACK_TIMEOUT_SECONDS = 180;
    private static final String SUCCESS_PAGE_TEMPLATE = loadTemplate("/templates/auth_success_page.html");
    private static final String ERROR_PAGE = loadTemplate("/templates/auth_error_page.html");

    private final AuthenticatorConfig config;
    private final PublicClientApplication application;
    private final OpenBrowserAction browserAction;
    private final String successPage;

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
        this(config, browserAction, "Ya puedes volver a CRUD2GUI.");
    }

    /**
     * Crea el cliente público con un mensaje de éxito adecuado al navegador seleccionado por la interfaz.
     *
     * @param config configuración pública de Entra ID
     * @param browserAction acción que presenta la URL de autorización
     * @param successMessage texto mostrado al completar el callback
     * @throws NullPointerException si algún parámetro es {@code null}
     * @throws MalformedURLException si la autoridad configurada no es válida
     */
    public EntraIdAuthenticator(AuthenticatorConfig config, OpenBrowserAction browserAction, String successMessage)
            throws MalformedURLException {
        this.config = Objects.requireNonNull(config, "config");
        this.browserAction = Objects.requireNonNull(browserAction, "browserAction");
        this.successPage = renderSuccessPage(successMessage);
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
            .htmlMessageSuccess(successPage)
            .htmlMessageError(ERROR_PAGE);
        browserOptions.openBrowserAction(browserAction);
        InteractiveRequestParameters request = InteractiveRequestParameters.builder(config.redirectUri())
                .prompt(Prompt.LOGIN)
                .scopes(config.scopes())
                .httpPollingTimeoutInSeconds(CALLBACK_TIMEOUT_SECONDS)
                .systemBrowserOptions(browserOptions.build())
                .build();
        return application.acquireToken(request)
                .thenApply(result -> IdTokenClaims.parse(result.idToken()));
    }

    /**
     * Elimina las cuentas y tokens de la cache MSAL en memoria.
     *
     * @return futuro que finaliza al vaciar la cache; no cierra por si solo la sesion del navegador
     */
    public CompletableFuture<Void> clearAccounts() {
        return application.getAccounts().thenCompose(accounts -> CompletableFuture.allOf(
                accounts.stream().map(application::removeAccount).toArray(CompletableFuture[]::new)));
    }

    /**
     * Obtiene la URL de cierre de Microsoft con retorno al redirect URI registrado.
     *
     * @return endpoint de cierre de sesion del tenant
     */
    public URI logoutUri() {
        return logoutUri(true);
    }

    /**
     * Obtiene la URL de cierre de Microsoft, opcionalmente con retorno a la URI registrada.
     * El navegador del sistema no permite a la aplicación observar ese retorno.
     *
     * @param includePostLogoutRedirect si se debe incluir el retorno al callback
     * @return endpoint de cierre de sesión del tenant
     */
    public URI logoutUri(boolean includePostLogoutRedirect) {
        String endpoint = config.authority() + "/oauth2/v2.0/logout";
        if (!includePostLogoutRedirect) {
            return URI.create(endpoint);
        }
        return URI.create(endpoint + "?post_logout_redirect_uri="
                + URLEncoder.encode(config.redirectUri().toString(), StandardCharsets.UTF_8));
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

    static String renderSuccessPage(String message) {
        String escapedMessage = Objects.requireNonNull(message, "message")
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        return SUCCESS_PAGE_TEMPLATE.replace("{{SUCCESS_MESSAGE}}", escapedMessage);
    }
}

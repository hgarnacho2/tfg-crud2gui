package com.crud2gui.authenticator;

import com.microsoft.aad.msal4j.InteractiveRequestParameters;
import com.microsoft.aad.msal4j.PublicClientApplication;

import java.net.MalformedURLException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** Autenticacion interactiva de escritorio con Entra ID mediante navegador y MSAL4J. */
public final class EntraIdAuthenticator {

    private final AuthenticatorConfig config;
    private final PublicClientApplication application;

    /**
     * Crea un cliente publico para el tenant configurado; no usa client secret.
     *
     * @param config parametros publicos de la aplicacion registrada
     * @throws MalformedURLException si la autoridad configurada no es valida
     */
    public EntraIdAuthenticator(AuthenticatorConfig config) throws MalformedURLException {
        this.config = Objects.requireNonNull(config, "config");
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
        InteractiveRequestParameters request = InteractiveRequestParameters.builder(config.redirectUri())
                .scopes(config.scopes())
                .build();
        return application.acquireToken(request)
                .thenApply(result -> IdTokenClaims.parse(result.idToken()));
    }
}

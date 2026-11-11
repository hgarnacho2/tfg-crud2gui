package com.crud2gui.authenticator.entraid;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntraIdAuthenticatorTest {

    // Comprueba que el logout apunta al tenant correcto y conserva los parámetros del callback registrado.
    @Test
    void buildsTenantLogoutWithEncodedRegisteredReturn() throws Exception {
        var config = new AuthenticatorConfig("client-id", "tenant.example",
                URI.create("http://localhost:8765/callback?source=desktop"), Set.of("openid"));
        var authenticator = new EntraIdAuthenticator(config, url -> {});
        URI logout = authenticator.logoutUri();
        assertEquals("https", logout.getScheme());
        assertEquals("login.microsoftonline.com", logout.getHost());
        assertEquals("/tenant.example/oauth2/v2.0/logout", logout.getPath());
        assertEquals("post_logout_redirect_uri=" + config.redirectUri(),
                URLDecoder.decode(logout.getRawQuery(), StandardCharsets.UTF_8));
    }

    @Test
    void buildsTenantLogoutWithoutReturnForExternalBrowser() throws Exception {
        var config = new AuthenticatorConfig("client-id", "tenant.example",
                URI.create("http://localhost"), Set.of("openid"));
        var authenticator = new EntraIdAuthenticator(config, url -> {});

        URI logout = authenticator.logoutUri(false);

        assertEquals("/tenant.example/oauth2/v2.0/logout", logout.getPath());
        assertNull(logout.getRawQuery());
    }

    // Limpiar una caché vacía debe terminar sin lanzar una excepción.
    @Test
    void clearsAnEmptyCacheWithoutBrowserOrNetwork() throws Exception {
        var authenticator = new EntraIdAuthenticator(new AuthenticatorConfig("client-id",
                "tenant.example", URI.create("http://localhost"), Set.of("openid")), url -> {});
        assertDoesNotThrow(() -> authenticator.clearAccounts().get(5, TimeUnit.SECONDS));
    }
}

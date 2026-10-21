package com.crud2gui.authenticator;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatorConfigTest {

    @Test
    void buildsTenantAuthorityAndKeepsScopesImmutable() {
        AuthenticatorConfig config = new AuthenticatorConfig("client-id", "tenant.example",
                URI.create("http://localhost"), Set.of("openid", "profile"));

        assertEquals("https://login.microsoftonline.com/tenant.example", config.authority());
        assertEquals(AuthBrowserMode.SYSTEM, config.browserMode());
        assertThrows(UnsupportedOperationException.class, () -> config.scopes().add("User.Read"));
    }

    @Test
    void parsesConfiguredBrowserMode() {
        assertEquals(AuthBrowserMode.SYSTEM, AuthBrowserMode.parse(null));
        assertEquals(AuthBrowserMode.SYSTEM, AuthBrowserMode.parse("system"));
        assertEquals(AuthBrowserMode.EMBEDDED, AuthBrowserMode.parse("embedded"));
        assertThrows(IllegalArgumentException.class, () -> AuthBrowserMode.parse("unknown"));
    }

    @Test
    void embeddedModeKeepsTheRegisteredLoopbackRedirect() {
        AuthenticatorConfig config = new AuthenticatorConfig("client-id", "tenant.example",
                URI.create("http://localhost"), Set.of("openid"), AuthBrowserMode.EMBEDDED);

        assertEquals(AuthBrowserMode.EMBEDDED, config.browserMode());
        assertEquals(URI.create("http://localhost"), config.redirectUri());
    }

    @Test
    void onlyAcceptsLoopbackHttpRedirectsForDesktopClients() {
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatorConfig("client-id", "tenant",
                URI.create("https://example.com/callback"), Set.of("openid")));
    }
}

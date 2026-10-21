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
        assertThrows(UnsupportedOperationException.class, () -> config.scopes().add("User.Read"));
    }

    @Test
    void onlyAcceptsLoopbackHttpRedirectsForDesktopClients() {
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatorConfig("client-id", "tenant",
                URI.create("https://example.com/callback"), Set.of("openid")));
    }
}

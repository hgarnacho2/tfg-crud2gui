package com.crud2gui.authenticator.entraid;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica la construcción y las validaciones principales de la configuración de autenticación
 * de Entra ID para un cliente público de escritorio.
 */
class AuthenticatorConfigTest {

    /**
     * Comprueba que la autoridad incluya el tenant configurado y que los scopes expuestos
     * no puedan modificarse después de crear la configuración.
     */
    @Test
    void buildsTenantAuthorityAndKeepsScopesImmutable() {
        AuthenticatorConfig config = new AuthenticatorConfig("client-id", "tenant.example",
                URI.create("http://localhost"), Set.of("openid", "profile"));

        assertEquals("https://login.microsoftonline.com/tenant.example", config.authority());
        assertThrows(UnsupportedOperationException.class, () -> config.scopes().add("User.Read"));
    }

    /**
     * Comprueba que se rechace una URI de retorno HTTPS alojada en un servidor remoto,
     * ya que este cliente de escritorio requiere un callback HTTP de loopback.
     */
    @Test
    void onlyAcceptsLoopbackHttpRedirectsForDesktopClients() {
        assertThrows(IllegalArgumentException.class, () -> new AuthenticatorConfig("client-id", "tenant",
                URI.create("https://example.com/callback"), Set.of("openid")));
    }
}

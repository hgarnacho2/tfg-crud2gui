package com.crud2gui.authenticator.entraid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPageTemplatesTest {

    // Verifica que las páginas locales de éxito y error se cargan desde los recursos del classpath.
    @Test
    void loadsSuccessAndErrorPagesFromClasspathResources() {
        String success = EntraIdAuthenticator.loadTemplate("/templates/auth_success_page.html");
        String error = EntraIdAuthenticator.loadTemplate("/templates/auth_error_page.html");

        assertTrue(success.contains("Autenticación completada"));
        assertTrue(error.contains("No se pudo completar la autenticación"));
    }
}

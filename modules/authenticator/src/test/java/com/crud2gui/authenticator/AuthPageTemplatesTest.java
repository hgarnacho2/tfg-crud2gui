package com.crud2gui.authenticator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPageTemplatesTest {

    @Test
    void loadsSuccessAndErrorPagesFromClasspathResources() {
        String success = EntraIdAuthenticator.loadTemplate("/templates/auth_success_page.html");
        String error = EntraIdAuthenticator.loadTemplate("/templates/auth_error_page.html");

        assertTrue(success.contains("Autenticación completada"));
        assertTrue(error.contains("No se pudo completar la autenticación"));
    }
}

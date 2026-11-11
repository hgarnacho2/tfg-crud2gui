package com.crud2gui.authenticator.entraid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthPageTemplatesTest {

    // Verifica que las páginas locales de éxito y error se cargan desde los recursos del classpath.
    @Test
    void loadsSuccessAndErrorPagesFromClasspathResources() {
        String success = EntraIdAuthenticator.loadTemplate("/templates/auth_success_page.html");
        String error = EntraIdAuthenticator.loadTemplate("/templates/auth_error_page.html");

        assertTrue(success.contains("Autenticación completada"));
        assertTrue(success.contains("{{SUCCESS_MESSAGE}}"));
        assertTrue(error.contains("No se pudo completar la autenticación"));
    }

    @Test
    void rendersTheSelectedBrowserMessageAndEscapesHtml() {
        String systemMessage = EntraIdAuthenticator.renderSuccessPage(
                "Ya puedes volver a CRUD2GUI y cerrar esta pestaña.");
        String webViewMessage = EntraIdAuthenticator.renderSuccessPage(
                "La aplicación continuará automáticamente.");
        String escapedMessage = EntraIdAuthenticator.renderSuccessPage("<script>alert('x')</script>");

        assertTrue(systemMessage.contains("Ya puedes volver a CRUD2GUI y cerrar esta pestaña."));
        assertTrue(webViewMessage.contains("La aplicación continuará automáticamente."));
        assertFalse(escapedMessage.contains("<script>"));
        assertTrue(escapedMessage.contains("&lt;script&gt;"));
    }
}

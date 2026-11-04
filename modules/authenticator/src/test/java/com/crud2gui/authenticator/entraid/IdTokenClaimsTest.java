package com.crud2gui.authenticator.entraid;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdTokenClaimsTest {

    // Comprueba la conversión de los claims de identidad y roles al modelo de sesión de la aplicación.
    @Test
    void readsIdentityAndAssignedApplicationRoles() {
        // oid debe tener prioridad sobre sub; la comprobación de roles no distingue mayúsculas.
        String token = token("""
                {"oid":"object-1","sub":"subject-1","tid":"tenant-1",
                 "preferred_username":"alumno@example.test","name":"Alumno Test",
                 "roles":["alumno"],"exp":1893456000}
                """);

        AuthenticatedUser user = IdTokenClaims.parse(token);

        assertEquals("object-1", user.objectId());
        assertEquals("alumno@example.test", user.username());
        assertEquals("Alumno Test", user.displayName());
        assertEquals("tenant-1", user.tenantId());
        assertTrue(user.isAlumno());
        assertFalse(user.isDocente());
        assertTrue(user.hasRole("ALUMNO"));
        assertEquals(Instant.ofEpochSecond(1893456000), user.expiresAt());
    }

    // El parser debe rechazar una cadena que no tenga formato JWT.
    @Test
    void rejectsMalformedTokens() {
        assertThrows(IllegalArgumentException.class, () -> IdTokenClaims.parse("not-a-jwt"));
    }

    private static String token(String payload) {
        // Este test cubre la lectura del payload; la autenticidad del ID token la valida MSAL.
        String encoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "header." + encoded + ".signature";
    }
}

package com.crud2gui.authenticator;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Identidad autenticada y roles de aplicacion del usuario; no conserva tokens ni credenciales. */
public record AuthenticatedUser(String objectId, String username, String displayName,
                                String tenantId, Set<String> roles, Instant expiresAt) {

    public AuthenticatedUser {
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("El token no contiene oid ni sub");
        }
        roles = roles == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(roles));
    }

    /** Comprueba un rol sin distinguir mayusculas y minusculas. */
    public boolean hasRole(String role) {
        return role != null && roles.stream().anyMatch(existing -> existing.equalsIgnoreCase(role));
    }

    public boolean isAlumno() {
        return hasRole("alumno");
    }

    public boolean isDocente() {
        return hasRole("docente");
    }
}

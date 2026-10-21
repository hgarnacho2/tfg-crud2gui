package com.crud2gui.authenticator;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Identidad y permisos de aplicación del usuario que ha completado el inicio de sesión en Entra ID.
 * Representa el estado de sesión de la aplicación; no contiene ni almacena tokens o credenciales.
 *
 * @param objectId identificador estable del usuario dentro del tenant, obtenido de {@code oid} o {@code sub}
 * @param username nombre de inicio de sesión o correo electrónico, si está presente en el token
 * @param displayName nombre visible del usuario, si está presente en el token
 * @param tenantId identificador del tenant que emitió el token
 * @param roles roles de aplicación asignados al usuario; la colección se copia y se hace inmutable
 * @param expiresAt instante de expiración del token de identidad, si está disponible
 */
public record AuthenticatedUser(String objectId, String username, String displayName,
                                String tenantId, Set<String> roles, Instant expiresAt) {

     /**
      * Valida la identidad y normaliza la colección de roles.
      *
      * @throws IllegalArgumentException si no se recibió un identificador de usuario
      */
    public AuthenticatedUser {
        if (objectId == null || objectId.isBlank()) {
            throw new IllegalArgumentException("El token no contiene oid ni sub");
        }
        roles = roles == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(roles));
    }

    /**
     * Comprueba si el usuario tiene un rol, sin distinguir mayúsculas y minúsculas.
     *
     * @param role nombre del rol que se desea comprobar
     * @return {@code true} si el usuario tiene ese rol; {@code false} si no lo tiene o el argumento es {@code null}
     */
    public boolean hasRole(String role) {
        return role != null && roles.stream().anyMatch(existing -> existing.equalsIgnoreCase(role));
    }

    /**
     * Comprueba si el usuario tiene el rol de alumno.
     *
     * @return {@code true} si tiene asignado el rol {@code alumno}
     */
    public boolean isAlumno() {
        return hasRole("alumno");
    }

    /**
     * Comprueba si el usuario tiene el rol de docente.
     *
     * @return {@code true} si tiene asignado el rol {@code docente}
     */
    public boolean isDocente() {
        return hasRole("docente");
    }
}

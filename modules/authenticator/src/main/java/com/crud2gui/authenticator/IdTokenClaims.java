package com.crud2gui.authenticator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Convierte los claims del ID token devuelto por MSAL en el modelo de identidad de la aplicación.
 * Extrae identificador, usuario, nombre visible, tenant, roles y expiración. No valida la firma del JWT;
 * debe recibir únicamente el ID token obtenido por el flujo autenticado de MSAL.
 */
final class IdTokenClaims {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Evita crear instancias de esta clase de utilidades. */
    private IdTokenClaims() { }

    /**
     * Decodifica el payload de un ID token y construye el usuario autenticado.
     * Usa {@code oid} o {@code sub} como identificador, y busca el nombre de usuario en
     * {@code preferred_username}, {@code upn} o {@code email}, en ese orden.
     *
     * @param idToken JWT de identidad devuelto por MSAL tras completar la autenticación
     * @return identidad normalizada con los roles y la expiración disponibles en el token
     * @throws IllegalArgumentException si el token está vacío, no tiene formato JWT, no se puede leer
     *                                  o no contiene un identificador de usuario
     */
    static AuthenticatedUser parse(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("MSAL no devolvio un ID token");
        }
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("El ID token no tiene formato JWT");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode claims = MAPPER.readTree(new String(payload, StandardCharsets.UTF_8));
            String objectId = firstText(claims, "oid", "sub");
            String username = firstText(claims, "preferred_username", "upn", "email");
            String displayName = firstText(claims, "name");
            if (displayName == null || displayName.isBlank()) displayName = username;
            Set<String> roles = new LinkedHashSet<>();
            JsonNode roleClaims = claims.path("roles");
            if (roleClaims.isArray()) {
                roleClaims.forEach(role -> {
                    if (role.isTextual() && !role.asText().isBlank()) roles.add(role.asText());
                });
            }
            JsonNode exp = claims.path("exp");
            Instant expiresAt = exp.canConvertToLong() ? Instant.ofEpochSecond(exp.asLong()) : null;
            return new AuthenticatedUser(objectId, username, displayName,
                    firstText(claims, "tid"), roles, expiresAt);
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalArgumentException("No se pudieron leer los claims del ID token", e);
        }
    }

    /**
     * Devuelve el primer claim textual no vacío de una lista de nombres alternativos.
     *
     * @param claims objeto JSON con los claims del token
     * @param names nombres de claim que se prueban en orden de prioridad
     * @return valor del primer claim textual no vacío, o {@code null} si ninguno existe
     */
    private static String firstText(JsonNode claims, String... names) {
        for (String name : names) {
            JsonNode value = claims.path(name);
            if (value.isTextual() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }
}

package com.crud2gui.authenticator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/** Extrae identidad y roles del ID token que MSAL ya recibio en el flujo autenticado. */
final class IdTokenClaims {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IdTokenClaims() { }

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

    private static String firstText(JsonNode claims, String... names) {
        for (String name : names) {
            JsonNode value = claims.path(name);
            if (value.isTextual() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }
}

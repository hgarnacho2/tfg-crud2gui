package com.crud2gui.authenticator;

import com.crud2gui.common.DotEnv;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Configuracion publica para iniciar sesion en Microsoft Entra ID desde una aplicacion de escritorio. */
public record AuthenticatorConfig(String clientId, String tenantId, URI redirectUri, Set<String> scopes,
                                  AuthBrowserMode browserMode) {

    public static final String DEFAULT_REDIRECT_URI = "http://localhost";
    public static final String DEFAULT_SCOPES = "openid profile email";

    public AuthenticatorConfig {
        clientId = required(clientId, "clientId");
        tenantId = required(tenantId, "tenantId");
        if (redirectUri == null || !isLoopbackRedirect(redirectUri)) {
            throw new IllegalArgumentException("redirectUri debe ser una URI HTTP de loopback, como http://localhost");
        }
        browserMode = browserMode == null ? AuthBrowserMode.SYSTEM : browserMode;
        scopes = scopes == null ? Set.of() : scopes.stream()
                .filter(scope -> scope != null && !scope.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("Debe indicarse al menos un scope");
        }
        scopes = Collections.unmodifiableSet(scopes);
    }

    public AuthenticatorConfig(String clientId, String tenantId, URI redirectUri, Set<String> scopes) {
        this(clientId, tenantId, redirectUri, scopes, AuthBrowserMode.SYSTEM);
    }

    /** Lee la configuracion con precedencia de entorno, propiedades Java y archivos .env del repositorio. */
    public static AuthenticatorConfig fromEnvironment() {
        String clientId = DotEnv.getStringVar("AZURE_CLIENT_ID", "");
        String tenantId = DotEnv.getStringVar("AZURE_TENANT_ID", "");
        URI redirectUri = URI.create(DotEnv.getStringVar("AZURE_REDIRECT_URI", DEFAULT_REDIRECT_URI));
        Set<String> scopes = parseScopes(DotEnv.getStringVar("AZURE_SCOPES", DEFAULT_SCOPES));
        AuthBrowserMode browserMode = AuthBrowserMode.parse(DotEnv.getStringVar("AZURE_AUTH_BROWSER_MODE", "system"));
        return new AuthenticatorConfig(clientId, tenantId, redirectUri, scopes, browserMode);
    }

    /** @return autoridad de Entra limitada al tenant configurado */
    public String authority() {
        return "https://login.microsoftonline.com/" + tenantId;
    }

    static Set<String> parseScopes(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.trim().split("[,\\s]+"))
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta configurar " + name);
        }
        return value.strip();
    }

    private static boolean isLoopbackRedirect(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (!"http".equalsIgnoreCase(scheme) || host == null || uri.getUserInfo() != null
                || uri.getFragment() != null) {
            return false;
        }
        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")
                || host.equals("::1") || host.equals("[::1]");
    }
}

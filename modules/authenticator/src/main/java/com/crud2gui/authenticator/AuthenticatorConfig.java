package com.crud2gui.authenticator;

import com.crud2gui.common.DotEnv;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuración pública e inmutable necesaria para iniciar sesión en Microsoft Entra ID desde una app de escritorio.
 * Contiene únicamente identificadores y parámetros OAuth; no almacena secretos, credenciales ni tokens.
 *
 * @param clientId Application (client) ID del registro público de Entra ID
 * @param tenantId ID o dominio del tenant que emite la autenticación
 * @param redirectUri URI HTTP de loopback a la que Entra devuelve el resultado interactivo
 * @param scopes permisos OIDC/API solicitados; se copian y quedan inmutables
 */
public record AuthenticatorConfig(String clientId, String tenantId, URI redirectUri, Set<String> scopes) {

    public static final String DEFAULT_REDIRECT_URI = "http://localhost";
    public static final String DEFAULT_SCOPES = "openid profile email";

    /**
     * Valida los parámetros y normaliza los scopes para evitar valores nulos, vacíos o mutables.
     *
     * @throws IllegalArgumentException si faltan el client ID o tenant, el redirect no es loopback,
     *                                  o no se proporciona ningún scope válido
     */
    public AuthenticatorConfig {
        clientId = required(clientId, "clientId");
        tenantId = required(tenantId, "tenantId");
        if (redirectUri == null || !isLoopbackRedirect(redirectUri)) {
            throw new IllegalArgumentException("redirectUri debe ser una URI HTTP de loopback, como http://localhost");
        }
        scopes = scopes == null ? Set.of() : scopes.stream()
                .filter(scope -> scope != null && !scope.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (scopes.isEmpty()) {
            throw new IllegalArgumentException("Debe indicarse al menos un scope");
        }
        scopes = Collections.unmodifiableSet(scopes);
    }

    /**
     * Carga la configuración mediante {@link DotEnv}, que prioriza variables de entorno, propiedades Java,
     * {@code .env.private} y finalmente {@code .env.public}. Si no se configuran, usa los defaults declarados aquí.
     *
     * @return configuración validada para el cliente Entra actual
     * @throws IllegalArgumentException si falta un dato obligatorio o alguno de los valores no es válido
     */
    public static AuthenticatorConfig fromEnvironment() {
        String clientId = DotEnv.getStringVar("AZURE_CLIENT_ID", "");
        String tenantId = DotEnv.getStringVar("AZURE_TENANT_ID", "");
        URI redirectUri = URI.create(DotEnv.getStringVar("AZURE_REDIRECT_URI", DEFAULT_REDIRECT_URI));
        Set<String> scopes = parseScopes(DotEnv.getStringVar("AZURE_SCOPES", DEFAULT_SCOPES));
        return new AuthenticatorConfig(clientId, tenantId, redirectUri, scopes);
    }

    /**
     * Construye la autoridad OAuth de Microsoft Entra ID restringida al tenant configurado.
     *
     * @return URL base de autoridad, con el tenant como segmento final
     */
    public String authority() {
        return "https://login.microsoftonline.com/" + tenantId;
    }

    /**
     * Separa scopes delimitados por comas o espacios y descarta entradas vacías.
     *
     * @param value texto de scopes leído de la configuración
     * @return conjunto ordenado de scopes, o conjunto vacío si el texto es nulo o blanco
     */
    static Set<String> parseScopes(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.trim().split("[,\\s]+"))
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Rechaza valores de configuración obligatorios nulos o en blanco y elimina espacios exteriores.
     *
     * @param value valor leído de la configuración
     * @param name nombre del parámetro, usado en el mensaje de error
     * @return valor sin espacios exteriores
     * @throws IllegalArgumentException si el valor es nulo o está en blanco
     */
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta configurar " + name);
        }
        return value.strip();
    }

    /**
     * Comprueba que el callback use HTTP y vuelva a la propia máquina, como requiere el flujo desktop loopback.
     *
     * @param uri URI de callback que se va a validar
     * @return {@code true} para localhost o una dirección loopback IPv4/IPv6 sin credenciales ni fragmento
     */
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

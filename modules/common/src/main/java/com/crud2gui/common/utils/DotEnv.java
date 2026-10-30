package com.crud2gui.common.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lector de ficheros {@code .env} ({@code CLAVE=valor}) y gestor de configuración jerárquica.
 */
public final class DotEnv {

    private DotEnv() { }

    private static Boolean debugEnabledCache = null;

    /**
     * Comprueba si el modo de depuración de DotEnv está activado.
     * Soporta: Variable de entorno, Propiedad Java (-D) o clave DOTENV_DEBUG en .env.public
     */
    private static boolean isDebugEnabled() {
        if (debugEnabledCache != null) {
            return debugEnabledCache;
        }

        // 1. Comprobar variable de entorno
        String val = System.getenv("DOTENV_DEBUG");
        
        // 2. Comprobar propiedad de sistema Java (-DDOTENV_DEBUG=true)
        if (val == null || val.isBlank()) {
            val = System.getProperty("DOTENV_DEBUG");
        }

        // 3. Comprobar dentro de los propios ficheros .env si no se indicó antes
        if (val == null || val.isBlank()) {
            try {
                Path resolvedDir = resolveProjectDefaultsDir();
                Map<String, String> map = loadDefaultsIn(resolvedDir);
                val = map.get("DOTENV_DEBUG");
            } catch (Exception ignored) { }
        }

        debugEnabledCache = "true".equalsIgnoreCase(val);
        return debugEnabledCache;
    }

    private static void logDebug(String message) {
        if (isDebugEnabled()) {
            System.out.println("[DotEnv] [DEBUG] " + message);
        }
    }

    /**
     * Resuelve una propiedad de tipo texto (String) siguiendo la jerarquía estricta.
     */
    public static String getStringVar(String clave, String defaultValue) {
        // 1. Comprobar variable de entorno del sistema operativo
        String envVal = System.getenv(clave);
        if (envVal != null && !envVal.isBlank()) {
            logDebug("Clave '" + clave + "' encontrada en variable de entorno del S.O. Valor: '" + envVal.trim() + "'");
            return envVal.trim();
        }

        // 2. Comprobar propiedad de sistema de Java (-Dkey=...)
        String propVal = System.getProperty(clave);
        if (propVal != null && !propVal.isBlank()) {
            logDebug("Clave '" + clave + "' encontrada en propiedad del sistema Java (-D). Valor: '" + propVal.trim() + "'");
            return propVal.trim();
        }

        // 3. Cargar desde los ficheros de entorno del proyecto
        try {
            Path resolvedDir = resolveProjectDefaultsDir();
            Path publicFile = resolvedDir.resolve(".env.public");
            Path privateFile = resolvedDir.resolve(".env.private");

            logDebug("Consultando ficheros de entorno en directorio base: " + resolvedDir.toAbsolutePath());
            logDebug("  -> Buscando public: " + publicFile.toAbsolutePath() + " (Existe: " + Files.exists(publicFile) + ")");
            logDebug("  -> Buscando private: " + privateFile.toAbsolutePath() + " (Existe: " + Files.exists(privateFile) + ")");

            Map<String, String> dotEnvMap = load(publicFile, privateFile);
            if (dotEnvMap.containsKey(clave)) {
                String val = dotEnvMap.get(clave);
                if (val != null && !val.isBlank()) {
                    logDebug("Clave '" + clave + "' encontrada con éxito. Valor: '" + val.trim() + "'");
                    return val.trim();
                }
            } else {
                logDebug("Clave '" + clave + "' no está presente en los ficheros consultados.");
            }
        } catch (IOException e) {
            logDebug("Error leyendo los ficheros de entorno para la clave '" + clave + "': " + e.getMessage());
        }

        // 4. Valor por defecto
        logDebug("Clave '" + clave + "' no encontrada en ninguna fuente. Aplicando valor por defecto: " + defaultValue);
        return defaultValue;
    }

    public static boolean getBooleanVar(String clave, boolean defaultValue) {
        String val = getStringVar(clave, null);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }

    // --- Métodos de carga y resolución ---

    public static Map<String, String> load(Path... files) throws IOException {
        Map<String, String> mergedValues = new LinkedHashMap<>();
        if (files == null) {
            return mergedValues;
        }
        for (Path file : files) {
            mergedValues.putAll(loadSingle(file));
        }
        return mergedValues;
    }

    public static Map<String, String> loadDefaultsIn(Path baseDir) throws IOException {
        Path publicFile = baseDir.resolve(".env.public");
        Path privateFile = baseDir.resolve(".env.private");
        return load(publicFile, privateFile);
    }

    private static Path resolveProjectDefaultsDir() {
        try {
            var resourceUrl = DotEnv.class.getClassLoader().getResource(".env.public");
            if (resourceUrl != null) {
                Path resourcePath = Path.of(resourceUrl.toURI());
                if (Files.isRegularFile(resourcePath)) {
                    return resourcePath.getParent();
                }
            }
        } catch (Exception ignored) { }
        
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            Path publicFile = current.resolve(".env.public");
            Path rootPom = current.resolve("pom.xml");
            
            if (Files.exists(publicFile) || (Files.exists(rootPom) && Files.exists(current.resolve("modules")))) {
                if (Files.exists(publicFile)) {
                    return current;
                }
                return current;
            }
            current = current.getParent();
        }
        
        return Path.of(".").toAbsolutePath().normalize();
    }

    public static Map<String, String> loadProjectDefaults() throws IOException {
        Path resolvedDir = resolveProjectDefaultsDir();
        return loadDefaultsIn(resolvedDir);
    }

    private static Map<String, String> loadSingle(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.replace("\uFEFF", "").strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).stripLeading();
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).strip();
            String value = line.substring(equals + 1).strip();
            if (value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(key, value);
        }
        return values;
    }
}
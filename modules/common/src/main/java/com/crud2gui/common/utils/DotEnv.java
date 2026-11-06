package com.crud2gui.common.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lee ficheros {@code .env} y resuelve configuración desde variables de entorno,
 * propiedades Java y ficheros públicos y privados del proyecto.
 *
 * <p>Al combinar ficheros, se procesan en el orden recibido y los posteriores
 * sobrescriben las claves anteriores.</p>
 */
public final class DotEnv {

    private DotEnv() { }

    private static Boolean debugEnabledCache = null;

    /**
     * Comprueba si el modo de depuración está activado y almacena el resultado para este proceso.
     * La primera consulta busca {@code DOTENV_DEBUG} en el entorno del sistema, las propiedades
     * Java y los ficheros del proyecto, en ese orden.
     *
     * @return {@code true} si el primer valor encontrado es {@code true}, sin distinguir mayúsculas
     */
    private static boolean isDebugEnabled() {
        if (debugEnabledCache != null) {
            return debugEnabledCache;
        }

        String val = System.getenv("DOTENV_DEBUG");

        if (val == null || val.isBlank()) {
            val = System.getProperty("DOTENV_DEBUG");
        }

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

    /**
     * Escribe un mensaje de diagnóstico cuando {@code DOTENV_DEBUG} está activado.
     *
     * @param message mensaje que se registra
     */
    private static void logDebug(String message) {
        if (isDebugEnabled()) {
            System.out.println("[DotEnv] [DEBUG] " + message);
        }
    }

    /**
     * Resuelve una propiedad de texto siguiendo su precedencia de configuración.
     * Busca primero en las variables de entorno del sistema, después en propiedades Java
     * {@code -D}, luego en {@code .env.public} y {@code .env.private}, y finalmente usa el valor
     * predeterminado. Los valores encontrados se devuelven sin espacios exteriores.
     *
     * @param clave nombre de la propiedad
     * @param defaultValue valor usado si la clave no aparece o está vacía en todas las fuentes
     * @return valor configurado o {@code defaultValue}
     */
    public static String getStringVar(String clave, String defaultValue) {
        String envVal = System.getenv(clave);
        if (envVal != null && !envVal.isBlank()) {
            logDebug("Clave '" + clave + "' encontrada en variable de entorno del S.O. Valor: '"
                    + diagnosticValue(clave, envVal) + "'");
            return envVal.trim();
        }

        String propVal = System.getProperty(clave);
        if (propVal != null && !propVal.isBlank()) {
            logDebug("Clave '" + clave + "' encontrada en propiedad del sistema Java (-D). Valor: '"
                    + diagnosticValue(clave, propVal) + "'");
            return propVal.trim();
        }

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
                    logDebug("Clave '" + clave + "' encontrada con éxito. Valor: '"
                            + diagnosticValue(clave, val) + "'");
                    return val.trim();
                }
            } else {
                logDebug("Clave '" + clave + "' no está presente en los ficheros consultados.");
            }
        } catch (IOException e) {
            logDebug("Error leyendo los ficheros de entorno para la clave '" + clave + "': " + e.getMessage());
        }

        logDebug("Clave '" + clave + "' no encontrada en ninguna fuente. Aplicando valor por defecto: "
                + diagnosticValue(clave, defaultValue));
        return defaultValue;
    }

    static String diagnosticValue(String key, String value) {
        if (value == null) {
            return "null";
        }
        String normalizedKey = key == null ? "" : key.toUpperCase(Locale.ROOT);
        boolean sensitive = normalizedKey.contains("KEY") || normalizedKey.contains("TOKEN")
                || normalizedKey.contains("SECRET") || normalizedKey.contains("PASSWORD")
                || normalizedKey.contains("CREDENTIAL") || normalizedKey.contains("PRIVATE");
        return sensitive ? "<redacted>" : value.trim();
    }

    /**
     * Resuelve una propiedad como booleano usando {@link #getStringVar(String, String)}.
     * Si no encuentra valor, devuelve el predeterminado; si lo encuentra, aplica
     * {@link Boolean#parseBoolean(String)}.
     *
     * @param clave nombre de la propiedad
     * @param defaultValue valor usado si la clave no está configurada
     * @return valor booleano resuelto
     */
    public static boolean getBooleanVar(String clave, boolean defaultValue) {
        String val = getStringVar(clave, null);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }

    /**
     * Carga y combina una secuencia de ficheros {@code .env}.
     * Los ficheros inexistentes se ignoran; si una clave se repite, prevalece el último valor.
     *
     * @param files rutas de los ficheros en orden de precedencia
     * @return mapa con las claves combinadas, vacío si {@code files} es {@code null}
     * @throws IOException si no se puede leer alguno de los ficheros existentes
     */
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

    /**
     * Carga {@code .env.public} y después {@code .env.private} desde un directorio.
     * El fichero privado es opcional y sus claves sobrescriben las públicas.
     *
     * @param baseDir directorio que contiene los ficheros
     * @return mapa con los valores combinados
     * @throws IOException si un fichero existente no se puede leer
     */
    public static Map<String, String> loadDefaultsIn(Path baseDir) throws IOException {
        Path publicFile = baseDir.resolve(".env.public");
        Path privateFile = baseDir.resolve(".env.private");
        return load(publicFile, privateFile);
    }

    /**
     * Busca la configuración en los recursos empaquetados o ascendiendo desde {@code user.dir}.
     * Se detiene al encontrar {@code .env.public} o la raíz de un proyecto Maven multimódulo;
     * si no encuentra ninguna, usa el directorio actual.
     *
     * @return directorio donde buscar los ficheros de configuración
     */
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

    /**
     * Detecta el directorio del proyecto y carga sus ficheros de configuración predeterminados.
     *
     * @return mapa combinado de {@code .env.public} y {@code .env.private}
     * @throws IOException si un fichero existente no se puede leer
     */
    public static Map<String, String> loadProjectDefaults() throws IOException {
        Path resolvedDir = resolveProjectDefaultsDir();
        return loadDefaultsIn(resolvedDir);
    }

    /**
     * Interpreta un fichero {@code .env}: ignora líneas vacías y comentarios, admite el prefijo
     * {@code export}, elimina BOM y comillas envolventes, y separa cada clave en el primer {@code =}.
     *
     * @param file ruta del fichero; si no es un fichero regular, se devuelve un mapa vacío
     * @return valores encontrados en el fichero
     * @throws IOException si no se puede leer un fichero regular
     */
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
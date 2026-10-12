package com.crud2gui.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lector de ficheros {@code .env} ({@code CLAVE=valor}).
 * Soporta múltiples ficheros, mezcla de configuraciones (ej. public y private)
 * y búsqueda automática en la raíz del proyecto.
 */
public final class DotEnv {

    private DotEnv() { }

    /**
     * Carga y mezcla múltiples ficheros de entorno en orden secuencial.
     * Si una clave se repite en un fichero posterior, sobrescribe el valor anterior.
     *
     * @param files rutas de los ficheros a cargar (numero variable de argumentos)
     * @return mapa consolidado con todas las claves y valores (Mergeo de ficheros .env.public y .env.private)
     * @throws IOException si alguno de los ficheros existe pero no se puede leer
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
     * Busca automáticamente en el directorio base los ficheros {@code env.public} y {@code env.private} 
     * (si existen) y los mezcla, dando prioridad a los valores del privado.
     * El fichero privado es completamente opcional.
     *
     * @param baseDir directorio donde buscar los ficheros
     * @return mapa consolidado
     * @throws IOException si hay errores de lectura
     */
    public static Map<String, String> loadDefaultsIn(Path baseDir) throws IOException {
        Path publicFile = baseDir.resolve("env.public");
        Path privateFile = baseDir.resolve("env.private");
        return load(publicFile, privateFile);
    }

    /**
     * Busca y carga automáticamente los ficheros {@code env.public} y {@code env.private}
     * situados en la raíz del proyecto, detectando si la ejecución se realiza desde 
     * un submódulo (subiendo un nivel si fuera necesario).
     *
     * @return mapa consolidado con las variables de entorno de la raíz
     * @throws IOException si hay errores de lectura
     */
    public static Map<String, String> loadProjectDefaults() throws IOException {
        Path current = Path.of(".").toAbsolutePath().normalize();
        Path publicFile = current.resolve("env.public");
        Path privateFile = current.resolve("env.private");
        
        // Si no se encuentran en el directorio actual, comprueba si estamos en un submódulo y sube a la raíz
        if (!Files.exists(publicFile) && current.getParent() != null) {
            Path parentPublic = current.getParent().resolve("env.public");
            if (Files.exists(parentPublic)) {
                current = current.getParent();
                publicFile = current.resolve("env.public");
                privateFile = current.resolve("env.private");
            }
        }
        
        return load(publicFile, privateFile);
    }

    /**
     * Lee las asignaciones de un único fichero {@code .env}.
     * Si el fichero no existe, devuelve un mapa vacío de forma silenciosa.
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
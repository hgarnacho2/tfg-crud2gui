package com.crud2gui.statement.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Lee y escribe {@link StatementFilterConfig} en formato JSON.
 *
 * <p>Esta clase solo realiza la conversión entre JSON y el modelo de
 * configuración; el consumidor decide cuándo cargar o guardar el fichero.
 * La configuración no se persiste automáticamente al ejecutar el filtro.</p>
 */
public final class StatementFilterConfigJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private StatementFilterConfigJson() {
    }

    /**
     * Lee y valida una configuración JSON.
     *
     * @param path ruta del fichero JSON
     * @return configuración reconstruida desde el fichero
     * @throws IOException si el fichero no existe, no puede leerse o contiene JSON inválido
     * @throws NullPointerException si {@code path} es {@code null}
     * @throws IllegalArgumentException si la configuración no cumple sus invariantes
     */
    public static StatementFilterConfig read(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (!Files.isRegularFile(path)) {
            throw new IOException("No existe el fichero de configuración: " + path);
        }

        try (InputStream input = Files.newInputStream(path)) {
            return MAPPER.readValue(input, StatementFilterConfig.class);
        }
    }

    /**
     * Escribe la configuración como JSON legible, creando los directorios
     * padre si no existen.
     *
     * @param config configuración que se serializará
     * @param path ruta del fichero JSON de destino
     * @throws IOException si no se puede crear el directorio o escribir el fichero
     * @throws NullPointerException si {@code config} o {@code path} es {@code null}
     */
    public static void write(StatementFilterConfig config, Path path) throws IOException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(path, "path");

        Path absolutePath = path.toAbsolutePath();
        Path parent = absolutePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (OutputStream output = Files.newOutputStream(path)) {
            MAPPER.writeValue(output, config);
        }
    }
}
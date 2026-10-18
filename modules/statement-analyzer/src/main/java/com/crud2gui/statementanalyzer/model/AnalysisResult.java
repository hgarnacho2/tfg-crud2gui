package com.crud2gui.statementanalyzer.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Resultado de analizar un enunciado: la respuesta del modelo y su JSON ya interpretado.
 *
 * @param response metadatos y texto original de la respuesta
 * @param json     el JSON devuelto por el modelo
 */
public record AnalysisResult(ModelResponse response, JsonNode json) {

    private static final ObjectMapper PRETTY = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyMMdd_HHmmss");

    /**
     * El JSON con sangria.
     *
     * @return texto JSON legible
     * @throws JsonProcessingException si no se puede serializar
     */
    public String prettyJson() throws JsonProcessingException {
        return PRETTY.writeValueAsString(json);
    }

    /**
     * Escribe el JSON en un fichero UTF-8, creando las carpetas necesarias y sobrescribiendolo.
     *
     * @param file destino
     * @throws IOException si no se puede escribir
     */
    public void writeTo(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, prettyJson() + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    /**
     * Nombre de fichero seguro en cualquier sistema operativo para un proveedor y modelo
     * (por ejemplo {@code ollama-qwen3_8b.json}); los caracteres como ":" o "/" se sustituyen.
     *
     * @param provider proveedor
     * @param model    modelo
     * @return nombre de fichero
     */
    public static String fileNameFor(String provider, String model) {
        return (provider + "-" + model).replaceAll("[^A-Za-z0-9._-]", "_") + ".json";
    }

    /**
     * Nombre de fichero con marca de tiempo para conservar resultados de ejecuciones distintas.
     *
     * @param provider proveedor
     * @param model    modelo
     * @param timestamp fecha y hora que se incluiran en el nombre
     * @return nombre de fichero con marca de tiempo
     */
    public static String timestampedFileNameFor(String provider, String model, LocalDateTime timestamp) {
        String fileName = fileNameFor(provider, model);
        int extensionIndex = fileName.lastIndexOf('.');
        return fileName.substring(0, extensionIndex) + "_" + FILE_TIMESTAMP.format(timestamp)
                + fileName.substring(extensionIndex);
    }
}

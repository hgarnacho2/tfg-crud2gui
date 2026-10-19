package com.crud2gui.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lectura y escritura del metamodelo en JSON.
 *
 * <p>El JSON se escribe con sangria y sin los campos nulos. Al leer se ignoran las propiedades
 * desconocidas, de modo que un fichero generado por una version menor posterior sigue siendo legible.</p>
 */
public final class ModelIO {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private ModelIO() { }

    /**
     * Escribe el modelo en un fichero JSON, sobrescribiendolo si existe.
     *
     * @param model modelo a guardar
     * @param file  fichero de destino
     * @throws IOException si no se puede escribir el fichero
     */
    public static void write(ProjectModel model, Path file) throws IOException {
        MAPPER.writeValue(file.toFile(), model);
    }

    /**
     * Serializa el modelo a una cadena JSON.
     *
     * @param model modelo a serializar
     * @return el JSON con sangria
     * @throws IOException si el modelo no se puede serializar
     */
    public static String toJson(ProjectModel model) throws IOException {
        return MAPPER.writeValueAsString(model);
    }

    /**
     * Lee un modelo desde un fichero JSON y comprueba que la version mayor del esquema es compatible.
     *
     * @param file fichero JSON
     * @return el modelo leido
     * @throws IOException si no se puede leer el fichero, el JSON es invalido o la version mayor del
     *                     esquema no coincide con {@link ProjectModel#CURRENT_SCHEMA}
     */
    public static ProjectModel read(Path file) throws IOException {
        ProjectModel model = MAPPER.readValue(Files.readAllBytes(file), ProjectModel.class);
        validateSchemaVersion(model);
        return model;
    }

    /**
     * Lee y valida un metamodelo desde una cadena JSON.
     *
     * @param json contenido JSON
     * @return modelo leido
     * @throws IOException si el JSON es invalido o su version mayor no es compatible
     */
    public static ProjectModel fromJson(String json) throws IOException {
        ProjectModel model = MAPPER.readValue(json, ProjectModel.class);
        validateSchemaVersion(model);
        return model;
    }

    private static void validateSchemaVersion(ProjectModel model) throws IOException {
        String found = model.schemaVersion();
        String expectedMajor = ProjectModel.CURRENT_SCHEMA.split("\\.")[0];
        if (found == null || !found.split("\\.")[0].equals(expectedMajor)) {
            throw new IOException("Version de esquema no compatible: " + found
                    + " (se esperaba " + ProjectModel.CURRENT_SCHEMA + ")");
        }
    }
}

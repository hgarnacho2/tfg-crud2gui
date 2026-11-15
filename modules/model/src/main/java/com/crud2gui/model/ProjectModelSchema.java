package com.crud2gui.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Proporciona el contrato JSON canónico de {@link ProjectModel} versión 1.0.
 *
 * <p>Permite obtener el esquema para incluirlo en una petición al LLM y validar su respuesta antes
 * de deserializarla con {@link ModelIO}. La validación comprueba la estructura y los tipos JSON;
 * no sustituye las comprobaciones semánticas del modelo.</p>
 */
public final class ProjectModelSchema {

    /** Nombre del esquema empaquetado junto a esta clase. */
    private static final String RESOURCE = "project-model-v1.schema.json";

    /** Conversor JSON utilizado para analizar las respuestas como árboles de nodos. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Esquema compilado una vez al inicializar la clase y reutilizado en cada validación. */
    private static final JsonSchema SCHEMA = loadSchema();

    /** Impide crear instancias de esta clase de utilidades estáticas. */
    private ProjectModelSchema() { }

    /**
     * Lee el esquema JSON versionado que puede incluirse en una petición al LLM.
     *
     * @return contenido UTF-8 del esquema JSON de {@link ProjectModel} versión 1.0
     * @throws IOException si no se encuentra el recurso o no se puede leer
     */
    public static String jsonSchema() throws IOException {
        try (InputStream input = ProjectModelSchema.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("No se encuentra el recurso " + RESOURCE);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Comprueba que una cadena contiene un objeto JSON conforme al esquema de
     * {@link ProjectModel} versión 1.0, sin deserializarlo.
     *
     * @param json respuesta JSON que se va a comprobar
     * @throws IOException si el texto no es JSON válido, no contiene un objeto o incumple el esquema
     */
    public static void validate(String json) throws IOException {
        JsonNode instance = MAPPER.readTree(json);
        if (instance == null || !instance.isObject()) {
            throw new IOException("El JSON del ProjectModel debe ser un objeto");
        }
        Set<ValidationMessage> errors = SCHEMA.validate(instance);
        if (!errors.isEmpty()) {
            String details = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new IOException("El JSON no cumple el esquema ProjectModel 1.0: " + details);
        }
    }

    /**
     * Carga y compila el recurso JSON Schema usando la especificación Draft 7.
     *
     * @return esquema compilado para validar instancias JSON
     * @throws ExceptionInInitializerError si el recurso no está disponible o no se puede leer
     */
    private static JsonSchema loadSchema() {
        try (InputStream input = ProjectModelSchema.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IOException("No se encuentra el recurso " + RESOURCE);
            }
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(input);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}
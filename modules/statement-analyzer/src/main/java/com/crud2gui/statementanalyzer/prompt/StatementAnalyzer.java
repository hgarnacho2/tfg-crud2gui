package com.crud2gui.statementanalyzer.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.crud2gui.statementanalyzer.model.AnalysisResult;
import com.crud2gui.statementanalyzer.exception.InvalidModelOutputException;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.client.ModelClient;

/**
 * Analiza el enunciado de una practica con un {@link ModelClient}: lee el prompt y el enunciado,
 * llama al modelo y comprueba que la respuesta es un objeto JSON.
 *
 * <p>Por ahora no valida el contenido contra el metamodelo; solo exige JSON bien formado, sin texto
 * antes ni despues. El resultado se guarda tal cual para compararlo mas adelante.</p>
 */
public final class StatementAnalyzer {

    private static final ObjectMapper STRICT = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private final ModelClient client;

    /**
     * Crea el analizador.
     *
     * @param client cliente del proveedor que se va a consultar
     */
    public StatementAnalyzer(ModelClient client) {
        this.client = client;
    }

    /**
     * Lee los dos ficheros y analiza el enunciado.
     *
     * @param model         modelo a usar
     * @param promptFile    fichero de texto con la consulta
     * @param statementFile fichero de texto plano con el enunciado
     * @param options       parametros de generacion
     * @return resultado con el JSON del modelo
     * @throws IOException                 si un fichero no existe o esta vacio, o falla la llamada
     * @throws InvalidModelOutputException si la respuesta no es un objeto JSON valido
     * @throws InterruptedException        si el hilo se interrumpe
     */
    public AnalysisResult analyze(String model, Path promptFile, Path statementFile, GenerationOptions options)
            throws IOException, InterruptedException {
        return analyze(model, readText(promptFile, "prompt"), readText(statementFile, "enunciado"), options);
    }

    /**
     * Analiza un enunciado ya cargado en memoria.
     *
     * @param model     modelo a usar
     * @param prompt    consulta para el modelo
     * @param statement texto del enunciado
     * @param options   parametros de generacion
     * @return resultado con el JSON del modelo
     * @throws IOException                 si falla la llamada
     * @throws InvalidModelOutputException si la respuesta no es un objeto JSON valido
     * @throws InterruptedException        si el hilo se interrumpe
     */
    public AnalysisResult analyze(String model, String prompt, String statement, GenerationOptions options)
            throws IOException, InterruptedException {
        ModelResponse response = client.generate(model, prompt, statement, options);
        JsonNode json;
        try {
            json = STRICT.readTree(response.text());
        } catch (JsonProcessingException e) {
            throw new InvalidModelOutputException("La respuesta no es JSON valido: " + e.getOriginalMessage(),
                    response, e);
        }
        if (json == null || !json.isObject()) {
            throw new InvalidModelOutputException("La respuesta es JSON pero no un objeto", response, null);
        }
        return new AnalysisResult(response, json);
    }

    /** Lee un TXT en UTF-8 normalizando los saltos de linea a {@code \n} y recortando los extremos. */
    static String readText(Path file, String label) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("No existe el fichero de " + label + ": " + file);
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            throw new IOException("El fichero de " + label + " no esta en UTF-8: " + file, e);
        }
        text = text.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (text.isEmpty()) {
            throw new IOException("El fichero de " + label + " esta vacio: " + file);
        }
        return text;
    }
}

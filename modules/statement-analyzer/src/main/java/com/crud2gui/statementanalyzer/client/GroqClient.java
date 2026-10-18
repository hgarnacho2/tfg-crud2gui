package com.crud2gui.statementanalyzer.client;

import com.crud2gui.common.DotEnv;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.exception.ModelClientException;
import com.crud2gui.statementanalyzer.prompt.PromptComposer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cliente de la API REST de Groq (compatible con OpenAI {@code api.groq.com/openai/v1}).
 *
 * <p>La clave viaja siempre en la cabecera de autorización {@code Authorization: Bearer <TOKEN>}.</p>
 */
public final class GroqClient implements ModelClient {

    /** Nombre del proveedor. */
    public static final String PROVIDER = "groq";
    /** URL base de la API de Groq. */
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.groq.com/openai/v1");
    /** Variable de entorno (o clave del fichero .env) que contiene la API key de Groq. */
    public static final String API_KEY_VARIABLE = "GROQ_API_KEY";
    /** Tiempo maximo por peticion por defecto. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(300);

    private static final String AUTH_HEADER = "Authorization";
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9._-]+");

    private final String apiKey;
    private final String baseUrl;
    private final JsonHttp http;

    /**
     * Cliente con la configuracion por defecto pasándole explícitamente la API key.
     *
     * @param apiKey clave de la API de Groq
     */
    public GroqClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URI, DEFAULT_TIMEOUT, RetryPolicy.defaults());
    }

    /**
     * Cliente configurable.
     *
     * @param apiKey         clave de la API
     * @param baseUri        URL base, hasta {@code /v1}
     * @param requestTimeout tiempo maximo de cada peticion
     * @param retryPolicy    reintentos ante errores transitorios
     */
    public GroqClient(String apiKey, URI baseUri, Duration requestTimeout, RetryPolicy retryPolicy) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("La API key de Groq no puede estar vacia");
        }
        this.apiKey = apiKey.strip();
        this.baseUrl = stripTrailingSlashes(baseUri.toString());
        this.http = new JsonHttp("Groq", requestTimeout, retryPolicy);
    }

    /**
     * Constructor por defecto que lee {@code GROQ_API_KEY} empleando {@link DotEnv#getStringVar(String, String)}.
     */
    public GroqClient() {
        this(getApiKeyFromDotEnv());
    }

    private static String getApiKeyFromDotEnv() {
        String key = DotEnv.getStringVar(API_KEY_VARIABLE, "");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Falta " + API_KEY_VARIABLE
                    + ": define la variable de entorno o configúrala en tus ficheros .env.private / .env.public en la raíz del proyecto");
        }
        return key;
    }

    /**
     * Crea un cliente leyendo {@code GROQ_API_KEY} mediante {@link DotEnv}.
     *
     * @return cliente con la configuracion por defecto
     * @throws IOException si no hay clave
     */
    public static GroqClient fromEnvironment() throws IOException {
        String key = DotEnv.getStringVar(API_KEY_VARIABLE, "");
        if (key == null || key.isBlank()) {
            throw new IOException("Falta " + API_KEY_VARIABLE
                    + ": define la variable de entorno o configura los ficheros env.public / env.private en la raíz del proyecto");
        }
        return new GroqClient(key);
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public List<ModelInfo> listModels() throws IOException, InterruptedException {
        List<ModelInfo> models = new ArrayList<>();
        JsonNode json = http.get(URI.create(baseUrl + "/models"), headers());
        for (JsonNode model : json.path("data")) {
            String id = model.path("id").asText("");
            if (!id.isEmpty()) {
                models.add(new ModelInfo(id, id));
            }
        }
        return models;
    }

    @Override
    public ModelResponse generate(String model, String prompt, String statement, GenerationOptions options)
            throws IOException, InterruptedException {
        String modelId = normalizeModelId(model);
        ObjectNode body = buildRequestBody(modelId, PromptComposer.compose(prompt, statement), options);
        long start = System.nanoTime();
        JsonNode json = http.post(URI.create(baseUrl + "/chat/completions"), headers(), body);
        return parseResponse(modelId, json, Duration.ofNanos(System.nanoTime() - start));
    }

    /** Construye el cuerpo JSON estándar compatible con OpenAI para Chat Completions. */
    static ObjectNode buildRequestBody(String modelId, String text, GenerationOptions options) {
        ObjectNode root = JsonHttp.MAPPER.createObjectNode();
        root.put("model", modelId);
        
        ArrayNode messages = root.putArray("messages");
        messages.addObject()
                .put("role", "user")
                .put("content", text);

        root.put("temperature", options.temperature());
        
        // Si se requiere formato JSON estricto soportado por Groq
        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        return root;
    }

    static String normalizeModelId(String model) {
        if (model == null) {
            throw new IllegalArgumentException("El modelo no puede ser nulo");
        }
        String id = model.strip();
        if (!MODEL_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Identificador de modelo de Groq no valido: '" + model + "'");
        }
        return id;
    }

    private Map<String, String> headers() {
        return Map.of(AUTH_HEADER, "Bearer " + apiKey);
    }

    private static ModelResponse parseResponse(String modelId, JsonNode json, Duration elapsed)
            throws ModelClientException {
        JsonNode choice = json.path("choices").path(0);
        if (choice.isMissingNode()) {
            throw new ModelClientException("Groq no devolvio ninguna opcion de respuesta", 0);
        }
        
        String text = choice.path("message").path("content").asText("");
        String finishReason = choice.path("finish_reason").asText(null);
        
        if (text.isEmpty()) {
            throw new ModelClientException("Groq devolvio una respuesta sin texto (finishReason=" + finishReason + ")", 0);
        }

        JsonNode usage = json.path("usage");
        return new ModelResponse(PROVIDER, modelId, text, finishReason,
                usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null,
                usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null,
                elapsed);
    }

    private static String stripTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}

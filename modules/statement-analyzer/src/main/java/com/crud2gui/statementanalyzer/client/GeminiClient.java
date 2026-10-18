package com.crud2gui.statementanalyzer.client;

import com.crud2gui.common.DotEnv;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.exception.ModelClientException;
import com.crud2gui.statementanalyzer.prompt.PromptComposer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cliente de la API REST de Gemini ({@code generativelanguage.googleapis.com/v1beta}).
 *
 * <p>La clave viaja siempre en la cabecera {@code x-goog-api-key}, nunca en la URL. La URL de
 * generacion se construye por concatenacion y no con {@link URI#resolve(String)}: el ":" de
 * {@code modelo:generateContent} haria que Java interpretase el modelo como un esquema.</p>
 */
public final class GeminiClient implements ModelClient {

    /** Nombre del proveedor. */
    public static final String PROVIDER = "gemini";
    /** URL base de la API. */
    public static final URI DEFAULT_BASE_URI = URI.create("https://generativelanguage.googleapis.com/v1beta");
    /** Variable de entorno (o clave del fichero {@code .env.public}/{@code .env.private}) que contiene la API key. */
    public static final String API_KEY_VARIABLE = "GEMINI_API_KEY";
    /** Tiempo maximo por peticion por defecto. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(300);

    private static final String API_KEY_HEADER = "x-goog-api-key";
    private static final String MODEL_PREFIX = "models/";
    private static final Pattern MODEL_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final int PAGE_SIZE = 1000;
    private static final int MAX_PAGES = 50;

    private final String apiKey;
    private final String baseUrl;
    private final JsonHttp http;

    /**
     * Cliente con la configuracion por defecto pasándole explícitamente la API key.
     *
     * @param apiKey clave de la API de Gemini
     */
    public GeminiClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URI, DEFAULT_TIMEOUT, RetryPolicy.defaults());
    }

    /**
     * Cliente configurable (por ejemplo para apuntar a un servidor de pruebas).
     *
     * @param apiKey         clave de la API
     * @param baseUri        URL base, hasta {@code /v1beta}
     * @param requestTimeout tiempo maximo de cada peticion
     * @param retryPolicy    reintentos ante errores transitorios
     */
    public GeminiClient(String apiKey, URI baseUri, Duration requestTimeout, RetryPolicy retryPolicy) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("La API key de Gemini no puede estar vacia");
        }
        this.apiKey = apiKey.strip();
        this.baseUrl = stripTrailingSlashes(baseUri.toString());
        this.http = new JsonHttp("Gemini", requestTimeout, retryPolicy);
    }

    /**
     * Constructor por defecto que lee {@code GEMINI_API_KEY} empleando de manera unificada
     * la jerarquía encapsulada en {@link DotEnv#getStringVar(String, String)}.
     */
    public GeminiClient() {
        this(getApiKeyFromDotEnv());
    }

    private static String getApiKeyFromDotEnv() {
        String key = DotEnv.getStringVar(API_KEY_VARIABLE, null);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Falta " + API_KEY_VARIABLE
                    + ": define la variable de entorno o configúrala en tus ficheros .env.private / .env.public en la raíz del proyecto");
        }
        return key;
    }

    /**
     * Crea un cliente leyendo {@code GEMINI_API_KEY} mediante {@link DotEnv}.
     *
     * @return cliente con la configuracion por defecto
     * @throws IOException si no hay clave o los ficheros de entorno no se pueden leer
     */
    public static GeminiClient fromEnvironment() throws IOException {
        String key = DotEnv.getStringVar(API_KEY_VARIABLE, null);
        if (key == null || key.isBlank()) {
            throw new IOException("Falta " + API_KEY_VARIABLE
                    + ": define la variable de entorno o configura los ficheros .env.public / .env.private en la raíz del proyecto");
        }
        return new GeminiClient(key);
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public List<ModelInfo> listModels() throws IOException, InterruptedException {
        List<ModelInfo> models = new ArrayList<>();
        String pageToken = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            String url = baseUrl + "/models?pageSize=" + PAGE_SIZE
                    + (pageToken == null ? "" : "&pageToken=" + URLEncoder.encode(pageToken, StandardCharsets.UTF_8));
            JsonNode json = http.get(URI.create(url), headers());
            for (JsonNode model : json.path("models")) {
                String id = stripModelPrefix(model.path("name").asText(""));
                if (!id.isEmpty() && supportsGenerateContent(model)) {
                    models.add(new ModelInfo(id, model.path("displayName").asText(id)));
                }
            }
            pageToken = json.path("nextPageToken").asText("");
            if (pageToken.isEmpty()) {
                return models;
            }
        }
        return models;
    }

    @Override
    public ModelResponse generate(String model, String prompt, String statement, GenerationOptions options)
            throws IOException, InterruptedException {
        String modelId = normalizeModelId(model);
        ObjectNode body = buildRequestBody(PromptComposer.compose(prompt, statement), options);
        long start = System.nanoTime();
        JsonNode json = http.post(generateUri(modelId), headers(), body);
        return parseResponse(modelId, json, Duration.ofNanos(System.nanoTime() - start));
    }

    /** Cuerpo identico al de la coleccion de Postman; Jackson se encarga de escapar el texto. */
    static ObjectNode buildRequestBody(String text, GenerationOptions options) {
        ObjectNode root = JsonHttp.MAPPER.createObjectNode();
        root.putArray("contents").addObject().putArray("parts").addObject().put("text", text);
        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", options.temperature());
        if (options.thinkingLevel() != null) {
            generationConfig.putObject("thinkingConfig").put("thinkingLevel", options.thinkingLevel());
        }
        return root;
    }

    /**
     * Acepta {@code gemini-x} o {@code models/gemini-x} y rechaza cualquier cosa que no sea un
     * identificador simple (evita ":" o "/" que alterarian la URL).
     */
    static String normalizeModelId(String model) {
        if (model == null) {
            throw new IllegalArgumentException("El modelo no puede ser nulo");
        }
        String id = stripModelPrefix(model.strip());
        if (!MODEL_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Identificador de modelo no valido: '" + model
                    + "' (use por ejemplo gemini-3.5-flash-lite)");
        }
        return id;
    }

    private URI generateUri(String modelId) {
        return URI.create(baseUrl + "/models/" + modelId + ":generateContent");
    }

    private Map<String, String> headers() {
        return Map.of(API_KEY_HEADER, apiKey);
    }

    private static ModelResponse parseResponse(String modelId, JsonNode json, Duration elapsed)
            throws ModelClientException {
        JsonNode candidate = json.path("candidates").path(0);
        if (candidate.isMissingNode()) {
            JsonNode blockReason = json.path("promptFeedback").path("blockReason");
            throw new ModelClientException("Gemini no devolvio ningun candidato"
                    + (blockReason.isTextual() ? " (bloqueado: " + blockReason.asText() + ")" : ""), 0);
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false) && part.hasNonNull("text")) {
                text.append(part.get("text").asText());
            }
        }
        String finishReason = candidate.path("finishReason").asText(null);
        if (text.length() == 0) {
            throw new ModelClientException("Gemini devolvio una respuesta sin texto (finishReason="
                    + finishReason + ")", 0);
        }
        JsonNode usage = json.path("usageMetadata");
        return new ModelResponse(PROVIDER, modelId, text.toString(), finishReason,
                usage.hasNonNull("promptTokenCount") ? usage.get("promptTokenCount").asInt() : null,
                usage.hasNonNull("candidatesTokenCount") ? usage.get("candidatesTokenCount").asInt() : null,
                elapsed);
    }

    private static boolean supportsGenerateContent(JsonNode model) {
        JsonNode methods = model.path("supportedGenerationMethods");
        if (!methods.isArray()) {
            return true;
        }
        for (JsonNode method : methods) {
            if ("generateContent".equals(method.asText())) {
                return true;
            }
        }
        return false;
    }

    private static String stripModelPrefix(String name) {
        return name.startsWith(MODEL_PREFIX) ? name.substring(MODEL_PREFIX.length()) : name;
    }

    private static String stripTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}
package com.crud2gui.statementanalyzer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.exception.ModelClientException;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.prompt.PromptComposer;

/**
 * Cliente de un servidor Ollama local ({@code /api/tags} y {@code /api/chat}).
 *
 * <p>Ollama usa por defecto una ventana de contexto pequena y recorta el prompt en silencio si no
 * cabe; un enunciado completo la supera con facilidad. Por eso este cliente fija {@code num_ctx}
 * explicitamente (32768 por defecto). {@link ModelResponse#inputTokens()} permite comprobar que el
 * prompt entro entero.</p>
 */
public final class OllamaClient implements ModelClient {

    /** Nombre del proveedor. */
    public static final String PROVIDER = "ollama";
    /** URL base por defecto de un Ollama local. */
    public static final URI DEFAULT_BASE_URI = URI.create("http://localhost:11434");
    /** Variable de entorno con la URL base (sin {@code /api/...}). */
    public static final String BASE_URL_VARIABLE = "OLLAMA_BASE_URL";
    /** Variable de entorno con el tamano de contexto. */
    public static final String NUM_CTX_VARIABLE = "OLLAMA_NUM_CTX";
    /** Tamano de contexto por defecto, en tokens. */
    public static final int DEFAULT_NUM_CTX = 32768;
    /** Tiempo maximo por peticion por defecto: un modelo local puede tardar minutos. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(600);

    private final String baseUrl;
    private final int numCtx;
    private final boolean disableThinking;
    private final JsonHttp http;

    /** Cliente con la configuracion por defecto. */
    public OllamaClient() {
        this(DEFAULT_BASE_URI, DEFAULT_TIMEOUT, DEFAULT_NUM_CTX, true, RetryPolicy.defaults());
    }

    /**
     * Cliente configurable.
     *
     * @param baseUri         URL base del servidor, por ejemplo {@code http://localhost:11434}
     * @param requestTimeout  tiempo maximo de cada peticion
     * @param numCtx          ventana de contexto en tokens
     * @param disableThinking si es {@code true} se envia {@code think:false} (modelos como qwen3)
     * @param retryPolicy     reintentos ante errores transitorios
     */
    public OllamaClient(URI baseUri, Duration requestTimeout, int numCtx, boolean disableThinking,
                        RetryPolicy retryPolicy) {
        if (numCtx < 1) {
            throw new IllegalArgumentException("numCtx debe ser positivo");
        }
        this.baseUrl = stripTrailingSlashes(baseUri.toString());
        this.numCtx = numCtx;
        this.disableThinking = disableThinking;
        this.http = new JsonHttp("Ollama", requestTimeout, retryPolicy);
    }

    /**
     * Crea un cliente leyendo {@code OLLAMA_BASE_URL} y {@code OLLAMA_NUM_CTX} del entorno si existen.
     *
     * @return cliente configurado
     * @throws IOException si {@code OLLAMA_NUM_CTX} no es un entero positivo
     */
    public static OllamaClient fromEnvironment() throws IOException {
        String url = System.getenv(BASE_URL_VARIABLE);
        String ctx = System.getenv(NUM_CTX_VARIABLE);
        int numCtx = DEFAULT_NUM_CTX;
        if (ctx != null && !ctx.isBlank()) {
            try {
                numCtx = Integer.parseInt(ctx.strip());
            } catch (NumberFormatException e) {
                throw new IOException(NUM_CTX_VARIABLE + " debe ser un entero: " + ctx, e);
            }
        }
        URI base = url == null || url.isBlank() ? DEFAULT_BASE_URI : URI.create(url.strip());
        return new OllamaClient(base, DEFAULT_TIMEOUT, numCtx, true, RetryPolicy.defaults());
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public List<ModelInfo> listModels() throws IOException, InterruptedException {
        JsonNode json = http.get(URI.create(baseUrl + "/api/tags"), Map.of());
        List<ModelInfo> models = new ArrayList<>();
        for (JsonNode model : json.path("models")) {
            String name = model.path("name").asText(model.path("model").asText(""));
            if (!name.isEmpty()) {
                models.add(new ModelInfo(name, name));
            }
        }
        return models;
    }

    @Override
    public ModelResponse generate(String model, String prompt, String statement, GenerationOptions options)
            throws IOException, InterruptedException {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("El modelo no puede estar vacio");
        }
        ObjectNode body = buildRequestBody(model.strip(), PromptComposer.compose(prompt, statement), options);
        long start = System.nanoTime();
        JsonNode json = http.post(URI.create(baseUrl + "/api/chat"), Map.of(), body);
        return parseResponse(model.strip(), json, Duration.ofNanos(System.nanoTime() - start));
    }

    private ObjectNode buildRequestBody(String model, String text, GenerationOptions options) {
        ObjectNode root = JsonHttp.MAPPER.createObjectNode();
        root.put("model", model);
        root.put("stream", false);
        root.put("format", "json");
        if (disableThinking) {
            root.put("think", false);
        }
        root.putObject("options")
                .put("temperature", options.temperature())
                .put("num_ctx", numCtx);
        root.putArray("messages").addObject().put("role", "user").put("content", text);
        return root;
    }

    private static ModelResponse parseResponse(String model, JsonNode json, Duration elapsed)
            throws ModelClientException {
        if (json.hasNonNull("error")) {
            throw new ModelClientException("Ollama devolvio un error: " + json.get("error").asText(), 0);
        }
        String text = json.path("message").path("content").asText("");
        String doneReason = json.path("done_reason").asText(null);
        if (text.isBlank()) {
            throw new ModelClientException("Ollama devolvio una respuesta sin texto (done_reason="
                    + doneReason + ")", 0);
        }
        return new ModelResponse(PROVIDER, model, text, doneReason,
                json.hasNonNull("prompt_eval_count") ? json.get("prompt_eval_count").asInt() : null,
                json.hasNonNull("eval_count") ? json.get("eval_count").asInt() : null,
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

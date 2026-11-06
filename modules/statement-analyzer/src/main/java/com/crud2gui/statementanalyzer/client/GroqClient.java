package com.crud2gui.statementanalyzer.client;

import com.crud2gui.common.utils.DotEnv;
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
 * Cliente HTTP para consultar modelos y generar respuestas mediante la API de Groq.
 *
 * <p>Usa la API compatible con OpenAI bajo {@code /openai/v1}: consulta {@code /models}
 * y envía generaciones a {@code /chat/completions}. Cada petición incluye el texto
 * compuesto a partir del prompt y el enunciado, la temperatura configurada y el modo
 * de respuesta JSON.</p>
 *
 * <p>La autenticación se realiza exclusivamente mediante la cabecera
 * {@code Authorization: Bearer <TOKEN>}. La clave puede proporcionarse al crear el
 * cliente o cargarse desde la configuración de entorno mediante {@link DotEnv}; no
 * debe incluirse en registros ni en URLs.</p>
 *
 * @see ModelClient
 * @see GroqClientProvider
 */
public final class GroqClient implements ModelClient {

    /** Identificador estable del proveedor que devuelve {@link #provider()}. */
    public static final String PROVIDER = "groq";
    /** URI base predeterminada de la API compatible con OpenAI de Groq. */
    public static final URI DEFAULT_BASE_URI = URI.create("https://api.groq.com/openai/v1");
    /** Nombre de la variable de entorno o clave DotEnv que contiene la API key. */
    public static final String API_KEY_VARIABLE = "GROQ_API_KEY";
    /** Tiempo máximo de espera de cada petición HTTP en la configuración predeterminada. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(300);

    /** Nombre de la cabecera HTTP estándar que transporta la autenticación Bearer. */
    private static final String AUTH_HEADER = "Authorization";
    /** Patrón de IDs admitidos: un nombre simple o un nombre con un único namespace. */
    private static final Pattern MODEL_ID = Pattern.compile(
            "[A-Za-z0-9][A-Za-z0-9._-]*(?:/[A-Za-z0-9][A-Za-z0-9._-]*)?");

    /** API key usada para firmar las peticiones; nunca debe registrarse ni exponerse. */
    private final String apiKey;
    /** URL base normalizada, sin barras finales, para construir las rutas del API. */
    private final String baseUrl;
    /** Ayudante HTTP que serializa JSON, aplica timeouts y reintenta errores transitorios. */
    private final JsonHttp http;

    /**
     * Crea un cliente con URI, timeout y política de reintentos predeterminados.
     *
     * @param apiKey clave de la API de Groq; se elimina el espacio exterior
     * @throws IllegalArgumentException si la clave es nula o está vacía
     */
    public GroqClient(String apiKey) {
        this(apiKey, DEFAULT_BASE_URI, DEFAULT_TIMEOUT, RetryPolicy.defaults());
    }

    /**
    * Crea un cliente con una configuración HTTP explícita, útil también para pruebas.
     *
    * @param apiKey clave de la API de Groq; se elimina el espacio exterior
    * @param baseUri URI base del API, normalmente terminada en {@code /openai/v1}; las barras
    *                finales se eliminan antes de construir rutas
    * @param requestTimeout tiempo máximo permitido para cada petición HTTP
    * @param retryPolicy política de reintentos aplicada a errores HTTP transitorios
    * @throws IllegalArgumentException si la clave es nula o está vacía
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
     * Crea un cliente con la clave obtenida mediante {@link DotEnv#getStringVar(String, String)}.
     *
     * @throws IllegalStateException si no se encuentra una clave no vacía
     */
    public GroqClient() {
        this(getApiKeyFromDotEnv());
    }

    /**
     * Resuelve la API key en el entorno del sistema, propiedades Java o archivos DotEnv.
     *
     * @return clave de Groq sin espacios exteriores
     * @throws IllegalStateException si no hay una clave configurada
     */
    private static String getApiKeyFromDotEnv() {
        String key = DotEnv.getStringVar(API_KEY_VARIABLE, "");
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("Falta " + API_KEY_VARIABLE
                    + ": define la variable de entorno o configúrala en tus ficheros .env.private / .env.public en la raíz del proyecto");
        }
        return key;
    }

    /**
     * Crea un cliente leyendo {@link #API_KEY_VARIABLE} mediante {@link DotEnv}.
     *
     * @return cliente con la configuración predeterminada
     * @throws IOException si no se encuentra una clave no vacía
     */
    public static GroqClient fromEnvironment() throws IOException {
        String key = DotEnv.getStringVar(API_KEY_VARIABLE, "");
        if (key == null || key.isBlank()) {
            throw new IOException("Falta " + API_KEY_VARIABLE
                    + ": define la variable de entorno o configura los ficheros env.public / env.private en la raíz del proyecto");
        }
        return new GroqClient(key);
    }

    /**
     * Devuelve el identificador estable de este proveedor.
     *
     * @return {@link #PROVIDER}
     */
    @Override
    public String provider() {
        return PROVIDER;
    }

    /**
     * Consulta los modelos que expone el endpoint {@code GET /models} de Groq.
     * Cada ID se devuelve como identificador y nombre visible para que pueda pasarse
     * directamente a {@link #generate(String, String, String, GenerationOptions)}.
     *
     * @return modelos devueltos por la API, o una lista vacía si no hay ninguno
     * @throws IOException si falla la petición o no se puede interpretar la respuesta
     * @throws InterruptedException si se interrumpe el hilo durante la petición o el reintento
     */
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

    /**
     * Envía un mensaje al endpoint de chat completions y convierte la primera opción
     * de respuesta en un {@link ModelResponse}. El prompt y el enunciado se combinan
     * mediante {@link PromptComposer}; la petición solicita una respuesta JSON.
     *
     * @param model ID de Groq, simple o con un namespace, tal como aparece en {@link #listModels()}
     * @param prompt instrucciones que se anteponen al enunciado
     * @param statement texto del enunciado que analizará el modelo
     * @param options temperatura y opciones comunes de generación
     * @return texto generado junto con el motivo de finalización, uso de tokens y duración
     * @throws IllegalArgumentException si el ID no tiene un formato admitido
     * @throws IOException si falla la petición HTTP o la respuesta no se puede interpretar
     * @throws InterruptedException si se interrumpe el hilo durante la petición o el reintento
     */
    @Override
    public ModelResponse generate(String model, String prompt, String statement, GenerationOptions options)
            throws IOException, InterruptedException {
        String modelId = normalizeModelId(model);
        ObjectNode body = buildRequestBody(modelId, PromptComposer.compose(prompt, statement), options);
        long start = System.nanoTime();
        JsonNode json = http.post(URI.create(baseUrl + "/chat/completions"), headers(), body);
        return parseResponse(modelId, json, Duration.ofNanos(System.nanoTime() - start));
    }

    /**
     * Construye el cuerpo JSON compatible con OpenAI para {@code /chat/completions}.
     * Incluye el ID del modelo, un mensaje de usuario, la temperatura y
     * {@code response_format.type=json_object}.
     *
     * @param modelId ID previamente normalizado del modelo
     * @param text contenido del mensaje de usuario
     * @param options opciones de generación cuya temperatura se enviará
     * @return nodo JSON listo para serializar en la petición HTTP
     */
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

    /**
     * Recorta y valida el ID antes de incluirlo en el cuerpo de la petición.
     * Se admite un segmento opcional separado por {@code /} para los IDs namespaced de Groq.
     *
     * @param model ID recibido del selector de modelos
     * @return ID sin espacios exteriores
     * @throws IllegalArgumentException si el ID es nulo o no coincide con el patrón permitido
     */
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

    /**
     * Construye las cabeceras de autenticación de la API.
     * La clave se incluye únicamente como token Bearer; la cabecera no debe registrarse.
     *
     * @return mapa con la cabecera {@code Authorization}
     */
    private Map<String, String> headers() {
        return Map.of(AUTH_HEADER, "Bearer " + apiKey);
    }

    /**
     * Extrae el texto y los metadatos de la primera opción de una respuesta de chat completions.
     *
     * @param modelId ID asociado a la petición
     * @param json respuesta JSON del API
     * @param elapsed tiempo transcurrido desde el envío de la petición
     * @return respuesta normalizada al modelo común de la aplicación
     * @throws ModelClientException si no hay una opción o su contenido textual está vacío
     */
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

    /**
     * Elimina todas las barras consecutivas del final de una URL base.
     *
     * @param url URL que se normalizará
     * @return URL sin barras finales
     */
    private static String stripTrailingSlashes(String url) {
        int end = url.length();
        while (end > 0 && url.charAt(end - 1) == '/') {
            end--;
        }
        return url.substring(0, end);
    }
}

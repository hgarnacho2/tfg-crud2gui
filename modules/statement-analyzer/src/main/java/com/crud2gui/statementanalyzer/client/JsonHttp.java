package com.crud2gui.statementanalyzer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

import com.crud2gui.statementanalyzer.exception.ModelClientException;

/**
 * Peticiones HTTP con cuerpo JSON compartidas por los clientes: tiempos de espera, reintentos ante
 * errores transitorios y mensajes de error legibles. Nunca incluye cabeceras (claves) en los errores.
 */
public final class JsonHttp {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Set<Integer> TRANSIENT_STATUS = Set.of(429, 500, 502, 503, 504);

    private final String service;
    private final Duration requestTimeout;
    private final RetryPolicy retry;
    private final HttpClient client;

    /**
     * Crea el ayudante.
     *
     * @param service        nombre del servicio para los mensajes de error
     * @param requestTimeout tiempo maximo de cada peticion
     * @param retry          politica de reintentos
     */
    JsonHttp(String service, Duration requestTimeout, RetryPolicy retry) {
        this.service = service;
        this.requestTimeout = requestTimeout;
        this.retry = retry;
        // HTTP/1.1 evita el intento de actualizacion a h2c contra servidores locales.
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    JsonNode get(URI uri, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET();
        headers.forEach(builder::header);
        return execute(builder.build());
    }

    JsonNode post(URI uri, Map<String, String> headers, JsonNode body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        headers.forEach(builder::header);
        return execute(builder.build());
    }

    private JsonNode execute(HttpRequest request) throws IOException, InterruptedException {
        for (int attempt = 1; ; attempt++) {
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (HttpTimeoutException e) {
                throw new ModelClientException(service + ": tiempo de espera agotado (" + requestTimeout.toSeconds()
                        + " s) en " + describe(request.uri()), e);
            } catch (ConnectException e) {
                throw new ModelClientException(service + ": no se pudo conectar con " + describe(request.uri())
                        + ". Comprueba que el servicio esta accesible.", e);
            }

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return parseBody(response.body());
            }
            if (TRANSIENT_STATUS.contains(status) && attempt < retry.maxAttempts()) {
                Thread.sleep(retry.delayAfter(attempt).toMillis());
                continue;
            }
            throw new ModelClientException(service + " respondio HTTP " + status + ": "
                    + errorMessage(response.body()), status);
        }
    }

    private JsonNode parseBody(String body) throws ModelClientException {
        if (body == null || body.isBlank()) {
            throw new ModelClientException(service + " devolvio una respuesta vacia", 0);
        }
        try {
            return MAPPER.readTree(body);
        } catch (JsonProcessingException e) {
            throw new ModelClientException(service + " devolvio una respuesta que no es JSON: "
                    + abbreviate(body, 300), e);
        }
    }

    static String errorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "(sin cuerpo)";
        }
        try {
            JsonNode error = MAPPER.readTree(body).path("error");
            if (error.isObject() && error.hasNonNull("message")) {
                return error.get("message").asText();
            }
            if (error.isTextual()) {
                return error.asText();
            }
        } catch (IOException ignored) {
            // el cuerpo no es JSON: se muestra tal cual, abreviado
        }
        return abbreviate(body, 500);
    }

    static String abbreviate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    /** Host y puerto de una URI, sin ruta ni parametros. */
    private static String describe(URI uri) {
        return uri.getPort() < 0 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
    }
}

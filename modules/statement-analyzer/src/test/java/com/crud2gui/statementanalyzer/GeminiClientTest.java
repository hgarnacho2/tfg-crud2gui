package com.crud2gui.statementanalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crud2gui.statementanalyzer.FakeServer.Reply;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import com.crud2gui.statementanalyzer.client.RetryPolicy;
import com.crud2gui.statementanalyzer.client.GeminiClient;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.exception.ModelClientException;


class GeminiClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final RetryPolicy FAST_RETRY = new RetryPolicy(3, Duration.ofMillis(1));
    private static final String OK_BODY = """
            {"candidates":[{"content":{"role":"model","parts":[
              {"text":"razonando...","thought":true},
              {"text":"{\\"a\\":"},{"text":"1}"}]},"finishReason":"STOP"}],
             "usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":5,"totalTokenCount":15}}
            """;

    private static GeminiClient client(FakeServer server, RetryPolicy retry) {
        return new GeminiClient("test-key", server.uri("/v1beta"), Duration.ofSeconds(10), retry);
    }

    @Test
    void generateSendsTheSameBodyAsPostmanWithoutManualEscaping() throws Exception {
        String prompt = "Prompt con \"comillas\", barra \\ y {{llaves}}\ncon salto\ty tabulador";
        String statement = "Enunciado \u00f1 \u00e1 \u20ac \uD83D\uDE00\nlinea 2 \"x\" \\n literal";
        try (FakeServer server = new FakeServer(r -> Reply.ok(OK_BODY))) {
            ModelResponse response = client(server, FAST_RETRY)
                    .generate("gemini-3.5-flash-lite", prompt, statement, GenerationOptions.defaults());

            FakeServer.Recorded request = server.requests().get(0);
            assertEquals("POST", request.method());
            // los dos puntos viajan literales, sin codificar
            assertEquals("/v1beta/models/gemini-3.5-flash-lite:generateContent", request.pathAndQuery());
            assertEquals("test-key", request.headers().getFirst("x-goog-api-key"));
            assertFalse(request.pathAndQuery().contains("key"), "la clave no debe ir en la URL");

            JsonNode body = JSON.readTree(request.body());
            assertEquals(1, body.path("contents").size());
            assertEquals(1, body.at("/contents/0/parts").size());
            assertEquals(prompt + "\n\n--- ENUNCIADO DE LA PR\u00c1CTICA ---\n" + statement,
                    body.at("/contents/0/parts/0/text").asText());
            assertEquals("application/json", body.at("/generationConfig/responseMimeType").asText());
            assertEquals(0.1, body.at("/generationConfig/temperature").asDouble(), 1e-9);
            assertEquals("low", body.at("/generationConfig/thinkingConfig/thinkingLevel").asText());

            assertEquals("{\"a\":1}", response.text(), "une las partes y descarta las de razonamiento");
            assertEquals("STOP", response.finishReason());
            assertEquals(10, response.inputTokens());
            assertEquals(5, response.outputTokens());
            assertEquals("gemini", response.provider());
        }
    }

    @Test
    void thinkingLevelIsOmittedWhenNull() throws Exception {
        try (FakeServer server = new FakeServer(r -> Reply.ok(OK_BODY))) {
            client(server, FAST_RETRY).generate("gemini-2.5-flash", "p", "s",
                    GenerationOptions.defaults().withThinkingLevel(null));
            JsonNode body = JSON.readTree(server.requests().get(0).body());
            assertTrue(body.at("/generationConfig/thinkingConfig").isMissingNode());
        }
    }

    @Test
    void acceptsModelNamesWithModelsPrefix() throws Exception {
        try (FakeServer server = new FakeServer(r -> Reply.ok(OK_BODY))) {
            client(server, FAST_RETRY).generate("models/gemini-x", "p", "s");
            assertEquals("/v1beta/models/gemini-x:generateContent", server.requests().get(0).pathAndQuery());
        }
    }

    @Test
    void rejectsModelNamesThatWouldAlterTheUrl() throws Exception {
        try (FakeServer server = new FakeServer(r -> Reply.ok(OK_BODY))) {
            GeminiClient client = client(server, FAST_RETRY);
            for (String bad : List.of("gemini:generateContent", "../x", "a/b", "", " ", "x y")) {
                assertThrows(IllegalArgumentException.class, () -> client.generate(bad, "p", "s"), bad);
            }
            assertThrows(IllegalArgumentException.class, () -> client.generate(null, "p", "s"));
            assertTrue(server.requests().isEmpty());
        }
    }

    @Test
    void listModelsFollowsPaginationAndKeepsOnlyGenerateContentModels() throws Exception {
        String page1 = """
                {"models":[
                  {"name":"models/gemini-a","displayName":"Gemini A","supportedGenerationMethods":["generateContent","countTokens"]},
                  {"name":"models/embedding-x","displayName":"Emb","supportedGenerationMethods":["embedContent"]}],
                 "nextPageToken":"tok/en+1"}
                """;
        String page2 = """
                {"models":[{"name":"models/gemini-b","supportedGenerationMethods":["generateContent"]}]}
                """;
        try (FakeServer server = new FakeServer(r -> Reply.ok(r.pathAndQuery().contains("pageToken=") ? page2 : page1))) {
            List<ModelInfo> models = client(server, FAST_RETRY).listModels();

            assertEquals(List.of(new ModelInfo("gemini-a", "Gemini A"), new ModelInfo("gemini-b", "gemini-b")), models);
            assertEquals(2, server.requests().size());
            assertEquals("GET", server.requests().get(0).method());
            assertEquals("/v1beta/models?pageSize=1000", server.requests().get(0).pathAndQuery());
            assertEquals("/v1beta/models?pageSize=1000&pageToken=tok%2Fen%2B1", server.requests().get(1).pathAndQuery());
            assertEquals("test-key", server.requests().get(0).headers().getFirst("x-goog-api-key"));
        }
    }

    @Test
    void retriesTransientErrorsThenSucceeds() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String overloaded = "{\"error\":{\"code\":503,\"message\":\"high demand\",\"status\":\"UNAVAILABLE\"}}";
        try (FakeServer server = new FakeServer(r ->
                calls.incrementAndGet() < 3 ? new Reply(503, overloaded) : Reply.ok(OK_BODY))) {
            ModelResponse response = client(server, FAST_RETRY).generate("gemini-x", "p", "s");
            assertEquals("{\"a\":1}", response.text());
            assertEquals(3, server.requests().size());
        }
    }

    @Test
    void givesUpAfterMaxAttemptsAndReportsTheApiMessage() throws Exception {
        String overloaded = "{\"error\":{\"code\":503,\"message\":\"high demand\"}}";
        try (FakeServer server = new FakeServer(r -> new Reply(503, overloaded))) {
            ModelClientException e = assertThrows(ModelClientException.class,
                    () -> client(server, FAST_RETRY).generate("gemini-x", "p", "s"));
            assertEquals(503, e.statusCode());
            assertTrue(e.getMessage().contains("high demand"), e.getMessage());
            assertEquals(3, server.requests().size());
        }
    }

    @Test
    void doesNotRetryClientErrorsAndNeverLeaksTheKey() throws Exception {
        try (FakeServer server = new FakeServer(r ->
                new Reply(400, "{\"error\":{\"code\":400,\"message\":\"Unsupported thinking level\"}}"))) {
            ModelClientException e = assertThrows(ModelClientException.class,
                    () -> client(server, FAST_RETRY).generate("gemini-x", "p", "s"));
            assertEquals(400, e.statusCode());
            assertTrue(e.getMessage().contains("Unsupported thinking level"));
            assertFalse(e.getMessage().contains("test-key"));
            assertEquals(1, server.requests().size());
        }
    }

    @Test
    void reportsBlockedPromptsAndEmptyAnswers() throws Exception {
        try (FakeServer blocked = new FakeServer(r -> Reply.ok("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}"))) {
            ModelClientException e = assertThrows(ModelClientException.class,
                    () -> client(blocked, FAST_RETRY).generate("gemini-x", "p", "s"));
            assertTrue(e.getMessage().contains("SAFETY"), e.getMessage());
        }
        try (FakeServer empty = new FakeServer(r ->
                Reply.ok("{\"candidates\":[{\"content\":{\"parts\":[]},\"finishReason\":\"MAX_TOKENS\"}]}"))) {
            ModelClientException e = assertThrows(ModelClientException.class,
                    () -> client(empty, FAST_RETRY).generate("gemini-x", "p", "s"));
            assertTrue(e.getMessage().contains("MAX_TOKENS"), e.getMessage());
        }
    }

    @Test
    void rejectsMissingApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new GeminiClient(" "));
        assertThrows(IllegalArgumentException.class, () -> new GeminiClient(null));
    }

    @Test
    void finishReasonIsNullWhenAbsent() throws Exception {
        try (FakeServer server = new FakeServer(r ->
                Reply.ok("{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}"))) {
            ModelResponse response = client(server, FAST_RETRY).generate("gemini-x", "p", "s");
            assertNull(response.finishReason());
            assertNull(response.inputTokens());
        }
    }
}

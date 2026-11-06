package com.crud2gui.statementanalyzer;

import com.crud2gui.statementanalyzer.FakeServer.Reply;
import com.crud2gui.statementanalyzer.client.GroqClient;
import com.crud2gui.statementanalyzer.client.RetryPolicy;
import com.crud2gui.statementanalyzer.exception.ModelClientException;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final RetryPolicy FAST_RETRY = new RetryPolicy(2, Duration.ofMillis(1));
    private static final String OK_BODY = """
            {"choices":[{"message":{"role":"assistant","content":"{\\"types\\":[]}"},
              "finish_reason":"stop"}],"usage":{"prompt_tokens":12,"completion_tokens":8}}
            """;

    private static GroqClient client(FakeServer server) {
        return new GroqClient("test-key", server.uri("/openai/v1"), Duration.ofSeconds(5), FAST_RETRY);
    }

    @Test
    void listsModelsAndKeepsNamespacedIds() throws Exception {
        String models = """
                {"data":[
                  {"id":"llama-3.3-70b-versatile","owned_by":"Meta"},
                  {"id":"meta-llama/llama-4-scout-17b-16e-instruct","owned_by":"Meta"},
                  {"owned_by":"missing-id"}]}
                """;
        try (FakeServer server = new FakeServer(request -> Reply.ok(models))) {
            List<ModelInfo> result = client(server).listModels();

            assertEquals(List.of(
                    new ModelInfo("llama-3.3-70b-versatile", "llama-3.3-70b-versatile"),
                    new ModelInfo("meta-llama/llama-4-scout-17b-16e-instruct",
                            "meta-llama/llama-4-scout-17b-16e-instruct")), result);
            assertEquals("GET", server.requests().get(0).method());
            assertEquals("/openai/v1/models", server.requests().get(0).pathAndQuery());
            assertEquals("Bearer test-key", server.requests().get(0).headers().getFirst("Authorization"));
        }
    }

    @Test
    void sendsChatRequestAndParsesResponseForNamespacedModel() throws Exception {
        String model = "meta-llama/llama-4-scout-17b-16e-instruct";
        String prompt = "Devuelve un objeto JSON";
        String statement = "Enunciado de prueba";
        try (FakeServer server = new FakeServer(request -> Reply.ok(OK_BODY))) {
            ModelResponse response = client(server).generate(model, prompt, statement, GenerationOptions.defaults());

            FakeServer.Recorded request = server.requests().get(0);
            assertEquals("POST", request.method());
            assertEquals("/openai/v1/chat/completions", request.pathAndQuery());
            assertEquals("Bearer test-key", request.headers().getFirst("Authorization"));
            JsonNode body = JSON.readTree(request.body());
            assertEquals(model, body.path("model").asText());
            assertEquals(prompt + "\n\n--- ENUNCIADO DE LA PRÁCTICA ---\n" + statement,
                    body.at("/messages/0/content").asText());
            assertEquals(0.1, body.path("temperature").asDouble(), 1e-9);
            assertEquals("json_object", body.at("/response_format/type").asText());
            assertFalse(request.body().contains("test-key"));

            assertEquals("{\"types\":[]}", response.text());
            assertEquals("stop", response.finishReason());
            assertEquals(12, response.inputTokens());
            assertEquals(8, response.outputTokens());
            assertEquals("groq", response.provider());
        }
    }

    @Test
    void rejectsMalformedModelIdsBeforeSendingRequest() throws Exception {
        try (FakeServer server = new FakeServer(request -> Reply.ok(OK_BODY))) {
            GroqClient client = client(server);
            for (String invalid : List.of("../model", "model/name/extra", "model name", "model?x")) {
                assertThrows(IllegalArgumentException.class,
                        () -> client.generate(invalid, "p", "s"), invalid);
            }
            assertThrows(IllegalArgumentException.class, () -> client.generate(null, "p", "s"));
            assertTrue(server.requests().isEmpty());
        }
    }

    @Test
    void reportsApiErrorsWithoutLeakingTheApiKey() throws Exception {
        try (FakeServer server = new FakeServer(request ->
                new Reply(401, "{\"error\":{\"message\":\"invalid api key\"}}"))) {
            ModelClientException error = assertThrows(ModelClientException.class,
                    () -> client(server).generate("llama-3.3-70b-versatile", "p", "s"));

            assertEquals(401, error.statusCode());
            assertTrue(error.getMessage().contains("invalid api key"));
            assertFalse(error.getMessage().contains("test-key"));
        }
    }

    @Test
    void rejectsMissingApiKey() {
        assertThrows(IllegalArgumentException.class, () -> new GroqClient(" "));
        assertThrows(IllegalArgumentException.class, () -> new GroqClient(null));
    }
}
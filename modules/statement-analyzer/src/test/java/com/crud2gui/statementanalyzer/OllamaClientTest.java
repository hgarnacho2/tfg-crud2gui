package com.crud2gui.statementanalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crud2gui.statementanalyzer.FakeServer.Reply;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

import com.crud2gui.statementanalyzer.client.RetryPolicy;
import com.crud2gui.statementanalyzer.client.OllamaClient;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.exception.ModelClientException;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.model.ModelInfo;


class OllamaClientTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final RetryPolicy FAST_RETRY = new RetryPolicy(2, Duration.ofMillis(1));
    private static final String CHAT_OK = """
            {"model":"qwen3:8b","message":{"role":"assistant","content":"{\\"ok\\":true}"},
             "done":true,"done_reason":"stop","prompt_eval_count":1234,"eval_count":56}
            """;

    private static OllamaClient client(FakeServer server, boolean disableThinking) {
        return new OllamaClient(server.uri(""), Duration.ofSeconds(10), 16384, disableThinking, FAST_RETRY);
    }

    @Test
    void listModelsReadsApiTags() throws Exception {
        String tags = """
                {"models":[{"name":"qwen3:8b","model":"qwen3:8b","size":123},{"name":"llama3.2:latest"}]}
                """;
        try (FakeServer server = new FakeServer(r -> Reply.ok(tags))) {
            List<ModelInfo> models = client(server, true).listModels();
            assertEquals(List.of(new ModelInfo("qwen3:8b", null), new ModelInfo("llama3.2:latest", null)), models);
            assertEquals("GET", server.requests().get(0).method());
            assertEquals("/api/tags", server.requests().get(0).pathAndQuery());
        }
    }

    @Test
    void generateUsesChatEndpointWithJsonFormatAndExplicitContext() throws Exception {
        String prompt = "Prompt \"con\" comillas\ny \\ barras";
        String statement = "Enunciado \u00f1\n{{x}}";
        try (FakeServer server = new FakeServer(r -> Reply.ok(CHAT_OK))) {
            ModelResponse response = client(server, true)
                    .generate("qwen3:8b", prompt, statement, GenerationOptions.defaults().withTemperature(0.3));

            FakeServer.Recorded request = server.requests().get(0);
            assertEquals("POST", request.method());
            assertEquals("/api/chat", request.pathAndQuery());
            JsonNode body = JSON.readTree(request.body());
            assertEquals("qwen3:8b", body.path("model").asText());
            assertEquals(false, body.path("stream").asBoolean(true));
            assertEquals("json", body.path("format").asText());
            assertEquals(false, body.path("think").asBoolean(true));
            assertEquals(0.3, body.at("/options/temperature").asDouble(), 1e-9);
            assertEquals(16384, body.at("/options/num_ctx").asInt());
            assertEquals(1, body.path("messages").size());
            assertEquals("user", body.at("/messages/0/role").asText());
            assertEquals(prompt + "\n\n--- ENUNCIADO DE LA PR\u00c1CTICA ---\n" + statement,
                    body.at("/messages/0/content").asText());

            assertEquals("{\"ok\":true}", response.text());
            assertEquals("stop", response.finishReason());
            assertEquals(1234, response.inputTokens());
            assertEquals(56, response.outputTokens());
            assertEquals("ollama", response.provider());
        }
    }

    @Test
    void thinkIsOmittedWhenNotDisabled() throws Exception {
        try (FakeServer server = new FakeServer(r -> Reply.ok(CHAT_OK))) {
            client(server, false).generate("qwen3:8b", "p", "s");
            assertTrue(JSON.readTree(server.requests().get(0).body()).path("think").isMissingNode());
        }
    }

    @Test
    void reportsServerErrorsWithTheirMessage() throws Exception {
        try (FakeServer server = new FakeServer(r -> new Reply(404, "{\"error\":\"model 'nope' not found\"}"))) {
            ModelClientException e = assertThrows(ModelClientException.class,
                    () -> client(server, true).generate("nope", "p", "s"));
            assertEquals(404, e.statusCode());
            assertTrue(e.getMessage().contains("model 'nope' not found"), e.getMessage());
            assertEquals(1, server.requests().size());
        }
    }

    @Test
    void explainsWhenOllamaIsNotRunning() throws Exception {
        URI dead;
        try (FakeServer server = new FakeServer(r -> Reply.ok("{}"))) {
            dead = server.uri("");
        }
        OllamaClient client = new OllamaClient(dead, Duration.ofSeconds(5), 4096, true, RetryPolicy.none());
        ModelClientException e = assertThrows(ModelClientException.class, client::listModels);
        assertTrue(e.getMessage().contains("Ollama") && e.getMessage().contains("no se pudo conectar"), e.getMessage());
    }

    @Test
    void rejectsBlankModelAndInvalidContext() {
        assertThrows(IllegalArgumentException.class, () -> new OllamaClient().generate(" ", "p", "s"));
        assertThrows(IllegalArgumentException.class,
                () -> new OllamaClient(URI.create("http://x"), Duration.ofSeconds(1), 0, true, RetryPolicy.none()));
    }
}

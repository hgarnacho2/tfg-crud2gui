package com.crud2gui.statementanalyzer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.model.AnalysisResult;
import com.crud2gui.statementanalyzer.client.GeminiClient;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.client.OllamaClient;
import com.crud2gui.statementanalyzer.exception.ModelClientException;
import com.crud2gui.statementanalyzer.prompt.StatementAnalyzer;
import com.crud2gui.statementanalyzer.model.GenerationOptions;


/**
 * Pruebas contra modelos reales. Consumen cuota (Gemini) o minutos de CPU/GPU (Ollama), por eso solo
 * se ejecutan si se pide expresamente:
 *
 * <pre>mvn test -Dllm.tests=true [-Dtest=LlmIntegrationTest]</pre>
 *
 * <p>Modelos: {@code GEMINI_MODEL} (por defecto gemini-3.5-flash-lite) y {@code OLLAMA_MODEL}
 * (por defecto qwen3:8b). Los resultados quedan en la carpeta {@code output/} del modulo.</p>
 */
@EnabledIfSystemProperty(named = "llm.tests", matches = "true")
class LlmIntegrationTest {

    private static final Path PROMPT = Path.of("src/test/resources/prompt.txt");
    private static final Path STATEMENT = Path.of("src/test/resources/statement.txt");
    private static final Path OUTPUT_DIR = Path.of("output");

    @Test
    void geminiAnalyzesTheStatement() throws Exception {
        GeminiClient client;
        try {
            client = GeminiClient.fromEnvironment();
        } catch (java.io.IOException e) {
            assumeTrue(false, e.getMessage());
            return;
        }
        String model = envOrDefault("GEMINI_MODEL", "gemini-3.5-flash-lite");
        List<ModelInfo> available = client.listModels();
        assumeTrue(available.stream().anyMatch(m -> m.id().equals(model)),
                "El modelo " + model + " no esta entre los disponibles: " + available);
        runAndSave(client, model);
    }

    @Test
    void ollamaAnalyzesTheStatement() throws Exception {
        OllamaClient client = OllamaClient.fromEnvironment();
        String model = envOrDefault("OLLAMA_MODEL", "qwen3:8b");
        List<ModelInfo> available;
        try {
            available = client.listModels();
        } catch (ModelClientException e) {
            assumeTrue(false, "Ollama no esta accesible: " + e.getMessage());
            return;
        }
        assumeTrue(available.stream().anyMatch(m -> m.id().equals(model)),
                "El modelo " + model + " no esta instalado en Ollama: " + available);
        runAndSave(client, model);
    }

    private static void runAndSave(ModelClient client, String model) throws Exception {
        AnalysisResult result = new StatementAnalyzer(client)
                .analyze(model, PROMPT, STATEMENT, GenerationOptions.defaults());
        Path file = OUTPUT_DIR.resolve(AnalysisResult.fileNameFor(client.provider(), model));
        result.writeTo(file);

        ModelResponse r = result.response();
        System.out.printf("[%s/%s] %.1f s, tokens %s/%s, fin=%s -> %s%n", r.provider(), r.model(),
                r.elapsed().toMillis() / 1000.0, r.inputTokens(), r.outputTokens(), r.finishReason(),
                file.toAbsolutePath().normalize());
        assertTrue(result.json().path("types").isArray() && !result.json().path("types").isEmpty(),
                "El JSON deberia contener un array 'types' no vacio");
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value.strip();
    }
}

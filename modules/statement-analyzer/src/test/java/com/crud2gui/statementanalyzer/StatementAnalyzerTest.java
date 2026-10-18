package com.crud2gui.statementanalyzer;

import com.crud2gui.common.DotEnv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.AnalysisResult;
import com.crud2gui.statementanalyzer.prompt.StatementAnalyzer;
import com.crud2gui.statementanalyzer.exception.InvalidModelOutputException;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import com.crud2gui.statementanalyzer.model.ModelInfo;

class StatementAnalyzerTest {

    /** Cliente de mentira que devuelve un texto fijo y recuerda lo que recibio. */
    private static final class FakeClient implements ModelClient {
        final List<String> prompts = new ArrayList<>();
        final List<String> statements = new ArrayList<>();
        private final String answer;

        FakeClient(String answer) {
            this.answer = answer;
        }

        @Override
        public String provider() {
            return "fake";
        }

        @Override
        public List<ModelInfo> listModels() {
            return List.of();
        }

        @Override
        public ModelResponse generate(String model, String prompt, String statement, GenerationOptions options) {
            prompts.add(prompt);
            statements.add(statement);
            return new ModelResponse("fake", model, answer, "STOP", 1, 2, Duration.ofMillis(5));
        }
    }

    @Test
    void readsFilesNormalizesLineEndingsAndReturnsParsedJson(@TempDir Path dir) throws Exception {
        Path prompt = dir.resolve("prompt.txt");
        Path statement = dir.resolve("statement.txt");
        Files.writeString(prompt, "L\u00ednea 1\r\nL\u00ednea 2\r\n\r\n", StandardCharsets.UTF_8);
        Files.writeString(statement, "  Enunciado \u00f1\r\nfin\n\n", StandardCharsets.UTF_8);
        FakeClient client = new FakeClient("{\"types\":[{\"id\":\"A\"}]}");

        AnalysisResult result = new StatementAnalyzer(client)
                .analyze("m", prompt, statement, GenerationOptions.defaults());

        assertEquals("L\u00ednea 1\nL\u00ednea 2", client.prompts.get(0));
        assertEquals("Enunciado \u00f1\nfin", client.statements.get(0));
        assertEquals("A", result.json().at("/types/0/id").asText());
    }

    @Test
    void writesPrettyJsonCreatingFolders(@TempDir Path dir) throws Exception {
        AnalysisResult result = new StatementAnalyzer(new FakeClient("{\"a\":{\"b\":[1,2]},\"t\":\"\u00f1\"}"))
                .analyze("m", "p", "s", GenerationOptions.defaults());
        Path out = dir.resolve("output/sub/result.json");

        result.writeTo(out);

        String text = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(text.contains("\n  \"a\""), "debe tener sangria: " + text);
        assertTrue(text.contains("\u00f1"));
    }

    @Test
    void rejectsInvalidJsonKeepingTheRawText() {
        FakeClient client = new FakeClient("Aqui tienes el JSON: {\"a\":1}");
        InvalidModelOutputException e = assertThrows(InvalidModelOutputException.class,
                () -> new StatementAnalyzer(client).analyze("m", "p", "s", GenerationOptions.defaults()));
        assertEquals("Aqui tienes el JSON: {\"a\":1}", e.rawText());
        assertTrue(e.getMessage().contains("finishReason=STOP"));
    }

    @Test
    void rejectsTextAfterTheJsonAndNonObjects() {
        for (String answer : List.of("{\"a\":1} y algo mas", "[1,2]", "\"texto\"", "42")) {
            assertThrows(InvalidModelOutputException.class,
                    () -> new StatementAnalyzer(new FakeClient(answer))
                            .analyze("m", "p", "s", GenerationOptions.defaults()), answer);
        }
    }

    @Test
    void failsClearlyOnMissingOrEmptyFiles(@TempDir Path dir) throws Exception {
        Path empty = dir.resolve("empty.txt");
        Files.writeString(empty, " \n ");
        Path ok = dir.resolve("ok.txt");
        Files.writeString(ok, "texto");
        StatementAnalyzer analyzer = new StatementAnalyzer(new FakeClient("{}"));

        IOException missing = assertThrows(IOException.class,
                () -> analyzer.analyze("m", ok, dir.resolve("nope.txt"), GenerationOptions.defaults()));
        assertTrue(missing.getMessage().contains("enunciado") && missing.getMessage().contains("nope.txt"));
        IOException blank = assertThrows(IOException.class,
                () -> analyzer.analyze("m", empty, ok, GenerationOptions.defaults()));
        assertTrue(blank.getMessage().contains("prompt") && blank.getMessage().contains("vac"));
    }

    @Test
    void outputFileNamesAreSafeOnEveryOperatingSystem() {
        assertEquals("ollama-qwen3_8b.json", AnalysisResult.fileNameFor("ollama", "qwen3:8b"));
        assertEquals("gemini-gemini-3.5-flash-lite.json", AnalysisResult.fileNameFor("gemini", "gemini-3.5-flash-lite"));
        assertEquals("ollama-org_model_v1.json", AnalysisResult.fileNameFor("ollama", "org/model:v1"));
        assertEquals("gemini-gemini-3.5-flash-lite_260111_115701.json",
            AnalysisResult.timestampedFileNameFor("gemini", "gemini-3.5-flash-lite",
                LocalDateTime.of(2026, 1, 11, 11, 57, 1)));
        assertEquals(Path.of("output", "gemini-gemini-3.5-flash-lite",
                "gemini-gemini-3.5-flash-lite_260111_115701.json"),
            StatementAnalyzerMain.timestampedOutputPath("gemini", "gemini-3.5-flash-lite",
                LocalDateTime.of(2026, 1, 11, 11, 57, 1)));
    }

    @Test
    void generationOptionsValidateTheirValues() {
        assertThrows(IllegalArgumentException.class, () -> new GenerationOptions(-0.1, "low"));
        assertThrows(IllegalArgumentException.class, () -> new GenerationOptions(Double.NaN, "low"));
        assertEquals(null, new GenerationOptions(0.2, "  ").thinkingLevel());
    }

    @Test
    void dotEnvParsesCommonFormats(@TempDir Path dir) throws Exception {
        Path env = dir.resolve(".env");
        Files.writeString(env, "\uFEFF# comentario\r\nGEMINI_API_KEY=abc123\r\nexport OTHER = \"con espacios\"\n"
                + "QUOTED='x'\nsin_igual\n=vacio\n", StandardCharsets.UTF_8);
        var values = DotEnv.load(env);
        assertEquals("abc123", values.get("GEMINI_API_KEY"));
        assertEquals("con espacios", values.get("OTHER"));
        assertEquals("x", values.get("QUOTED"));
        assertEquals(3, values.size());
        assertTrue(DotEnv.load(dir.resolve("no-existe")).isEmpty());
    }

    @Test
    void mainReportsUsageAndUnknownProviders() {
        var out = new java.io.ByteArrayOutputStream();
        var err = new java.io.ByteArrayOutputStream();
        var outPs = new java.io.PrintStream(out, true, StandardCharsets.UTF_8);
        var errPs = new java.io.PrintStream(err, true, StandardCharsets.UTF_8);

        assertEquals(2, StatementAnalyzerMain.run(new String[0], outPs, errPs));
        assertEquals(2, StatementAnalyzerMain.run(new String[] {"list", "openai"}, outPs, errPs));
        assertEquals(2, StatementAnalyzerMain.run(new String[] {"analyze", "ollama", "m"}, outPs, errPs));
        assertEquals(2, StatementAnalyzerMain.run(new String[] {"list", "ollama", "--nope"}, outPs, errPs));
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Proveedor desconocido"));
    }
}

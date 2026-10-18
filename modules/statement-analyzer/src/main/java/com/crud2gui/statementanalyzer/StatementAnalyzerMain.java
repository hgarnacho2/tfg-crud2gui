package com.crud2gui.statementanalyzer;

import com.crud2gui.statementanalyzer.client.GeminiClient;
import com.crud2gui.statementanalyzer.client.JsonHttp;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.client.OllamaClient;
import com.crud2gui.statementanalyzer.client.RetryPolicy;

import com.crud2gui.statementanalyzer.exception.ModelClientException;
import com.crud2gui.statementanalyzer.exception.InvalidModelOutputException;

import com.crud2gui.statementanalyzer.model.AnalysisResult;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelClients;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.ModelResponse;

import com.crud2gui.statementanalyzer.prompt.PromptComposer;
import com.crud2gui.statementanalyzer.prompt.StatementAnalyzer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Linea de comandos.
 *
 * <pre>
 * list    &lt;gemini|ollama&gt;
 * analyze &lt;gemini|ollama&gt; &lt;modelo&gt; &lt;prompt.txt&gt; &lt;enunciado.txt&gt; [salida.json]
 *         [--temperature=0.1] [--thinking=low|none]
 * </pre>
 *
 * <p>Sin fichero de salida se escribe {@code output/<proveedor>-<modelo>/<proveedor>-<modelo>_<fecha>.json}.</p>
 */
public final class StatementAnalyzerMain {

    private static final String USAGE = """
            Uso:
              list    <gemini|ollama>
              analyze <gemini|ollama> <modelo> <prompt.txt> <enunciado.txt> [salida.json] \
            [--temperature=0.1] [--thinking=low|none]
            """;

    private StatementAnalyzerMain() { }

    /**
     * Punto de entrada.
     *
     * @param args argumentos de la linea de comandos
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            List<String> positional = new ArrayList<>();
            GenerationOptions options = GenerationOptions.defaults();
            for (String arg : args) {
                if (arg.startsWith("--temperature=")) {
                    options = options.withTemperature(Double.parseDouble(arg.substring("--temperature=".length())));
                } else if (arg.startsWith("--thinking=")) {
                    String level = arg.substring("--thinking=".length());
                    options = options.withThinkingLevel("none".equalsIgnoreCase(level) ? null : level);
                } else if (arg.startsWith("--")) {
                    throw new IllegalArgumentException("Opcion desconocida: " + arg);
                } else {
                    positional.add(arg);
                }
            }
            if (positional.isEmpty()) {
                throw new IllegalArgumentException("Falta el comando");
            }
            return switch (positional.get(0)) {
                case "list" -> list(positional, out);
                case "analyze" -> analyze(positional, options, out, err);
                default -> throw new IllegalArgumentException("Comando desconocido: " + positional.get(0));
            };
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            err.print(USAGE);
            return 2;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            err.println("Interrumpido.");
            return 1;
        } catch (IOException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private static int list(List<String> args, PrintStream out) throws IOException, InterruptedException {
        if (args.size() != 2) {
            throw new IllegalArgumentException("list necesita un proveedor");
        }
        for (ModelInfo model : ModelClients.forProvider(args.get(1)).listModels()) {
            out.println(model.id().equals(model.displayName())
                    ? model.id() : model.id() + "  (" + model.displayName() + ")");
        }
        return 0;
    }

    private static int analyze(List<String> args, GenerationOptions options, PrintStream out, PrintStream err)
            throws IOException, InterruptedException {
        if (args.size() < 5 || args.size() > 6) {
            throw new IllegalArgumentException("analyze necesita proveedor, modelo, prompt y enunciado");
        }
        ModelClient client = ModelClients.forProvider(args.get(1));
        String model = args.get(2);
        Path output = args.size() == 6
                ? Path.of(args.get(5))
            : timestampedOutputPath(client.provider(), model, LocalDateTime.now());
        try {
            AnalysisResult result = new StatementAnalyzer(client)
                    .analyze(model, Path.of(args.get(3)), Path.of(args.get(4)), options);
            result.writeTo(output);
            ModelResponse response = result.response();
            out.printf("Modelo: %s/%s | %.1f s | tokens entrada/salida: %s/%s | fin: %s%n",
                    response.provider(), response.model(), response.elapsed().toMillis() / 1000.0,
                    response.inputTokens(), response.outputTokens(), response.finishReason());
            out.println("Resultado: " + output.toAbsolutePath().normalize());
            return 0;
        } catch (InvalidModelOutputException e) {
            Path raw = Path.of(output + ".invalid.txt");
            Files.writeString(raw, e.rawText(), StandardCharsets.UTF_8);
            err.println("Error: " + e.getMessage());
            err.println("Texto recibido guardado en: " + raw.toAbsolutePath().normalize());
            return 1;
        }
    }

    static Path timestampedOutputPath(String provider, String model, LocalDateTime timestamp) {
        String fileName = AnalysisResult.timestampedFileNameFor(provider, model, timestamp);
        String modelDirectory = AnalysisResult.fileNameFor(provider, model);
        modelDirectory = modelDirectory.substring(0, modelDirectory.lastIndexOf('.'));
        Path outputDirectory = Path.of("output", modelDirectory);
        Path output = outputDirectory.resolve(fileName);
        int collision = 1;
        while (Files.exists(output)) {
            int extensionIndex = fileName.lastIndexOf('.');
            output = outputDirectory.resolve(fileName.substring(0, extensionIndex) + "_" + collision++
                    + fileName.substring(extensionIndex));
        }
        return output;
    }
}

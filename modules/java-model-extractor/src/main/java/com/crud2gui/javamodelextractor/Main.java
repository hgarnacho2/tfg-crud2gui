package com.crud2gui.javamodelextractor;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import com.crud2gui.common.filesystem.OutputFileResolver;
import com.crud2gui.model.ModelIO;
import com.crud2gui.model.ProjectModel;

/**
 * Entrada de linea de comandos del extractor de modelos Java.
 *
 * <p>Analiza recursivamente una carpeta de codigo fuente, genera un {@link ProjectModel}
 * y lo guarda como JSON validado. Si no se indica un fichero de salida, usa la carpeta
 * y el nombre predeterminados de {@link JavaModelOutput}. Si el fichero ya existe,
 * anade un sufijo numerico para no sobrescribirlo.</p>
 *
 * <p>Muestra los parametros recibidos y el resumen tabulado del analisis, los avisos y la
 * ruta del modelo guardado.</p>
 *
 * <p>La ruta de salida no se reserva; si otro proceso la ocupa durante el analisis, la escritura
 * falla sin sobrescribirla.</p>
 */
public final class Main {

    /** Impide instanciar esta clase, que solo proporciona el punto de entrada. */
    private Main() { }

    /**
     * Interpreta los argumentos, ejecuta el analisis y escribe el modelo resultante.
     *
     * <p>Uso: {@code Main <carpeta-con-codigo> [salida.json] [--manager=id]
     * [--config=fichero.json]}.</p>
     *
     * @param args primer argumento: carpeta de codigo; opcionalmente, fichero de salida,
     *             {@code --manager=id} para seleccionar la clase gestora por su identificador cualificado y
     *             {@code --config=fichero.json} para configurar su deteccion
     * @throws IllegalArgumentException si un argumento no se reconoce o la salida explicita no termina en .json
     * @throws Exception si falla la lectura, el analisis, la validacion o la escritura
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: Main <carpeta-con-codigo> [salida.json] [--manager=id] [--config=fichero.json]");
            System.exit(1);
        }
        PrintStream output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        Path root = Path.of(args[0]);
        Path out = null;
        Path configPath = null;
        String managerOverride = null;
        boolean outputProvided = false;
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--manager=")) {
                managerOverride = args[i].substring("--manager=".length());
            } else if (args[i].startsWith("--config=")) {
                configPath = Path.of(args[i].substring("--config=".length()));
            } else if (!args[i].startsWith("--") && !outputProvided) {
                out = Path.of(args[i]);
                outputProvided = true;
            } else {
                throw new IllegalArgumentException("Argumento no reconocido: " + args[i]);
            }
        }

        if (outputProvided) {
            Path fileName = out.getFileName();
            if (fileName == null || !fileName.toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
                throw new IllegalArgumentException("La ruta de salida debe terminar en .json: " + out);
            }
        }

        if (!outputProvided) {
            out = JavaModelOutput.defaultDirectory().resolve(JavaModelOutput.fileName(root));
        }
        out = OutputFileResolver.uniquePath(out.toAbsolutePath().normalize());

        printParameters(output, root, out, outputProvided, managerOverride, configPath);
        output.printf("%nObteniendo modelo ...%n");
        output.printf("  Resultado                Valor%n");
        output.printf("  %-23s  %s%n", "-----------------------", "----------------------------------------------------------");

        ManagerDetectorConfig config = configPath == null
                ? ManagerDetectorConfig.loadDefault() : ManagerDetectorConfig.load(configPath);
        ProjectParser.ProjectParseResult parseResult = new ProjectParser().parseWithFiles(root, managerOverride, config);
        ProjectModel model = parseResult.model();
        String json = ModelIO.toJson(model);
        printRow(output, "Schema", "CRUD2GUI ProjectModel " + ProjectModel.CURRENT_SCHEMA);
        printRow(output, "Resultado Validación", "correcta");
        if (!outputProvided) {
            Files.createDirectories(out.getParent());
        }
        Files.writeString(out, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

        printRow(output, "Ficheros analizados", model.source().filesParsed());
        parseResult.files().forEach(file -> output.printf("    - %s%n", file));
        printRow(output, "Tipos encontrados", model.types().size());
        model.types().forEach(type -> output.printf("    - %s%n", type.id()));
        if (model.analysis() != null && model.analysis().managerClass() != null) {
            printRow(output, "Clase gestora", model.analysis().managerClass().id()
                    + " (" + model.analysis().managerClass().confidence() + ")");
        }
        model.source().warnings().forEach(w -> printRow(output, "AVISO", w));
        printRow(output, "Modelo guardado en", out.toAbsolutePath());
    }

    private static void printRow(PrintStream output, String label, Object value) {
        output.printf("  %-23s  %s%n", label, value);
    }

    private static void printParameters(PrintStream output, Path root, Path out, boolean outputProvided,
            String managerOverride, Path configPath) {
        output.printf("Parámetros recibidos%n");
        output.printf("  Parámetro                Valor%n");
        output.printf("  %-23s  %s%n", "-----------------------", "----------------------------------------------------------");
        printRow(output, "<carpeta-con-codigo>", root);

        String outputName = out.getFileName().toString();
        if (!outputProvided) {
            outputName = "(automático) " + outputName;
        }
        printRow(output, "[salida.json]", outputName + " (Se añadirá _n si ya existe)");
        if (managerOverride != null) {
            printRow(output, "--manager=id", managerOverride);
        }
        if (configPath != null) {
            printRow(output, "--config=fichero.json", configPath);
        }
        if (Boolean.getBoolean("crud2gui.extractor.install")) {
            printRow(output, "--install", "sí");
        }
        if (Boolean.getBoolean("crud2gui.extractor.verbose")) {
            printRow(output, "--verbose", "sí");
        }
    }

}

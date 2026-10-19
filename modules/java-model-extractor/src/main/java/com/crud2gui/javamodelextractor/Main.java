package com.crud2gui.javamodelextractor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.crud2gui.model.ModelIO;
import com.crud2gui.model.ProjectModel;

/**
 * Programa de linea de comandos que analiza una carpeta y escribe el metamodelo en JSON.
 *
 * <p>Uso: {@code Main <carpeta-con-codigo> [salida.json] [--manager=id] [--config=fichero.json]}.
 * Si no se indica fichero de salida, se crea {@code output/model_yyyyMMdd_HHmmss.json}
 * en la raíz de este módulo.</p>
 */
public final class Main {

    private Main() { }

    /**
     * Punto de entrada.
     *
     * @param args carpeta a analizar y, opcionalmente, fichero de salida
     * @throws Exception si la carpeta no existe o no se puede escribir el resultado
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso: Main <carpeta-con-codigo> [salida.json] [--manager=id] [--config=fichero.json]");
            System.exit(1);
        }
        Path root = Path.of(args[0]);
        Path out = defaultOutputPath();
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

        ManagerDetectorConfig config = configPath == null
                ? ManagerDetectorConfig.loadDefault() : ManagerDetectorConfig.load(configPath);
        ProjectModel model = new ProjectParser().parse(root, managerOverride, config);
        ModelIO.write(model, out);

        System.out.printf("Ficheros analizados: %d%n", model.source().filesParsed());
        System.out.printf("Tipos encontrados:   %d%n", model.types().size());
        if (model.analysis() != null && model.analysis().managerClass() != null) {
            System.out.printf("Clase gestora:       %s (%s)%n", model.analysis().managerClass().id(),
                model.analysis().managerClass().confidence());
        }
        model.source().warnings().forEach(w -> System.out.println("AVISO: " + w));
        System.out.println("Modelo escrito en " + out.toAbsolutePath());
    }

    private static Path defaultOutputPath() throws Exception {
        Path codeLocation = Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path moduleRoot = codeLocation.getParent().getParent();
        Path outputDirectory = moduleRoot.resolve("output");
        Files.createDirectories(outputDirectory);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return outputDirectory.resolve("model_" + timestamp + ".json");
    }
}

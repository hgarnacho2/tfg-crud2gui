package com.crud2gui.modelvalidator;

import com.crud2gui.model.ModelIO;

import java.nio.file.Path;

/** Compara el modelo esperado del enunciado con el extraido del codigo Java. */
public final class ModelComparisonMain {

    private ModelComparisonMain() { }

    /**
     * @param args primero el modelo esperado y despues el extraido del codigo
     * @throws Exception si los ficheros no se pueden leer
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Uso: ModelComparisonMain <modelo-esperado.json> <modelo-extraido.json>");
            System.exit(2);
        }

        ModelValidator.Result result = ModelValidator.validate(
                ModelIO.read(Path.of(args[0])), ModelIO.read(Path.of(args[1])));
        System.out.println("Resultado: " + (result.isValid() ? "VALID" : "INVALID"));
        for (ModelValidator.Finding finding : result.findings()) {
            System.out.printf("[%s] %s (%s): %s%n", finding.severity(), finding.path(),
                    finding.code(), finding.message());
        }
        if (!result.isValid()) {
            System.exit(1);
        }
    }
}
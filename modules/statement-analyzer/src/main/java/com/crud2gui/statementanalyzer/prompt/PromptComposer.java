package com.crud2gui.statementanalyzer.prompt;

/** Construye el texto unico que se envia al modelo a partir del prompt y del enunciado. */
public final class PromptComposer {

    /** Separador entre el prompt y el enunciado. */
    public static final String STATEMENT_SEPARATOR = "\n\n--- ENUNCIADO DE LA PR\u00c1CTICA ---\n";

    private PromptComposer() { }

    /**
     * Une prompt y enunciado.
     *
     * @param prompt    consulta para el modelo
     * @param statement texto del enunciado
     * @return {@code prompt + separador + enunciado}, sin escapar: la serializacion JSON la hace Jackson
     * @throws IllegalArgumentException si alguno de los dos esta vacio
     */
    public static String compose(String prompt, String statement) {
        requireText(prompt, "prompt");
        requireText(statement, "enunciado");
        return prompt + STATEMENT_SEPARATOR + statement;
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El " + label + " no puede estar vacio");
        }
    }
}

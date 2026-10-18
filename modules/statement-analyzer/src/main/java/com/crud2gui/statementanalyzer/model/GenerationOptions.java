package com.crud2gui.statementanalyzer.model;

/**
 * Parametros de generacion comunes a los proveedores.
 *
 * @param temperature   temperatura de muestreo; valores bajos dan respuestas mas deterministas
 * @param thinkingLevel nivel de razonamiento ({@code low}, {@code high}, ...) de la serie Gemini 3;
 *                      {@code null} para no enviarlo. Los modelos Gemini 2.5 usan otro parametro
 *                      ({@code thinkingBudget}) y rechazan este, y Ollama lo ignora
 */
public record GenerationOptions(double temperature, String thinkingLevel) {

    /** Temperatura por defecto. */
    public static final double DEFAULT_TEMPERATURE = 0.1;
    /** Nivel de razonamiento por defecto. */
    public static final String DEFAULT_THINKING_LEVEL = "low";

    /** Valida la temperatura y normaliza el nivel de razonamiento en blanco a {@code null}. */
    public GenerationOptions {
        if (Double.isNaN(temperature) || temperature < 0) {
            throw new IllegalArgumentException("La temperatura debe ser un numero mayor o igual que 0");
        }
        if (thinkingLevel != null && thinkingLevel.isBlank()) {
            thinkingLevel = null;
        }
    }

    /**
     * Opciones por defecto: temperatura 0.1 y razonamiento {@code low}.
     *
     * @return opciones por defecto
     */
    public static GenerationOptions defaults() {
        return new GenerationOptions(DEFAULT_TEMPERATURE, DEFAULT_THINKING_LEVEL);
    }

    /**
     * Copia con otra temperatura.
     *
     * @param value nueva temperatura
     * @return opciones nuevas
     */
    public GenerationOptions withTemperature(double value) {
        return new GenerationOptions(value, thinkingLevel);
    }

    /**
     * Copia con otro nivel de razonamiento.
     *
     * @param level nuevo nivel, o {@code null} para no enviarlo
     * @return opciones nuevas
     */
    public GenerationOptions withThinkingLevel(String level) {
        return new GenerationOptions(temperature, level);
    }
}

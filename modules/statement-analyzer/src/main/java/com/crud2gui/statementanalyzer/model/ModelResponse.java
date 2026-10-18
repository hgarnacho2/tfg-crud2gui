package com.crud2gui.statementanalyzer.model;

import java.time.Duration;

/**
 * Respuesta de un modelo.
 *
 * @param provider     proveedor que la genero
 * @param model        modelo usado
 * @param text         texto generado (se espera un JSON)
 * @param finishReason motivo de fin informado por el proveedor ({@code STOP}, {@code MAX_TOKENS},
 *                     {@code length}, ...); puede ser {@code null}
 * @param inputTokens  tokens de entrada consumidos, si el proveedor los informa
 * @param outputTokens tokens de salida generados, si el proveedor los informa
 * @param elapsed      tiempo total de la llamada, reintentos incluidos
 */
public record ModelResponse(String provider, String model, String text, String finishReason,
                            Integer inputTokens, Integer outputTokens, Duration elapsed) { }

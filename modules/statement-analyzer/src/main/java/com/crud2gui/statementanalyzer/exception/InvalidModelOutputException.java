package com.crud2gui.statementanalyzer.exception;

import java.io.IOException;

import com.crud2gui.statementanalyzer.model.ModelResponse;

/** El modelo respondio, pero su texto no es un objeto JSON valido. Conserva el texto para no perderlo. */
public class InvalidModelOutputException extends IOException {

    private static final long serialVersionUID = 1L;

    private final transient ModelResponse response;

    /**
     * Crea la excepcion.
     *
     * @param message  descripcion del problema
     * @param response respuesta original del modelo
     * @param cause    causa del fallo de analisis, o {@code null}
     */
    public InvalidModelOutputException(String message, ModelResponse response, Throwable cause) {
        super(message + " (finishReason=" + response.finishReason() + ")", cause);
        this.response = response;
    }

    /**
     * Respuesta original del modelo.
     *
     * @return la respuesta completa
     */
    public ModelResponse response() {
        return response;
    }

    /**
     * Texto sin interpretar devuelto por el modelo.
     *
     * @return el texto bruto
     */
    public String rawText() {
        return response.text();
    }
}

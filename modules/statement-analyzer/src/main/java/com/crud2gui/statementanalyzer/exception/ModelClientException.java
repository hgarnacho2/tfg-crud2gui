package com.crud2gui.statementanalyzer.exception;

import java.io.IOException;

/** Error al hablar con un proveedor de modelos. */
public class ModelClientException extends IOException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;

    /**
     * Error con codigo HTTP.
     *
     * @param message    descripcion del error
     * @param statusCode codigo HTTP recibido, o 0 si no hubo respuesta
     */
    public ModelClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Error sin respuesta HTTP (conexion, tiempo de espera, ...).
     *
     * @param message descripcion del error
     * @param cause   causa original
     */
    public ModelClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    /**
     * Codigo HTTP de la respuesta.
     *
     * @return el codigo, o 0 si no hubo respuesta
     */
    public int statusCode() {
        return statusCode;
    }
}

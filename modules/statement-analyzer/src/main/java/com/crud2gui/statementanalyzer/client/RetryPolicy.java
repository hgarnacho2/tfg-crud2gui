package com.crud2gui.statementanalyzer.client;

import java.time.Duration;
import java.util.Objects;

/**
 * Reintentos ante errores transitorios del servidor (HTTP 429, 500, 502, 503 y 504), con espera
 * exponencial. No se reintentan los tiempos de espera agotados ni los errores 4xx.
 *
 * @param maxAttempts numero total de intentos, incluido el primero
 * @param baseDelay   espera antes del segundo intento; se duplica en cada intento posterior
 */
public record RetryPolicy(int maxAttempts, Duration baseDelay) {

    /** Valida los parametros. */
    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts debe ser al menos 1");
        }
        Objects.requireNonNull(baseDelay, "baseDelay");
        if (baseDelay.isNegative()) {
            throw new IllegalArgumentException("baseDelay no puede ser negativo");
        }
    }

    /**
     * Tres intentos con 2 s y 4 s de espera.
     *
     * @return politica por defecto
     */
    public static RetryPolicy defaults() {
        return new RetryPolicy(3, Duration.ofSeconds(2));
    }

    /**
     * Un unico intento.
     *
     * @return politica sin reintentos
     */
    public static RetryPolicy none() {
        return new RetryPolicy(1, Duration.ZERO);
    }

    /**
     * Espera antes del intento siguiente a uno fallido.
     *
     * @param failedAttempt numero (desde 1) del intento que acaba de fallar
     * @return tiempo de espera
     */
    Duration delayAfter(int failedAttempt) {
        int doublings = Math.min(Math.max(failedAttempt - 1, 0), 20);
        return baseDelay.multipliedBy(1L << doublings);
    }
}

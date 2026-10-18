package com.crud2gui.statementanalyzer.model;

import java.io.IOException;
import java.util.Locale;

import com.crud2gui.statementanalyzer.client.OllamaClient;
import com.crud2gui.statementanalyzer.client.GeminiClient;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.client.GroqClient;

/** Fabrica de clientes a partir del nombre del proveedor. */
public final class ModelClients {

    private ModelClients() { }

    /**
     * Crea el cliente de un proveedor con la configuracion del entorno.
     *
     * @param provider {@code gemini} u {@code ollama} (sin distinguir mayusculas)
     * @return el cliente
     * @throws IOException              si falta la configuracion necesaria (por ejemplo la API key)
     * @throws IllegalArgumentException si el proveedor no existe
     */
    public static ModelClient forProvider(String provider) throws IOException {
        String name = provider == null ? "" : provider.strip().toLowerCase(Locale.ROOT);
        return switch (name) {
            case GeminiClient.PROVIDER -> GeminiClient.fromEnvironment();
            case OllamaClient.PROVIDER -> OllamaClient.fromEnvironment();
            case GroqClient.PROVIDER -> GroqClient.fromEnvironment();
            default -> throw new IllegalArgumentException("Proveedor desconocido: '" + provider
                    + "' (use " + GeminiClient.PROVIDER + " u " + OllamaClient.PROVIDER + " u " + GroqClient.PROVIDER + ")");
        };
    }
}

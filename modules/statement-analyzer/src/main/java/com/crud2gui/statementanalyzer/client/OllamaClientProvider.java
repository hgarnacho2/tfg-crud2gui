package com.crud2gui.statementanalyzer.client;

import java.io.IOException;

/** Registro del proveedor Ollama local. */
public final class OllamaClientProvider implements ModelClientProvider {

    @Override
    public String id() {
        return OllamaClient.PROVIDER;
    }

    @Override
    public String displayName() {
        return "Ollama (local)";
    }

    @Override
    public ModelClient createClient() throws IOException {
        return OllamaClient.fromEnvironment();
    }
}
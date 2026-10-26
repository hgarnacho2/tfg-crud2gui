package com.crud2gui.statementanalyzer.client;

import java.io.IOException;

/** Registro del proveedor Groq. */
public final class GroqClientProvider implements ModelClientProvider {

    @Override
    public String id() {
        return GroqClient.PROVIDER;
    }

    @Override
    public String displayName() {
        return "Groq";
    }

    @Override
    public ModelClient createClient() throws IOException {
        return GroqClient.fromEnvironment();
    }
}
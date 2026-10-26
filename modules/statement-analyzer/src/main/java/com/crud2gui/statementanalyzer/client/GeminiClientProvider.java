package com.crud2gui.statementanalyzer.client;

import java.io.IOException;

/** Registro del proveedor Google Gemini. */
public final class GeminiClientProvider implements ModelClientProvider {

    @Override
    public String id() {
        return GeminiClient.PROVIDER;
    }

    @Override
    public String displayName() {
        return "Google Gemini";
    }

    @Override
    public ModelClient createClient() throws IOException {
        return GeminiClient.fromEnvironment();
    }
}
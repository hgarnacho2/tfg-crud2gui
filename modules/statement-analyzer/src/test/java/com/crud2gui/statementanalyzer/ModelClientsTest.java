package com.crud2gui.statementanalyzer;

import com.crud2gui.statementanalyzer.model.ModelClients;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelClientsTest {

    @Test
    void discoversRegisteredProvidersThroughServiceLoader() {
        assertEquals(List.of("gemini", "groq", "ollama"),
                ModelClients.providers().stream().map(provider -> provider.id()).toList());
    }

    @Test
    void findsProviderWithoutCaseSensitivity() {
        assertEquals("ollama", ModelClients.provider("OLLAMA").id());
    }

    @Test
    void rejectsUnknownProviderWithAvailableProviders() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ModelClients.provider("unknown"));

        assertTrue(exception.getMessage().contains("gemini, groq, ollama"));
    }
}
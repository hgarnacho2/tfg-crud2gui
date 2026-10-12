package com.crud2gui.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DotEnvTest {

    @Test
    void loadSingleFile(@TempDir Path tempDir) throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "KEY1=value1\n# Comentario\nKEY2 = \"quoted_value\"\nexport KEY3='single_quoted'");

        Map<String, String> result = DotEnv.load(envFile);

        assertEquals(3, result.size());
        assertEquals("value1", result.get("KEY1"));
        assertEquals("quoted_value", result.get("KEY2"));
        assertEquals("single_quoted", result.get("KEY3"));
    }

    @Test
    void loadNonExistentFileReturnsEmpty(@TempDir Path tempDir) throws IOException {
        Path nonExistent = tempDir.resolve("non-existent.env");
        Map<String, String> result = DotEnv.load(nonExistent);
        assertTrue(result.isEmpty());
    }

    @Test
    void loadMultipleFilesWithOverride(@TempDir Path tempDir) throws IOException {
        Path publicFile = tempDir.resolve(".env.public");
        Path privateFile = tempDir.resolve(".env.private");

        Files.writeString(publicFile, "SHARED_KEY=public_value\nPUBLIC_ONLY=only_public");
        Files.writeString(privateFile, "SHARED_KEY=private_override\nPRIVATE_ONLY=only_private");

        Map<String, String> result = DotEnv.load(publicFile, privateFile);

        assertEquals(3, result.size());
        assertEquals("private_override", result.get("SHARED_KEY")); // Sobrescrito por private
        assertEquals("only_public", result.get("PUBLIC_ONLY"));
        assertEquals("only_private", result.get("PRIVATE_ONLY"));
    }

    @Test
    void loadDefaultsInWithMissingPrivate(@TempDir Path tempDir) throws IOException {
        Path publicFile = tempDir.resolve(".env.public");
        Files.writeString(publicFile, "API_KEY=public_key_value");

        // No creamos env.private para probar que es opcional
        Map<String, String> result = DotEnv.loadDefaultsIn(tempDir);

        assertEquals(1, result.size());
        assertEquals("public_key_value", result.get("API_KEY"));
    }
}

package com.crud2gui.javamodelextractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @TempDir
    Path directory;

    @Test
    void defaultOutputUsesConfiguredDirectoryAndKeepsExistingFiles() throws Exception {
        Assumptions.assumeTrue(System.getenv("JAVA_MODEL_OUTPUT_DIR") == null
                || System.getenv("JAVA_MODEL_OUTPUT_DIR").isBlank());
        Path source = source();
        Path output = directory.resolve("modelos");
        String previous = System.getProperty("JAVA_MODEL_OUTPUT_DIR");
        try {
            System.setProperty("JAVA_MODEL_OUTPUT_DIR", output.toString());
            Main.main(new String[] { source.toString() });
            Path original = output.resolve("modelo-codigo.json");
            String originalJson = Files.readString(original);
            Main.main(new String[] { source.toString() });

            assertEquals(originalJson, Files.readString(original));
            assertTrue(Files.isRegularFile(output.resolve("modelo-codigo_1.json")));
        } finally {
            if (previous == null) {
                System.clearProperty("JAVA_MODEL_OUTPUT_DIR");
            } else {
                System.setProperty("JAVA_MODEL_OUTPUT_DIR", previous);
            }
        }
    }

    @Test
    void explicitOutputKeepsExistingFiles() throws Exception {
        Path source = source();
        Path output = directory.resolve("resultado.json");
        Files.writeString(output, "contenido anterior");
        Files.writeString(directory.resolve("resultado_1.json"), "otra salida");

        Main.main(new String[] { source.toString(), output.toString() });

        assertEquals("contenido anterior", Files.readString(output));
        assertEquals("otra salida", Files.readString(directory.resolve("resultado_1.json")));
        assertTrue(Files.isRegularFile(directory.resolve("resultado_2.json")));
    }

    @Test
    void explicitOutputRequiresJsonExtension() throws Exception {
        Path source = source();
        for (String fileName : new String[] { "8-java_code_model", "resultado.txt" }) {
            Path output = directory.resolve(fileName);
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> Main.main(new String[] { source.toString(), output.toString() }));

            assertTrue(error.getMessage().contains("debe terminar en .json"));
            assertFalse(Files.exists(output));
        }
    }

    @Test
    void defaultOutputReadsDotEnv() throws Exception {
        Assumptions.assumeTrue(System.getenv("JAVA_MODEL_OUTPUT_DIR") == null
                || System.getenv("JAVA_MODEL_OUTPUT_DIR").isBlank());
        Path source = source();
        Path output = directory.resolve("configurado");
        Files.writeString(directory.resolve(".env.public"), "JAVA_MODEL_OUTPUT_DIR=" + output);
        String previousDir = System.getProperty("user.dir");
        String previousOutput = System.getProperty("JAVA_MODEL_OUTPUT_DIR");
        try {
            System.setProperty("user.dir", directory.toString());
            System.clearProperty("JAVA_MODEL_OUTPUT_DIR");
            Main.main(new String[] { source.toString() });
            assertTrue(Files.isRegularFile(output.resolve("modelo-codigo.json")));
        } finally {
            System.setProperty("user.dir", previousDir);
            if (previousOutput != null) {
                System.setProperty("JAVA_MODEL_OUTPUT_DIR", previousOutput);
            }
        }
    }

    @Test
    void showsParametersAndTheFullUniqueOutputPath() throws Exception {
        Path source = source();
        Path requested = directory.resolve("salida.json");
        Files.writeString(requested, "anterior");
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream previousOut = System.out;
        try (PrintStream output = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            Main.main(new String[] { source.toString(), requested.toString() });
        } finally {
            System.setOut(previousOut);
        }

        String text = captured.toString(StandardCharsets.UTF_8);
        Path actual = directory.resolve("salida_1.json");
        assertEquals(directory.resolve("salida_1.json"), actual);
        assertEquals("anterior", Files.readString(requested));
        assertTrue(Files.isRegularFile(actual));
        assertTrue(text.contains("[salida.json]"));
        assertTrue(text.contains("salida_1.json (Se añadirá _n si ya existe)"));
        assertTrue(text.contains("Obteniendo modelo ..."));
        assertTrue(text.contains(String.format("  %-23s  %s%n", "Schema", "CRUD2GUI ProjectModel 1.0")));
        assertTrue(text.contains(String.format("  %-23s  %s%n", "Resultado Validación", "correcta")));
        assertTrue(text.contains(String.format("  %-23s  %s%n", "Ficheros analizados", 1)));
        assertTrue(text.contains("    - Ejemplo.java"));
        assertTrue(text.contains(String.format("  %-23s  %s%n", "Tipos encontrados", 1)));
        assertTrue(text.contains("    - Ejemplo"));
        assertTrue(text.contains(String.format("  %-23s  %s%n", "Modelo guardado en", actual)));
    }

    private Path source() throws Exception {
        Path source = Files.createDirectories(directory.resolve("codigo"));
        Files.writeString(source.resolve("Ejemplo.java"), "class Ejemplo {}");
        return source;
    }
}

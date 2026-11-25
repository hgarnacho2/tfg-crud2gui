package com.crud2gui.common.filesystem;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputFileResolverTest {

    @Test
    void selectsFirstAvailableNameWithoutModifyingExistingFiles(@TempDir Path directory) throws Exception {
        Path requested = directory.resolve("modelo.json");
        assertEquals(requested, OutputFileResolver.uniquePath(requested));
        Files.writeString(requested, "original");
        Files.writeString(directory.resolve("modelo_1.json"), "first");
        assertEquals(directory.resolve("modelo_2.json"), OutputFileResolver.uniquePath(requested));
        assertEquals("original", Files.readString(requested));
        assertEquals("first", Files.readString(directory.resolve("modelo_1.json")));
    }

    @Test
    void handlesExtensionlessAndHiddenNamesAndExistingDirectories(@TempDir Path directory) throws Exception {
        Path extensionless = Files.createFile(directory.resolve("modelo"));
        Path hidden = Files.createFile(directory.resolve(".modelo"));
        Path existingDirectory = Files.createDirectory(directory.resolve("datos.json"));
        assertEquals(directory.resolve("modelo_1"), OutputFileResolver.uniquePath(extensionless));
        assertEquals(directory.resolve(".modelo_1"), OutputFileResolver.uniquePath(hidden));
        assertEquals(directory.resolve("datos_1.json"), OutputFileResolver.uniquePath(existingDirectory));
    }
}

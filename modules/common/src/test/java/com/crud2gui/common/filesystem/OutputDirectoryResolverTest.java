package com.crud2gui.common.filesystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputDirectoryResolverTest {

    @Test
    void resolvesConfiguredAndDefaultDirectories(@TempDir Path directory) throws Exception {
        Path root = directory.resolve("project");
        Path module = Files.createDirectories(root.resolve("modules").resolve("example"));
        Files.writeString(module.resolve("pom.xml"), "<project/>");
        Path working = Files.createDirectories(root.resolve("modules").resolve("desktop-app"));
        Path absolute = directory.resolve("custom-absolute").toAbsolutePath();
        Path defaultOutput = Path.of("modules", "example", "output");
        Predicate<Path> rootMatches = path -> Files.isRegularFile(path.resolve("modules/example/pom.xml"));

        assertEquals(root.resolve("custom").resolve("models"),
                OutputDirectoryResolver.resolve(working, rootMatches, defaultOutput, "custom/models"));
        assertEquals(absolute, OutputDirectoryResolver.resolve(working, rootMatches, defaultOutput, absolute.toString()));
        assertEquals(root.resolve(defaultOutput), OutputDirectoryResolver.resolve(working, rootMatches, defaultOutput, null));

        Path standalone = Files.createDirectories(directory.resolve("standalone"));
        assertEquals(standalone.resolve("output"), OutputDirectoryResolver.resolve(standalone, rootMatches, defaultOutput, null));
        assertEquals(standalone.resolve("custom"), OutputDirectoryResolver.resolve(standalone, rootMatches, defaultOutput, "custom"));
        assertEquals(absolute, OutputDirectoryResolver.resolve(standalone, rootMatches, defaultOutput, absolute.toString()));
    }

    @Test
    void supportsDirectoryMarkers(@TempDir Path directory) throws Exception {
        Path root = directory.resolve("project");
        Files.createDirectories(root.resolve("modules").resolve("model-validator"));
        Path working = Files.createDirectories(root.resolve("nested"));
        assertEquals(root.resolve("modules").resolve("model-validator").resolve("output"),
                OutputDirectoryResolver.resolve(working,
                        path -> Files.isDirectory(path.resolve("modules/model-validator")),
                        Path.of("modules", "model-validator", "output"), null));
    }
}

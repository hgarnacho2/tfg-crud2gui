package com.crud2gui.javamodelextractor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Busca ficheros {@code .java} bajo una carpeta, a cualquier profundidad, sin suponer una estructura
 * concreta ({@code src/main/java}, {@code src/java/es}, etc.): el paquete de cada tipo se obtiene
 * despues, de la declaracion {@code package} del propio fichero.
 *
 * <p>Se omiten las carpetas ocultas (que empiezan por punto) y las de {@link #DEFAULT_EXCLUDED},
 * por ser salidas de compilacion o dependencias. Tambien se ignoran {@code module-info.java} y
 * {@code package-info.java}.</p>
 */
public final class FolderScanner {

    /** Nombres de carpeta que se omiten por defecto: salida de compilacion y dependencias. */
    public static final Set<String> DEFAULT_EXCLUDED =
            Set.of("target", "build", "out", "bin", "node_modules");

    private final Set<String> excluded;

    /** Crea un escaner que omite las carpetas de {@link #DEFAULT_EXCLUDED}. */
    public FolderScanner() {
        this(DEFAULT_EXCLUDED);
    }

    /**
     * Crea un escaner con una lista propia de carpetas excluidas.
     *
     * @param excludedDirNames nombres de carpeta (no rutas) que se omiten, junto a las ocultas
     */
    public FolderScanner(Set<String> excludedDirNames) {
        this.excluded = Set.copyOf(excludedDirNames);
    }

    /**
     * Busca todos los ficheros {@code .java} bajo la carpeta raiz.
     *
     * @param root carpeta donde empezar a buscar
     * @return rutas de los ficheros encontrados, ordenadas para que el resultado sea determinista
     * @throws IOException si {@code root} no es una carpeta o no se puede recorrer
     */
    public List<Path> findJavaFiles(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IOException("No es una carpeta: " + root);
        }
        List<Path> result = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(root)) {
                    String name = dir.getFileName().toString();
                    if (name.startsWith(".") || excluded.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString();
                if (name.endsWith(".java")
                        && !name.equals("module-info.java")
                        && !name.equals("package-info.java")) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        Collections.sort(result);
        return result;
    }
}

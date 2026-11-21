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
 * Busca recursivamente ficheros {@code .java} bajo una carpeta raiz (root).
 * El paquete de cada tipo no se obtiene de las carpetas físicas, se obtiene despues, en la declaracion
 * {@code package} del propio fichero.
 *
 * <p>Se omiten las carpetas ocultas (cuyo nombre empieza por punto) y las indicadas en
 * {@link #DEFAULT_EXCLUDED_FOLDER_NAMES}, por ser normalmente salidas de compilacion o dependencias.
 * Tambien se ignoran los ficheros indicados en {@link #DEFAULT_EXCLUDED_FILE_NAMES}. Los ficheros o
 * carpetas que no se puedan leer se omiten y la busqueda continua.</p>
 *
 * <p>Proyecto: {@code CRUD2GUI}.</p>
 *
 * @author hgarnacho
 */
public final class FolderScanner {

    /**
     * Nombres de carpeta que se omiten por defecto porque suelen contener resultados de compilacion
     * o dependencias descargadas.
     */
    public static final Set<String> DEFAULT_EXCLUDED_FOLDER_NAMES =
            Set.of("target", "build", "out", "bin", "node_modules");

    /** Nombres de archivo que se omiten porque contienen metadatos de Java, no tipos del proyecto. */
    public static final Set<String> DEFAULT_EXCLUDED_FILE_NAMES =
            Set.of("module-info.java", "package-info.java");

    /**
     * Alias de {@link #DEFAULT_EXCLUDED_FOLDER_NAMES} conservado por compatibilidad.
     *
     * @deprecated Usa {@link #DEFAULT_EXCLUDED_FOLDER_NAMES}.
     */
    @Deprecated
    public static final Set<String> DEFAULT_EXCLUDED = DEFAULT_EXCLUDED_FOLDER_NAMES;

    /** Nombres de carpeta que esta instancia excluye, ademas de las carpetas ocultas. */
    private final Set<String> excludedDirectoryNames;

    /**
     * Crea un escaner con las exclusiones predeterminadas de {@link #DEFAULT_EXCLUDED_FOLDER_NAMES}.
     */
    public FolderScanner() {
        // Llama al constructor principal con las exclusiones predeterminadas
        this(DEFAULT_EXCLUDED_FOLDER_NAMES);
    }

    /**
     * Crea un escaner con nombres de carpeta excluidos personalizados. Esta lista sustituye a
     * {@link #DEFAULT_EXCLUDED_FOLDER_NAMES}; las carpetas ocultas se omiten en cualquier caso.
     *
     * @param excludedDirectoryNames nombres de carpeta (no rutas) que se omitiran
     */
    public FolderScanner(Set<String> excludedDirectoryNames) {
        this.excludedDirectoryNames = Set.copyOf(excludedDirectoryNames);
    }

    /**
     * Recorre la carpeta raiz y sus subcarpetas para encontrar ficheros con extension {@code .java}.
     * Omite las carpetas ocultas y excluidas, asi como los archivos de
     * {@link #DEFAULT_EXCLUDED_FILE_NAMES}. Los errores al visitar ficheros o carpetas individuales
     * no detienen el recorrido. El resultado se ordena para que sea determinista.
     *
     * @param root carpeta donde empezar a buscar
     * @return rutas de los ficheros encontrados, ordenadas por ruta
     * @throws IOException si {@code root} no es una carpeta o falla el recorrido
     */
    public List<Path> findJavaFiles(Path root) throws IOException {

        if (!Files.isDirectory(root)) {
            throw new IOException("No es una carpeta: " + root);
        }
        //Lista de ficheros Java encontrados
        List<Path> result = new ArrayList<>();
        // Recorrer el arbol de ficheros a partir de la carpeta raiz
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            //walkFileTree llama a este metodo antes de visitar una carpeta
            //Si devuelve SKIP_SUBTREE, no se visitan sus subcarpetas
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(root)) {
                    String name = dir.getFileName().toString();
                    //Excluye carpetas ocultas y las que estan en la lista de exclusiones
                    if (name.startsWith(".") || excludedDirectoryNames.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                // Continuar con la visita de la carpeta si no se ha excluido
                return FileVisitResult.CONTINUE;
            }

            @Override
            //walkFileTree llama a este metodo para cada fichero encontrado
            //Si devuelve CONTINUE, se sigue con el siguiente fichero
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String name = file.getFileName().toString();
                // Comprobar si el fichero es un archivo Java y no está en la lista de exclusión
                // En caso afirmativo, añadirlo a la lista de resultados
                if (name.endsWith(".java") && !DEFAULT_EXCLUDED_FILE_NAMES.contains(name)) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            //walkFileTree llama a este metodo si falla la visita de un fichero
            //Se puede usar para registrar errores, pero en este caso se ignoran y se continúa con el siguiente fichero
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
        // Devolver la lista de ficheros Java encontrados, ordenada alfabéticamente
        Collections.sort(result);
        return result;
    }
}

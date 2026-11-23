package com.crud2gui.common.path;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

/** Resuelve directorios de salida relativos a la raiz de un proyecto o al directorio de trabajo. */
public final class OutputDirectoryResolver {

    private OutputDirectoryResolver() { }

    /**
     * Busca la raiz ascendiendo desde el directorio de trabajo y resuelve el destino configurado.
     * Sin configuracion, usa el destino del proyecto o {@code output} si no se encuentra la raiz.
     *
     * @param workingDirectory directorio desde el que comienza la busqueda
     * @param isProjectRoot comprueba si un directorio es la raiz del proyecto buscado
     * @param projectDefault destino predeterminado relativo a la raiz
     * @param configuredPath destino configurado, absoluto o relativo; {@code null} si no existe
     * @return ruta absoluta normalizada
     */
    public static Path resolve(Path workingDirectory, Predicate<Path> isProjectRoot,
                               Path projectDefault, String configuredPath) {
        Path start = Objects.requireNonNull(workingDirectory, "workingDirectory").toAbsolutePath().normalize();
        Objects.requireNonNull(isProjectRoot, "isProjectRoot");
        Objects.requireNonNull(projectDefault, "projectDefault");
        Path root = start;
        while (root != null && !isProjectRoot.test(root)) {
            root = root.getParent();
        }
        Path base = root == null ? start : root;
        Path destination = configuredPath != null ? Path.of(configuredPath)
                : root == null ? Path.of("output") : projectDefault;
        return base.resolve(destination).normalize();
    }
}

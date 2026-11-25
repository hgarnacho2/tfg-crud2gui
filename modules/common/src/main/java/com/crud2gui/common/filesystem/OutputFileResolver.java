package com.crud2gui.common.filesystem;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Resuelve nombres de salida sin sustituir rutas existentes. */
public final class OutputFileResolver {

    private OutputFileResolver() { }

    /**
     * Devuelve una ruta disponible, añadiendo _1, _2, etc. antes de la extension.
     * La ruta no se reserva: otro proceso podria crearla antes de escribir.
     *
     * @param requestedPath ruta solicitada
     * @return ruta solicitada o una alternativa disponible
     */
    public static Path uniquePath(Path requestedPath) {
        String fileName = requestedPath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        String extension = extensionIndex > 0 ? fileName.substring(extensionIndex) : "";
        Path candidate = requestedPath;
        int suffix = 1;
        while (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            candidate = requestedPath.resolveSibling(baseName + "_" + suffix++ + extension);
        }
        return candidate;
    }
}

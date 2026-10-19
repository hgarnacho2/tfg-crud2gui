package com.crud2gui.model;

import java.util.List;

/**
 * Origen del modelo y datos de la ejecucion que lo genero.
 *
 * @param root        carpeta analizada
 * @param generatedAt marca de tiempo en formato ISO-8601
 * @param filesParsed numero de ficheros {@code .java} encontrados y analizados
 * @param warnings    avisos, como ficheros con errores de sintaxis; no interrumpen el proceso
 */
public record SourceInfo(String root, String generatedAt, int filesParsed, List<String> warnings) {

    /**
     * Normaliza los avisos: {@code null} se convierte en una lista vacia inmutable.
     */
    public SourceInfo {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

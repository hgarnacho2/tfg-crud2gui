package com.crud2gui.model;

import java.util.List;
import java.util.Map;

/**
 * Comentario Javadoc descompuesto en descripcion y etiquetas de bloque.
 *
 * @param description texto principal del comentario, sin las etiquetas
 * @param tags etiquetas de bloque (param, return, author, throws...) y la lista de sus apariciones;
 *             en {@code param} cada valor incluye el nombre del parametro seguido de su descripcion
 */
public record JavadocInfo(String description, Map<String, List<String>> tags) {

    /**
     * Normaliza las etiquetas: un mapa {@code null} se convierte en un mapa vacio inmutable.
     */
    public JavadocInfo {
        tags = tags == null ? Map.of() : Map.copyOf(tags);
    }
}

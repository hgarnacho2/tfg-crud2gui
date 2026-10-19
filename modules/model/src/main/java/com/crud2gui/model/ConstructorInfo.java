package com.crud2gui.model;

import java.util.List;

/**
 * Constructor de un tipo.
 *
 * @param visibility  visibilidad
 * @param parameters  parametros, en orden
 * @param thrown      excepciones declaradas con {@code throws}
 * @param annotations anotaciones del constructor
 * @param javadoc     documentacion, o {@code null} si no tiene
 * @param location    posicion en el codigo fuente
 */
public record ConstructorInfo(Visibility visibility, List<ParameterInfo> parameters,
                              List<TypeRef> thrown, List<AnnotationInfo> annotations,
                              JavadocInfo javadoc, Location location) {

    /**
     * Normaliza las listas: {@code null} se convierte en una lista vacia inmutable.
     */
    public ConstructorInfo {
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        thrown = thrown == null ? List.of() : List.copyOf(thrown);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}

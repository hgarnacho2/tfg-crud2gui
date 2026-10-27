package com.crud2gui.model;

import java.util.List;

/**
 * Metodo de un tipo.
 *
 * @param name        nombre del metodo
 * @param returnType  tipo devuelto ({@code void} se representa como un {@link TypeRef} de nombre {@code void})
 * @param visibility  visibilidad
 * @param modifiers   modificadores (abstract, static, default...)
 * @param parameters  parametros, en orden
 * @param thrown      excepciones declaradas con {@code throws}
 * @param annotations anotaciones del metodo
 * @param hasBody     {@code false} en metodos abstractos o de interfaz sin implementacion
 * @param javadoc     documentacion, o {@code null} si no tiene
 * @param location    posicion en el codigo fuente
 * @param optional    {@code true} si el enunciado marca este metodo como opcional
 */
public record MethodInfo(String name, TypeRef returnType, Visibility visibility, List<Modifier> modifiers,
                         List<ParameterInfo> parameters, List<TypeRef> thrown,
                         List<AnnotationInfo> annotations, boolean hasBody,
                         JavadocInfo javadoc, Location location, boolean optional) {

    /** Mantiene compatibilidad con llamadas que no especifican opcionalidad. */
    public MethodInfo(String name, TypeRef returnType, Visibility visibility, List<Modifier> modifiers,
                      List<ParameterInfo> parameters, List<TypeRef> thrown,
                      List<AnnotationInfo> annotations, boolean hasBody,
                      JavadocInfo javadoc, Location location) {
        this(name, returnType, visibility, modifiers, parameters, thrown, annotations,
                hasBody, javadoc, location, false);
    }

    /**
     * Normaliza las listas: {@code null} se convierte en una lista vacia inmutable.
     */
    public MethodInfo {
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        thrown = thrown == null ? List.of() : List.copyOf(thrown);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}

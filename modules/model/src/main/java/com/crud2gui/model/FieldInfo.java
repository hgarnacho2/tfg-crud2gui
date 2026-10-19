package com.crud2gui.model;

import java.util.List;

/**
 * Atributo de un tipo.
 *
 * @param name              nombre del atributo
 * @param type              tipo del atributo
 * @param visibility        visibilidad
 * @param modifiers         modificadores (static, final...)
 * @param initializedInline {@code true} si se inicializa en su declaracion, por ejemplo con {@code new ArrayList<>()};
 *                          es una de las evidencias para distinguir composicion de agregacion
 * @param annotations       anotaciones del atributo
 * @param javadoc           documentacion, o {@code null} si no tiene
 * @param location          posicion en el codigo fuente
 */
public record FieldInfo(String name, TypeRef type, Visibility visibility, List<Modifier> modifiers,
                        boolean initializedInline, List<AnnotationInfo> annotations,
                        JavadocInfo javadoc, Location location) {

    /**
     * Normaliza las listas: {@code null} se convierte en una lista vacia inmutable.
     */
    public FieldInfo {
        modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}

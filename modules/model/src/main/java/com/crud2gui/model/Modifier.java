package com.crud2gui.model;

/**
 * Modificadores de Java distintos de la visibilidad, que se expresa con {@link Visibility}.
 */
public enum Modifier {
    /** Clase o metodo abstracto. */
    ABSTRACT,
    /** Miembro de clase (estatico). */
    STATIC,
    /** No puede reasignarse, heredarse o sobrescribirse. */
    FINAL,
    /** Metodo sincronizado. */
    SYNCHRONIZED,
    /** Metodo nativo. */
    NATIVE,
    /** Campo excluido de la serializacion. */
    TRANSIENT,
    /** Campo volatil. */
    VOLATILE,
    /** Metodo por defecto de una interfaz. */
    DEFAULT,
    /** Tipo sellado. */
    SEALED
}

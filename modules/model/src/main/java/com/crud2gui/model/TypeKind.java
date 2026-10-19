package com.crud2gui.model;

/**
 * Tipo de declaracion Java. Una clase abstracta se representa como {@link #CLASS}
 * con el modificador {@link Modifier#ABSTRACT}.
 */
public enum TypeKind {
    /** Clase (incluidas las abstractas). */
    CLASS,
    /** Interfaz. */
    INTERFACE,
    /** Enumeracion. */
    ENUM,
    /** Record. */
    RECORD,
    /** Declaracion de anotacion. */
    ANNOTATION
}

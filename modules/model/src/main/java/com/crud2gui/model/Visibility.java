package com.crud2gui.model;

/**
 * Visibilidad de un tipo o miembro. Su simbolo en UML es {@code +}, {@code #}, {@code ~} y {@code -}
 * respectivamente.
 */
public enum Visibility {
    /** Publico (UML: {@code +}). */
    PUBLIC,
    /** Protegido (UML: {@code #}). */
    PROTECTED,
    /** Visibilidad de paquete, sin modificador explicito (UML: {@code ~}). */
    PACKAGE,
    /** Privado (UML: {@code -}). */
    PRIVATE
}

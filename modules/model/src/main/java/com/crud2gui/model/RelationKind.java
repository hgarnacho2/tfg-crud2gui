package com.crud2gui.model;

/**
 * Tipos de relacion entre dos tipos del proyecto, segun la semantica UML estricta.
 */
public enum RelationKind {
    /** Herencia ({@code extends}): linea continua con triangulo vacio. */
    INHERITANCE,
    /** Realizacion ({@code implements}): linea discontinua con triangulo vacio. */
    REALIZATION,
    /** Composicion: la parte la crea el todo y no se recibe de fuera (rombo relleno). */
    COMPOSITION,
    /** Agregacion: la parte se recibe de fuera (rombo vacio). */
    AGGREGATION,
    /** Asociacion: atributo sin evidencia de relacion todo-parte (linea simple). */
    ASSOCIATION,
    /** Dependencia: uso en parametros, retornos o variables locales (discontinua con flecha). */
    DEPENDENCY
}

package com.crud2gui.model;

/**
 * Constante de una enumeracion.
 *
 * @param name    nombre de la constante
 * @param javadoc documentacion, o {@code null} si no tiene
 */
public record EnumConstantInfo(String name, JavadocInfo javadoc) { }

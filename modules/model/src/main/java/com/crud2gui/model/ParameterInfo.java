package com.crud2gui.model;

/**
 * Parametro de un metodo o constructor.
 *
 * @param name    nombre del parametro
 * @param type    tipo del parametro; en un varargs es el tipo de cada elemento
 * @param varargs {@code true} si es de aridad variable ({@code Tipo...})
 */
public record ParameterInfo(String name, TypeRef type, boolean varargs) { }

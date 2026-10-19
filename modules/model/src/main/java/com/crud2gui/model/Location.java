package com.crud2gui.model;

/**
 * Posicion de un elemento en el codigo fuente.
 *
 * @param file fichero, relativo a la carpeta raiz analizada y con separador {@code /}
 * @param line numero de linea (empieza en 1); 0 si se desconoce
 */
public record Location(String file, int line) { }

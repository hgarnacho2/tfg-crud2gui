package com.crud2gui.model;

/**
 * Hechos detectados en el codigo que sustentan una relacion. Se guardan junto a ella para poder
 * cambiar de criterio (UML estricto o criterio del curso) sin volver a analizar el codigo.
 *
 * @param createdWithNew        la clase crea la parte con {@code new}
 * @param assignedFromParameter la parte llega a la clase por un constructor o un setter
 * @param initializedInline     el atributo se inicializa en su declaracion
 * @param exposedBySetter       existe un setter publico para el atributo
 * @param usedIn                lugar de uso en una dependencia: {@code parameter}, {@code return} o {@code local};
 *                              {@code null} en el resto de relaciones
 */
public record Evidence(boolean createdWithNew, boolean assignedFromParameter,
                       boolean initializedInline, boolean exposedBySetter, String usedIn) { }

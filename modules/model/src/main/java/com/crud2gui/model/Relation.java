package com.crud2gui.model;

/**
 * Arista del diagrama: relacion entre dos tipos del proyecto.
 *
 * @param kind         tipo de relacion
 * @param from         id del tipo origen (el todo o el hijo)
 * @param to           id del tipo destino
 * @param via          atributo que origina la relacion; {@code null} en herencia y realizacion
 * @param multiplicity multiplicidad en el extremo destino ({@code 0..1}, {@code 1}, {@code 0..*}); {@code null} si no aplica
 * @param confidence   fiabilidad con la que se ha deducido el tipo de relacion
 * @param evidence     hechos que sustentan la clasificacion
 */
public record Relation(RelationKind kind, String from, String to, String via,
                       String multiplicity, Confidence confidence, Evidence evidence) { }

package com.crud2gui.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultados calculados a partir del modelo, separados de los datos extraidos del codigo.
 *
 * @param managerClass   clase gestora elegida; siempre presente si hay tipos del dominio
 * @param alternatives   otras candidatas ordenadas por puntuacion descendente
 * @param entryPoints    ids de los tipos con metodo {@code main} (lanzadores)
 * @param manualOverride {@code true} si la gestora la fijo el usuario y no la heuristica
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Analysis(@JsonProperty("managerClass") ManagerCandidate managerClass,
                       @JsonProperty("alternatives") List<ManagerCandidate> alternatives,
                       @JsonProperty("entryPoints") List<String> entryPoints,
                       @JsonProperty("manualOverride") boolean manualOverride) {

    /**
     * Normaliza las listas: {@code null} se convierte en una lista vacia inmutable.
     */
    public Analysis {
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        entryPoints = entryPoints == null ? List.of() : List.copyOf(entryPoints);
    }
}

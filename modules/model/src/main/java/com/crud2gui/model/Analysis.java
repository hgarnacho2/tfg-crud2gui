package com.crud2gui.model;

import java.util.List;

/**
 * Resultados calculados a partir del modelo, separados de los datos extraidos del codigo.
 *
 * @param managerClass   clase gestora elegida; siempre presente si hay tipos del dominio
 * @param alternatives   otras candidatas ordenadas por puntuacion descendente
 * @param subManagers    ids de gestoras de segundo nivel, alcanzadas desde la principal
 * @param entryPoints    ids de los tipos con metodo {@code main} (lanzadores)
 * @param manualOverride {@code true} si la gestora la fijo el usuario y no la heuristica
 */
public record Analysis(ManagerCandidate managerClass, List<ManagerCandidate> alternatives,
                       List<String> subManagers, List<String> entryPoints, boolean manualOverride) {

    /**
     * Normaliza las listas: {@code null} se convierte en una lista vacia inmutable.
     */
    public Analysis {
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        subManagers = subManagers == null ? List.of() : List.copyOf(subManagers);
        entryPoints = entryPoints == null ? List.of() : List.copyOf(entryPoints);
    }
}

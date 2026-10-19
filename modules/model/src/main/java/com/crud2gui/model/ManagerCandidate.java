package com.crud2gui.model;

import java.util.List;

/**
 * Candidata a clase gestora, con la puntuacion y las razones que la respaldan.
 *
 * @param id         id del tipo candidato
 * @param score      puntuacion total (mayor es mejor)
 * @param confidence fiabilidad de la eleccion, segun la diferencia con las demas candidatas
 * @param reasons    explicacion legible de las senales que han sumado puntos
 */
public record ManagerCandidate(String id, double score, Confidence confidence, List<String> reasons) {

    /**
     * Normaliza las razones: {@code null} se convierte en una lista vacia inmutable.
     */
    public ManagerCandidate {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}

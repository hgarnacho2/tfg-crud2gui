package com.crud2gui.model;

import java.util.List;

/**
 * Raiz del metamodelo de un proyecto: tipos (nodos), relaciones (aristas), analisis y disposicion visual.
 * Es la unidad que se serializa a JSON con {@link ModelIO}.
 *
 * @param schemaVersion version del formato; la version mayor debe coincidir al leer
 * @param source        origen y datos de la generacion
 * @param types         tipos declarados en el proyecto
 * @param relations     relaciones entre los tipos calculadas por el parser
 * @param analysis      resultados calculados, incluida la candidata a clase gestora
 * @param layout        disposicion visual; {@code null} si no se definio
 */
public record ProjectModel(String schemaVersion, SourceInfo source, List<TypeInfo> types,
                           List<Relation> relations, Analysis analysis, Layout layout) {

    /** Version actual del esquema JSON. */
    public static final String CURRENT_SCHEMA = "1.0";

    /**
     * Normaliza las listas: {@code null} se convierte en una lista vacia inmutable.
     */
    public ProjectModel {
        types = types == null ? List.of() : List.copyOf(types);
        relations = relations == null ? List.of() : List.copyOf(relations);
    }
}

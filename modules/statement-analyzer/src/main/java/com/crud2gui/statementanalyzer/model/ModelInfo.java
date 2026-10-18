package com.crud2gui.statementanalyzer.model;

import java.util.Objects;

/**
 * Modelo ofrecido por un proveedor.
 *
 * @param id          identificador que se pasa a {@link ModelClient#generate}, sin prefijos de ruta
 * @param displayName nombre legible; si no se indica se usa el identificador
 */
public record ModelInfo(String id, String displayName) {

    /** Valida el identificador y rellena el nombre legible. */
    public ModelInfo {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("El identificador del modelo no puede estar vacio");
        }
        displayName = displayName == null || displayName.isBlank() ? id : displayName;
    }
}

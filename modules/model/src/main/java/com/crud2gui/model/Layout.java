package com.crud2gui.model;

import java.util.Map;

/**
 * Disposicion visual del diagrama. Se guarda aparte del modelo para que regenerarlo
 * no destruya las posiciones que el usuario haya elegido.
 *
 * @param positions posicion de cada caja, por id de tipo
 */
public record Layout(Map<String, Point> positions) {

    /**
     * Normaliza las posiciones: {@code null} se convierte en un mapa vacio inmutable.
     */
    public Layout {
        positions = positions == null ? Map.of() : Map.copyOf(positions);
    }
}

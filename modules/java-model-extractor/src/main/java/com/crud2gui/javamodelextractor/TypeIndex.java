package com.crud2gui.javamodelextractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registro de todos los tipos declarados en el proyecto, construido en la primera pasada del
 * analisis. Permite saber si un nombre completo pertenece al proyecto y buscar por nombre simple.
 */
final class TypeIndex {
    private final Set<String> ids = new HashSet<>();
    private final Map<String, List<String>> bySimpleName = new HashMap<>();

    /**
     * Registra un tipo. Registrar dos veces el mismo id no tiene efecto.
     *
     * @param id nombre completo del tipo, incluido el contenedor si es una clase interna
     */
    void add(String id) {
        if (ids.add(id)) {
            String simple = id.substring(id.lastIndexOf('.') + 1);
            bySimpleName.computeIfAbsent(simple, k -> new ArrayList<>()).add(id);
        }
    }

    /**
     * Indica si un nombre completo pertenece al proyecto.
     *
     * @param id nombre completo a comprobar
     * @return {@code true} si el tipo esta declarado en el proyecto
     */
    boolean contains(String id) {
        return ids.contains(id);
    }

    /**
     * Busca los tipos del proyecto que tienen un nombre simple dado.
     *
     * @param simple nombre simple, sin paquete
     * @return ids de los tipos con ese nombre; lista vacia si no hay ninguno
     */
    List<String> withSimpleName(String simple) {
        return bySimpleName.getOrDefault(simple, List.of());
    }
}

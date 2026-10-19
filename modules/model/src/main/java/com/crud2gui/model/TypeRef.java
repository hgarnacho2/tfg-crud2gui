package com.crud2gui.model;

import java.util.List;

/**
 * Referencia a un tipo, con genericos y arrays resueltos.
 *
 * @param name            nombre simple tal como se escribio, por ejemplo {@code List}, {@code Usuario} o {@code int}
 * @param qualifiedName   nombre completo si el tipo esta declarado en el proyecto; {@code null} si es
 *                        externo (JDK o librerias) o primitivo
 * @param projectType     {@code true} si el tipo pertenece al proyecto analizado; solo estos tipos generan aristas
 * @param arguments       argumentos genericos; para {@code List<Usuario>} contiene la referencia a {@code Usuario}
 * @param arrayDimensions numero de dimensiones de array; {@code String[]} tiene 1
 */
public record TypeRef(String name, String qualifiedName, boolean projectType,
                      List<TypeRef> arguments, int arrayDimensions) {

    /**
     * Normaliza los argumentos genericos: {@code null} se convierte en una lista vacia inmutable.
     */
    public TypeRef {
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }

    /**
     * Devuelve el texto para mostrar el tipo en un diagrama.
     *
     * @return representacion legible, por ejemplo {@code List<Usuario>}, {@code Map<String, Producto>} o {@code int[]}
     */
    public String display() {
        StringBuilder sb = new StringBuilder(name);
        if (!arguments.isEmpty()) {
            sb.append('<');
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(arguments.get(i).display());
            }
            sb.append('>');
        }
        for (int i = 0; i < arrayDimensions; i++) sb.append("[]");
        return sb.toString();
    }
}

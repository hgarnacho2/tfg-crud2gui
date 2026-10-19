package com.tienda;

public interface Imprimible {
    void imprimir();

    default String etiqueta() {
        return "item";
    }
}

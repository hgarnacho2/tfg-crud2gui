package com.tienda;

import java.util.ArrayList;
import java.util.List;

/** Clase gestora de la tienda. */
public class Tienda {
    private List<Usuario> usuarios = new ArrayList<>();

    public void addUser(Usuario u) {
        usuarios.add(u);
    }

    public static class Linea {
        private Producto producto;
    }

    public record Resumen(String titulo, double precio) { }
}

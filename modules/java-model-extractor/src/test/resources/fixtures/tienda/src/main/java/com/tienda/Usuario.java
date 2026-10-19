package com.tienda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Usuario {
    private String nombre;
    private List<Producto> productos = new ArrayList<>();
    private Map<String, Producto> indice = new HashMap<>();
    private Producto[] destacados;

    public Usuario(String nombre) { this.nombre = nombre; }

    public String getNombre() { return nombre; }

    public void addProducto(Producto p) {
        productos.add(p);
    }
}

package com.tienda;

/**
 * Producto generico de la tienda.
 *
 * @author equipo
 */
public abstract class Producto {
    private int id;
    private String titulo;
    private double precio;
    private T_Estado estado;

    /**
     * Crea un producto.
     *
     * @param id identificador
     * @param titulo titulo del producto
     */
    public Producto(int id, String titulo) {
        this.id = id;
        this.titulo = titulo;
        this.estado = T_Estado.NUEVO;
    }

    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public T_Estado getEstado() { return estado; }

    /** Imprime las caracteristicas propias de cada tipo. */
    protected abstract void printCaracteristicas();
}

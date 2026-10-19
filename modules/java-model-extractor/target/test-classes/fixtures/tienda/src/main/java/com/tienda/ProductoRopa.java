package com.tienda;

public class ProductoRopa extends Producto implements Imprimible {
    private String talla;

    public ProductoRopa(int id, String titulo, String talla) {
        super(id, titulo);
        this.talla = talla;
    }

    public String getTalla() { return talla; }

    @Override
    protected void printCaracteristicas() {
        System.out.println("Talla: " + talla);
    }

    @Override
    public void imprimir() {
        printCaracteristicas();
    }
}

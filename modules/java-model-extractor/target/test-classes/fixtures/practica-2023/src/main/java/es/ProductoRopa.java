/** Producto de la categoría ropa, caracterizado por su talla. */
public class ProductoRopa extends Producto {
    private String talla;

    public ProductoRopa(int id, String titulo, double precio, String descripcion,
                        T_Estado estado, String talla) {
        super(id, titulo, precio, descripcion, estado);
        this.talla = talla;
    }

    public String getTalla() { return talla; }
    public void setTalla(String talla) { this.talla = talla; }

    @Override
    protected void printCaracteristicas() {
        System.out.println("Talla: " + talla);
    }
}

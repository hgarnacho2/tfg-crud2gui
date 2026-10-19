import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Usuario identificado por nombre y correo, con sus productos ofertados. */
public class Usuario {
    private String nombre;
    private String email;
    private List<Producto> productos;

    public Usuario(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
        this.productos = new ArrayList<>();
        System.out.println("Creado usuario " + email);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<Producto> getProductos() { return Collections.unmodifiableList(productos); }
    public void setProductos(List<Producto> productos) {
        this.productos = new ArrayList<>(productos);
    }

    public void addProducto(Producto producto) {
        if (producto != null && buscarProducto(producto.getId()) == null) {
            producto.setEstado(T_Estado.DISPONIBLE);
            productos.add(producto);
        }
    }

    public boolean removeProducto(int id) {
        Producto producto = buscarProducto(id);
        return producto != null && productos.remove(producto);
    }

    public Producto buscarProducto(int id) {
        for (Producto p : productos) if (p.getId() == id) return p;
        return null;
    }

    public void print() {
        System.out.println("------------");
        System.out.println("Nombre: " + nombre);
        System.out.println("Email: " + email);
        System.out.println("------------");
        for (Producto p : productos) p.print();
    }
}

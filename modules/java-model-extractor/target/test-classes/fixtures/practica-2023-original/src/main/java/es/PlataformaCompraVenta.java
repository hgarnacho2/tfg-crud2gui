import java.util.ArrayList;
import java.util.List;

/** Gestiona los usuarios y los productos publicados en la plataforma. */
public class PlataformaCompraVenta {
    private String nombre;
    private List<Usuario> usuarios;

    public PlataformaCompraVenta(String nombre) {
        this.nombre = nombre;
        this.usuarios = new ArrayList<>();
        System.out.println("Creada plataforma de compraventa " + nombre);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public List<Usuario> getUsuarios() { return new ArrayList<>(usuarios); }

    public void addUser(Usuario usuario) {
        if (usuario != null && buscarUsuario(usuario.getEmail()) == null) {
            usuarios.add(usuario);
            System.out.println("El usuario con email " + usuario.getEmail() + " ha sido añadido");
        }
    }

    public void removeUser(String email) {
        Usuario usuario = buscarUsuario(email);
        if (usuario != null) {
            usuarios.remove(usuario);
            System.out.println("El usuario con email " + email + " ha sido eliminado");
        } else System.out.println("No existe usuario con email " + email);
    }

    private Usuario buscarUsuario(String email) {
        for (Usuario u : usuarios)
            if (u.getEmail().equalsIgnoreCase(email)) return u;
        return null;
    }

    public void addProduct2User(Producto producto, String email) {
        Usuario u = buscarUsuario(email);
        if (u == null) { System.out.println("No existe usuario con email " + email); return; }
        u.addProducto(producto);
        System.out.println("El producto con id " + producto.getId() +
                " se ha añadido al usuario " + email);
    }

    public void addProduct2User(Producto producto, Usuario usuario) {
        if (usuario != null) addProduct2User(producto, usuario.getEmail());
    }

    public void removeProduct2User(int id, String email) {
        Usuario u = buscarUsuario(email);
        if (u != null && u.removeProducto(id))
            System.out.println("El producto con id " + id +
                    " se ha eliminado de la lista del usuario " + email);
        else System.out.println("No se ha encontrado el producto " + id + " para " + email);
    }

    public void printAllUsers() {
        for (Usuario u : usuarios) u.print();
    }

    public void printAllProducts() {
        for (Usuario u : usuarios)
            for (Producto p : u.getProductos()) p.print();
    }

    public void printProductsByState(T_Estado estado) {
        for (Usuario u : usuarios)
            for (Producto p : u.getProductos())
                if (p.getEstado() == estado) p.print();
    }

    public void printProductsByUser(String email) {
        Usuario u = buscarUsuario(email);
        if (u == null) { System.out.println("No existe usuario con email " + email); return; }
        for (Producto p : u.getProductos()) p.print();
    }

    public void printProductsByUser(String email, T_Estado estado) {
        Usuario u = buscarUsuario(email);
        if (u == null) { System.out.println("No existe usuario con email " + email); return; }
        for (Producto p : u.getProductos())
            if (p.getEstado() == estado) p.print();
    }

    public void searchProductsByKeyword(String keyword) {
        if (keyword == null) return;
        String needle = keyword.toLowerCase();
        for (Usuario u : usuarios)
            for (Producto p : u.getProductos())
                if (p.getDescripcion().toLowerCase().contains(needle)) p.print();
    }
}

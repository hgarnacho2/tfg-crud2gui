/**
 * Clase base abstracta de los productos ofertados en la plataforma.
 */
public abstract class Producto {
    private int id;
    private String titulo;
    private double precio;
    private String descripcion;
    private T_Estado estado;

    public Producto(int id, String titulo, double precio, String descripcion, T_Estado estado) {
        this.id = id;
        this.titulo = titulo;
        this.precio = precio;
        this.descripcion = descripcion;
        this.estado = estado;
        System.out.println("Creado producto con id " + id);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public T_Estado getEstado() { return estado; }

    public void setEstado(T_Estado estado) {
        this.estado = estado;
        System.out.println("El estado del producto con id " + id + " ha sido modificado a estado " + estado);
    }

    public void print() {
        System.out.println("ID: " + id);
        System.out.println("Titulo: " + titulo);
        System.out.println("Precio: " + precio);
        System.out.println("Descripcion: " + descripcion);
        System.out.println("Estado: " + estado);
        printCaracteristicas();
        System.out.println();
    }

    /** Cada subclase imprime sus atributos específicos. */
    protected abstract void printCaracteristicas();
}

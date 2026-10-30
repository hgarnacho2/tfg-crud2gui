public class Evento {
    private int id;
    private String titulo;
    private double precioEntrada;
    private String descripcion;
    private T_Estado estado;

    public Evento(int id, String titulo, double precioEntrada, String descripcion, T_Estado estado) {
        this.id = id;
        this.titulo = titulo;
        this.precioEntrada = precioEntrada;
        this.descripcion = descripcion;
        this.estado = estado;
        System.out.println("Creado evento con id " + id);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public double getPrecioEntrada() { return precioEntrada; }
    public void setPrecioEntrada(double precioEntrada) { this.precioEntrada = precioEntrada; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public T_Estado getEstado() { return estado; }

    public void setEstado(T_Estado estado) {
        this.estado = estado;
        System.out.println("El estado del evento con id " + id + " ha sido modificado a estado " + estado);
    }

    public void print() {
        System.out.println("ID: " + id);
        System.out.println("Titulo: " + titulo);
        System.out.println("Precio de entrada: " + precioEntrada);
        System.out.println("Descripcion: " + descripcion);
        System.out.println("Estado: " + estado);
    }
}

public abstract class Alojamiento {
    private int id;
    private String titulo;
    private double precioNoche;
    private String descripcion;
    private T_Estado estado;

    public Alojamiento(int id, String titulo, double precioNoche, String descripcion, T_Estado estado) {
        this.id = id;
        this.titulo = titulo;
        this.precioNoche = precioNoche;
        this.descripcion = descripcion;
        this.estado = estado;
        System.out.println("Creado alojamiento con id " + id);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public double getPrecioNoche() { return precioNoche; }
    public void setPrecioNoche(double precioNoche) { this.precioNoche = precioNoche; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public T_Estado getEstado() { return estado; }

    public void setEstado(T_Estado estado) {
        this.estado = estado;
        System.out.println("El estado del alojamiento con id " + id + " ha sido modificado a estado " + estado);
    }

    public void print() {
        System.out.println("ID: " + id);
        System.out.println("Titulo: " + titulo);
        System.out.println("Precio por noche: " + precioNoche);
        System.out.println("Descripcion: " + descripcion);
        System.out.println("Estado: " + estado);
    }
}

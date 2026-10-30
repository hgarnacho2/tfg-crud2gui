public abstract class Curso {
    private int id;
    private String titulo;
    private double precioMatricula;
    private String descripcion;
    private T_Estado estado;

    public Curso(int id, String titulo, double precioMatricula, String descripcion, T_Estado estado) {
        this.id = id;
        this.titulo = titulo;
        this.precioMatricula = precioMatricula;
        this.descripcion = descripcion;
        this.estado = estado;
        System.out.println("Creado curso con id " + id);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public double getPrecioMatricula() { return precioMatricula; }
    public void setPrecioMatricula(double precioMatricula) { this.precioMatricula = precioMatricula; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public T_Estado getEstado() { return estado; }

    public void setEstado(T_Estado estado) {
        this.estado = estado;
        System.out.println("El estado del curso con id " + id + " ha sido modificado a estado " + estado);
    }

    public void print() {
        System.out.println("ID: " + id);
        System.out.println("Titulo: " + titulo);
        System.out.println("Precio de matricula: " + precioMatricula);
        System.out.println("Descripcion: " + descripcion);
        System.out.println("Estado: " + estado);
    }
}

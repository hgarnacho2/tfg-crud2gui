/**
 * Pueblo atendido por la empresa de reparto de leña.
 */
public class Pueblo {
    private String nombre;
    private double precioTransporte;

    /** Crea un pueblo con el coste de transporte por defecto (10 euros). */
    public Pueblo(String nombre) {
        this.nombre = nombre;
        this.precioTransporte = 10;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPrecioTransporte() { return precioTransporte; }
    public void setPrecioTransporte(double precioTransporte) { this.precioTransporte = precioTransporte; }

    /** Imprime la informacion del pueblo. */
    public void imprimeInformacion() {
        System.out.println("Población: " + nombre);
        System.out.println("Precio del transporte: " + precioTransporte + " euros");
    }
}

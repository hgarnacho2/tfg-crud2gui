/**
 * Cliente de la empresa; reside en un Pueblo.
 */
public class Cliente {
    private String nombre;
    private String direccion;
    private Pueblo pueblo;

    public Cliente(String nombre, Pueblo pueblo) {
        this.nombre = nombre;
        this.pueblo = pueblo;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public Pueblo getPueblo() { return pueblo; }
    public void setPueblo(Pueblo pueblo) { this.pueblo = pueblo; }

    /** Imprime nombre, direccion y la informacion del pueblo. */
    public void imprimeInformacion() {
        System.out.println("Cliente: " + nombre);
        System.out.println("Dirección: " + direccion);
        pueblo.imprimeInformacion();
    }
}

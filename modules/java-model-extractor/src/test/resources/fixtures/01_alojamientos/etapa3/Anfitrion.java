import java.util.ArrayList;

public class Anfitrion {
    private String nombre;
    private String email;
    private ArrayList<Alojamiento> alojamientos;

    public Anfitrion(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
        this.alojamientos = new ArrayList<Alojamiento>();
        System.out.println("Creado anfitrion " + email);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public ArrayList<Alojamiento> getAlojamientos() { return alojamientos; }

    /** Añade el alojamiento a la lista y lo deja en estado DISPONIBLE. */
    public void addAlojamiento(Alojamiento x) {
        x.setEstado(T_Estado.DISPONIBLE);
        alojamientos.add(x);
    }

    /** Elimina de la lista el alojamiento con el id indicado. Devuelve true si existía. */
    public boolean removeAlojamiento(int id) {
        for (int k = 0; k < alojamientos.size(); k++) {
            if (alojamientos.get(k).getId() == id) {
                alojamientos.remove(k);
                return true;
            }
        }
        return false;
    }

    public void print() {
        System.out.println("------------");
        System.out.println("Nombre: " + nombre);
        System.out.println("Email: " + email);
        System.out.println("------------");
        for (Alojamiento x : alojamientos) {
            x.print();
        }
    }
}

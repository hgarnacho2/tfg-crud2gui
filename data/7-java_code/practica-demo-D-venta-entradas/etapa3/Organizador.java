import java.util.ArrayList;

public class Organizador {
    private String nombre;
    private String email;
    private ArrayList<Evento> eventos;

    public Organizador(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
        this.eventos = new ArrayList<Evento>();
        System.out.println("Creado organizador " + email);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public ArrayList<Evento> getEventos() { return eventos; }

    /** Añade el evento a la lista y lo deja en estado DISPONIBLE. */
    public void addEvento(Evento x) {
        x.setEstado(T_Estado.DISPONIBLE);
        eventos.add(x);
    }

    /** Elimina de la lista el evento con el id indicado. Devuelve true si existía. */
    public boolean removeEvento(int id) {
        for (int k = 0; k < eventos.size(); k++) {
            if (eventos.get(k).getId() == id) {
                eventos.remove(k);
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
        for (Evento x : eventos) {
            x.print();
        }
    }
}

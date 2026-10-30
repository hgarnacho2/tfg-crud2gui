import java.util.ArrayList;

public class PlataformaAlojamientos {
    private String nombre;
    private ArrayList<Anfitrion> anfitriones;

    public PlataformaAlojamientos(String nombre) {
        this.nombre = nombre;
        this.anfitriones = new ArrayList<Anfitrion>();
        System.out.println("Creada plataforma de alojamientos " + nombre);
    }

    private Anfitrion buscarAnfitrion(String email) {
        for (Anfitrion x : anfitriones) {
            if (x.getEmail().equals(email)) { return x; }
        }
        return null;
    }

    public void addAnfitrion(Anfitrion x) {
        anfitriones.add(x);
        System.out.println("El anfitrion con email " + x.getEmail() + " ha sido añadido");
    }

    public void removeAnfitrion(String email) {
        Anfitrion x = buscarAnfitrion(email);
        if (x != null) {
            anfitriones.remove(x);
            System.out.println("El anfitrion con email " + email + " ha sido eliminado");
        }
    }

    public void addAlojamiento2Anfitrion(Alojamiento a, String email) {
        Anfitrion x = buscarAnfitrion(email);
        if (x != null) {
            x.addAlojamiento(a);
            System.out.println("El alojamiento con id " + a.getId() + " se ha añadido al anfitrion " + email);
        }
    }

    public void removeAlojamiento2Anfitrion(int id, String email) {
        Anfitrion x = buscarAnfitrion(email);
        if (x != null && x.removeAlojamiento(id)) {
            System.out.println("El alojamiento con id " + id + " se ha eliminado de la lista del anfitrion " + email);
        }
    }

    public void printAllAnfitriones() {
        for (Anfitrion x : anfitriones) { x.print(); }
    }

    public void printAllAlojamientos() {
        for (Anfitrion x : anfitriones) {
            for (Alojamiento a : x.getAlojamientos()) { a.print(); }
        }
    }

    public void printAlojamientosByAnfitrion(String email) {
        Anfitrion x = buscarAnfitrion(email);
        if (x != null) {
            for (Alojamiento a : x.getAlojamientos()) { a.print(); }
        }
    }

    // ---- Parte opcional ----
    public void printAlojamientosByState(T_Estado estado) {
        for (Anfitrion x : anfitriones) {
            for (Alojamiento a : x.getAlojamientos()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void printAlojamientosByAnfitrion(String email, T_Estado estado) {
        Anfitrion x = buscarAnfitrion(email);
        if (x != null) {
            for (Alojamiento a : x.getAlojamientos()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void searchAlojamientosByKeyword(String palabra) {
        for (Anfitrion x : anfitriones) {
            for (Alojamiento a : x.getAlojamientos()) {
                if (a.getDescripcion().toLowerCase().contains(palabra.toLowerCase())) { a.print(); }
            }
        }
    }
}

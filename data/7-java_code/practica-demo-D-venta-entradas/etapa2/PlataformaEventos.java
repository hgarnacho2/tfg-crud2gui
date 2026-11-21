import java.util.ArrayList;

public class PlataformaEventos {
    private String nombre;
    private ArrayList<Organizador> organizadores;

    public PlataformaEventos(String nombre) {
        this.nombre = nombre;
        this.organizadores = new ArrayList<Organizador>();
        System.out.println("Creada plataforma de eventos " + nombre);
    }

    private Organizador buscarOrganizador(String email) {
        for (Organizador x : organizadores) {
            if (x.getEmail().equals(email)) { return x; }
        }
        return null;
    }

    public void addOrganizador(Organizador x) {
        organizadores.add(x);
        System.out.println("El organizador con email " + x.getEmail() + " ha sido añadido");
    }

    public void removeOrganizador(String email) {
        Organizador x = buscarOrganizador(email);
        if (x != null) {
            organizadores.remove(x);
            System.out.println("El organizador con email " + email + " ha sido eliminado");
        }
    }

    public void addEvento2Organizador(Evento a, String email) {
        Organizador x = buscarOrganizador(email);
        if (x != null) {
            x.addEvento(a);
            System.out.println("El evento con id " + a.getId() + " se ha añadido al organizador " + email);
        }
    }

    public void removeEvento2Organizador(int id, String email) {
        Organizador x = buscarOrganizador(email);
        if (x != null && x.removeEvento(id)) {
            System.out.println("El evento con id " + id + " se ha eliminado de la lista del organizador " + email);
        }
    }

    public void printAllOrganizadores() {
        for (Organizador x : organizadores) { x.print(); }
    }

    public void printAllEventos() {
        for (Organizador x : organizadores) {
            for (Evento a : x.getEventos()) { a.print(); }
        }
    }

    public void printEventosByOrganizador(String email) {
        Organizador x = buscarOrganizador(email);
        if (x != null) {
            for (Evento a : x.getEventos()) { a.print(); }
        }
    }

    // ---- Parte opcional ----
    public void printEventosByState(T_Estado estado) {
        for (Organizador x : organizadores) {
            for (Evento a : x.getEventos()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void printEventosByOrganizador(String email, T_Estado estado) {
        Organizador x = buscarOrganizador(email);
        if (x != null) {
            for (Evento a : x.getEventos()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void searchEventosByKeyword(String palabra) {
        for (Organizador x : organizadores) {
            for (Evento a : x.getEventos()) {
                if (a.getDescripcion().toLowerCase().contains(palabra.toLowerCase())) { a.print(); }
            }
        }
    }
}

import java.util.ArrayList;

public class PlataformaAdopciones {
    private String nombre;
    private ArrayList<Refugio> refugios;

    public PlataformaAdopciones(String nombre) {
        this.nombre = nombre;
        this.refugios = new ArrayList<Refugio>();
        System.out.println("Creada plataforma de adopciones " + nombre);
    }

    private Refugio buscarRefugio(String email) {
        for (Refugio x : refugios) {
            if (x.getEmail().equals(email)) { return x; }
        }
        return null;
    }

    public void addRefugio(Refugio x) {
        refugios.add(x);
        System.out.println("El refugio con email " + x.getEmail() + " ha sido añadido");
    }

    public void removeRefugio(String email) {
        Refugio x = buscarRefugio(email);
        if (x != null) {
            refugios.remove(x);
            System.out.println("El refugio con email " + email + " ha sido eliminado");
        }
    }

    public void addAnimal2Refugio(Animal a, String email) {
        Refugio x = buscarRefugio(email);
        if (x != null) {
            x.addAnimal(a);
            System.out.println("El animal con id " + a.getId() + " se ha añadido al refugio " + email);
        }
    }

    public void removeAnimal2Refugio(int id, String email) {
        Refugio x = buscarRefugio(email);
        if (x != null && x.removeAnimal(id)) {
            System.out.println("El animal con id " + id + " se ha eliminado de la lista del refugio " + email);
        }
    }

    public void printAllRefugios() {
        for (Refugio x : refugios) { x.print(); }
    }

    public void printAllAnimales() {
        for (Refugio x : refugios) {
            for (Animal a : x.getAnimales()) { a.print(); }
        }
    }

    public void printAnimalesByRefugio(String email) {
        Refugio x = buscarRefugio(email);
        if (x != null) {
            for (Animal a : x.getAnimales()) { a.print(); }
        }
    }

    // ---- Parte opcional ----
    public void printAnimalesByState(T_Estado estado) {
        for (Refugio x : refugios) {
            for (Animal a : x.getAnimales()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void printAnimalesByRefugio(String email, T_Estado estado) {
        Refugio x = buscarRefugio(email);
        if (x != null) {
            for (Animal a : x.getAnimales()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void searchAnimalesByKeyword(String palabra) {
        for (Refugio x : refugios) {
            for (Animal a : x.getAnimales()) {
                if (a.getDescripcion().toLowerCase().contains(palabra.toLowerCase())) { a.print(); }
            }
        }
    }
}

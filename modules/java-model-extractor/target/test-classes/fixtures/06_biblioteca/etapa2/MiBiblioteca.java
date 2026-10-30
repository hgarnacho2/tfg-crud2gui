import java.util.ArrayList;

public class MiBiblioteca {
    private ArrayList<Lector> lectores = new ArrayList<Lector>();

    public void anadirLector(Lector x) {
        int n = lectores.size() + 1;
        x.setIdLector("lector" + n);
        x.setURL("https://www.miBiblioteca.com/lector" + n);
        lectores.add(x);
    }

    private Lector buscarLector(String id) {
        for (Lector x : lectores) {
            if (x.getIdLector().equals(id)) { return x; }
        }
        return null;
    }

    public boolean comprobarPassword(String id, String password) {
        Lector x = buscarLector(id);
        if (x == null) { System.out.println("No existe el lector " + id); return false; }
        if (!x.getPassword().equals(password)) { System.out.println("La password es incorrecta"); return false; }
        System.out.println("Password correcta");
        return true;
    }

    public void visualizarBiblioteca() {
        System.out.println("Lectores en la biblioteca:");
        for (Lector x : lectores) { System.out.println("     " + x.getIdLector() + ": " + x.getNombre()); }
    }

    public void visualizarLector(String id) {
        Lector x = buscarLector(id);
        if (x == null) { System.out.println("No existe el lector " + id); } else { x.imprimeInformacion(); }
    }

    public void anadirResena(String id, String texto) {
        Lector x = buscarLector(id);
        if (x == null) { System.out.println("No existe el identificador de lector " + id); } else { x.anadirResena(texto); }
    }
}

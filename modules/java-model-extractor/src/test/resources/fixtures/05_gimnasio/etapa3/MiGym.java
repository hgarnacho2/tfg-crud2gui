import java.util.ArrayList;

public class MiGym {
    private ArrayList<Socio> socios = new ArrayList<Socio>();

    public void anadirSocio(Socio x) {
        int n = socios.size() + 1;
        x.setIdSocio("socio" + n);
        x.setURL("https://www.miGym.com/socio" + n);
        socios.add(x);
    }

    private Socio buscarSocio(String id) {
        for (Socio x : socios) {
            if (x.getIdSocio().equals(id)) { return x; }
        }
        return null;
    }

    public boolean comprobarPassword(String id, String password) {
        Socio x = buscarSocio(id);
        if (x == null) { System.out.println("No existe el socio " + id); return false; }
        if (!x.getPassword().equals(password)) { System.out.println("La password es incorrecta"); return false; }
        System.out.println("Password correcta");
        return true;
    }

    public void visualizarGimnasio() {
        System.out.println("Socios en el gimnasio:");
        for (Socio x : socios) { System.out.println("     " + x.getIdSocio() + ": " + x.getNombre()); }
    }

    public void visualizarSocio(String id) {
        Socio x = buscarSocio(id);
        if (x == null) { System.out.println("No existe el socio " + id); } else { x.imprimeInformacion(); }
    }

    public void anadirSesion(String id, String texto) {
        Socio x = buscarSocio(id);
        if (x == null) { System.out.println("No existe el identificador de socio " + id); } else { x.anadirSesion(texto); }
    }
}

import java.util.ArrayList;

public class Socio {
    private String nombre;
    private String idSocio;
    private String password;
    private String URL;
    private FichaSocio ficha;
    private ArrayList<Sesion> sesiones;

    public Socio() { }
    public Socio(String nombre, String password) { this.nombre = nombre; this.password = password; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdSocio() { return idSocio; }
    public void setIdSocio(String idSocio) { this.idSocio = idSocio; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getURL() { return URL; }
    public void setURL(String URL) { this.URL = URL; }
    public FichaSocio getFicha() { return ficha; }
    public void setFicha(FichaSocio ficha) { this.ficha = ficha; }

    public void anadirSesion(String texto) {
        if (sesiones == null) { sesiones = new ArrayList<Sesion>(); }
        Sesion nuevo = new Sesion(texto);
        nuevo.setURL("https://www.miGym.com/sesion/" + idSocio + "." + primerNumeroLibre());
        sesiones.add(nuevo);
    }

    private int primerNumeroLibre() {
        int n = 1;
        boolean libre = false;
        while (!libre) {
            libre = true;
            String url = "https://www.miGym.com/sesion/" + idSocio + "." + n;
            for (Sesion x : sesiones) {
                if (x.getURL().equals(url)) { libre = false; }
            }
            if (!libre) { n++; }
        }
        return n;
    }

    /** Elimina el sesión con la URL indicada. Devuelve true si existía. */
    public boolean eliminarSesion(String url) {
        if (sesiones == null) { return false; }
        for (int k = 0; k < sesiones.size(); k++) {
            if (sesiones.get(k).getURL().equals(url)) {
                sesiones.remove(k);
                return true;
            }
        }
        return false;
    }

    public void imprimeInformacion() {
        System.out.println("Identificador de socio: " + idSocio);
        System.out.println("     Nombre: " + nombre);
        System.out.println("     URL: " + URL);
        if (ficha != null) {
            System.out.println("     FICHA:");
            ficha.imprimeInformacion();
        }
        if (sesiones != null) {
            System.out.println("     SESIONES:");
            for (Sesion x : sesiones) { x.imprimeInformacion(); }
        }
    }
}

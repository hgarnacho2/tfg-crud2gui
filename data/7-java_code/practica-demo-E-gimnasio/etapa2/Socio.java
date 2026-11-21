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
        nuevo.setURL("https://www.miGym.com/sesion/" + idSocio + "." + (sesiones.size() + 1));
        sesiones.add(nuevo);
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

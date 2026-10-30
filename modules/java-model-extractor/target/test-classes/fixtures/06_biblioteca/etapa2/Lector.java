import java.util.ArrayList;

public class Lector {
    private String nombre;
    private String idLector;
    private String password;
    private String URL;
    private FichaLector ficha;
    private ArrayList<Resena> resenas;

    public Lector() { }
    public Lector(String nombre, String password) { this.nombre = nombre; this.password = password; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdLector() { return idLector; }
    public void setIdLector(String idLector) { this.idLector = idLector; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getURL() { return URL; }
    public void setURL(String URL) { this.URL = URL; }
    public FichaLector getFicha() { return ficha; }
    public void setFicha(FichaLector ficha) { this.ficha = ficha; }

    public void anadirResena(String texto) {
        if (resenas == null) { resenas = new ArrayList<Resena>(); }
        Resena nuevo = new Resena(texto);
        nuevo.setURL("https://www.miBiblioteca.com/resena/" + idLector + "." + (resenas.size() + 1));
        resenas.add(nuevo);
    }

    public void imprimeInformacion() {
        System.out.println("Identificador de lector: " + idLector);
        System.out.println("     Nombre: " + nombre);
        System.out.println("     URL: " + URL);
        if (ficha != null) {
            System.out.println("     FICHA:");
            ficha.imprimeInformacion();
        }
        if (resenas != null) {
            System.out.println("     RESEÑAS:");
            for (Resena x : resenas) { x.imprimeInformacion(); }
        }
    }
}

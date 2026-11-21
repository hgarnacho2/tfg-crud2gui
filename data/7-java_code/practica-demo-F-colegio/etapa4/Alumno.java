import java.util.ArrayList;

public class Alumno {
    private String nombre;
    private String idAlumno;
    private String password;
    private String URL;
    private FichaAlumno ficha;
    private ArrayList<Anotacion> anotaciones;

    public Alumno() { }
    public Alumno(String nombre, String password) { this.nombre = nombre; this.password = password; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdAlumno() { return idAlumno; }
    public void setIdAlumno(String idAlumno) { this.idAlumno = idAlumno; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getURL() { return URL; }
    public void setURL(String URL) { this.URL = URL; }
    public FichaAlumno getFicha() { return ficha; }
    public void setFicha(FichaAlumno ficha) { this.ficha = ficha; }

    public void anadirAnotacion(String texto) {
        if (anotaciones == null) { anotaciones = new ArrayList<Anotacion>(); }
        Anotacion nuevo = new Anotacion(texto);
        nuevo.setURL("https://www.miColegio.com/anotacion/" + idAlumno + "." + primerNumeroLibre());
        anotaciones.add(nuevo);
    }

    private int primerNumeroLibre() {
        int n = 1;
        boolean libre = false;
        while (!libre) {
            libre = true;
            String url = "https://www.miColegio.com/anotacion/" + idAlumno + "." + n;
            for (Anotacion x : anotaciones) {
                if (x.getURL().equals(url)) { libre = false; }
            }
            if (!libre) { n++; }
        }
        return n;
    }

    /** Elimina el anotación con la URL indicada. Devuelve true si existía. */
    public boolean eliminarAnotacion(String url) {
        if (anotaciones == null) { return false; }
        for (int k = 0; k < anotaciones.size(); k++) {
            if (anotaciones.get(k).getURL().equals(url)) {
                anotaciones.remove(k);
                return true;
            }
        }
        return false;
    }

    public void imprimeInformacion() {
        System.out.println("Identificador de alumno: " + idAlumno);
        System.out.println("     Nombre: " + nombre);
        System.out.println("     URL: " + URL);
        if (ficha != null) {
            System.out.println("     FICHA:");
            ficha.imprimeInformacion();
        }
        if (anotaciones != null) {
            System.out.println("     ANOTACIONES:");
            for (Anotacion x : anotaciones) { x.imprimeInformacion(); }
        }
    }
}

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
        nuevo.setURL("https://www.miColegio.com/anotacion/" + idAlumno + "." + (anotaciones.size() + 1));
        anotaciones.add(nuevo);
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

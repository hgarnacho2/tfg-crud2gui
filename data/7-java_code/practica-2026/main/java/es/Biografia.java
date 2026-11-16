import java.util.Date;

/**
 * Biografia de un usuario: fecha de nacimiento, aficiones, profesion y objetivos vitales.
 */
public class Biografia {
    private Date fechaDeNacimiento;
    private String aficiones;
    private String profesion;
    private String objetivosVitales;

    public Biografia() {
    }

    public Date getFechaDeNacimiento() { return fechaDeNacimiento; }
    public void setFechaDeNacimiento(Date fechaDeNacimiento) { this.fechaDeNacimiento = fechaDeNacimiento; }

    public String getAficiones() { return aficiones; }
    public void setAficiones(String aficiones) { this.aficiones = aficiones; }

    public String getProfesion() { return profesion; }
    public void setProfesion(String profesion) { this.profesion = profesion; }

    public String getObjetivosVitales() { return objetivosVitales; }
    public void setObjetivosVitales(String objetivosVitales) { this.objetivosVitales = objetivosVitales; }

    /** Imprime en pantalla la informacion de la biografia. */
    public void imprimeInformacion() {
        System.out.println("Fecha de nacimiento: " + fechaDeNacimiento);
        System.out.println("Aficiones: " + aficiones);
        System.out.println("Profesión: " + profesion);
        System.out.println("Objetivos vitales: " + objetivosVitales);
    }
}

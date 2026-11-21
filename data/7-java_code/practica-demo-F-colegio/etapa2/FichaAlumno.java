import java.util.Date;

public class FichaAlumno {
    private Date fechaDeNacimiento;
    private String aficiones;
    private String asignaturaFavorita;
    private String objetivosAcademicos;

    public Date getFechaDeNacimiento() { return fechaDeNacimiento; }
    public void setFechaDeNacimiento(Date fechaDeNacimiento) { this.fechaDeNacimiento = fechaDeNacimiento; }
    public String getAficiones() { return aficiones; }
    public void setAficiones(String aficiones) { this.aficiones = aficiones; }
    public String getAsignaturaFavorita() { return asignaturaFavorita; }
    public void setAsignaturaFavorita(String asignaturaFavorita) { this.asignaturaFavorita = asignaturaFavorita; }
    public String getObjetivosAcademicos() { return objetivosAcademicos; }
    public void setObjetivosAcademicos(String objetivosAcademicos) { this.objetivosAcademicos = objetivosAcademicos; }

    public void imprimeInformacion() {
        System.out.println("          Fecha de nacimiento: " + fechaDeNacimiento);
        System.out.println("          Aficiones: " + aficiones);
        System.out.println("          Asignatura favorita: " + asignaturaFavorita);
        System.out.println("          Objetivos académicos: " + objetivosAcademicos);
    }
}

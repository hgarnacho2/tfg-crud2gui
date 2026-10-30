import java.util.Date;

public class FichaSocio {
    private Date fechaDeNacimiento;
    private String objetivo;
    private String lesiones;
    private String nivelActividad;

    public Date getFechaDeNacimiento() { return fechaDeNacimiento; }
    public void setFechaDeNacimiento(Date fechaDeNacimiento) { this.fechaDeNacimiento = fechaDeNacimiento; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getLesiones() { return lesiones; }
    public void setLesiones(String lesiones) { this.lesiones = lesiones; }
    public String getNivelActividad() { return nivelActividad; }
    public void setNivelActividad(String nivelActividad) { this.nivelActividad = nivelActividad; }

    public void imprimeInformacion() {
        System.out.println("          Fecha de nacimiento: " + fechaDeNacimiento);
        System.out.println("          Objetivo: " + objetivo);
        System.out.println("          Lesiones: " + lesiones);
        System.out.println("          Nivel de actividad: " + nivelActividad);
    }
}

/**
 * Perfil profesional: extiende Perfil con curriculum y ocupacion actual.
 */
public class PerfilProfesional extends Perfil {
    private String curriculum;
    private String ocupacion;

    public PerfilProfesional(String nombre, String password) {
        super(nombre, password);
    }

    public String getCurriculum() { return curriculum; }
    public void setCurriculum(String curriculum) { this.curriculum = curriculum; }

    public String getOcupacion() { return ocupacion; }
    public void setOcupacion(String ocupacion) { this.ocupacion = ocupacion; }

    /** Reescribe el metodo del padre anadiendo la etiqueta y los datos profesionales. */
    @Override
    public void imprimeInformacion() {
        super.imprimeInformacion();
        System.out.println("Perfil profesional");
        System.out.println("Currículum: " + curriculum);
        System.out.println("Ocupación actual: " + ocupacion);
    }
}

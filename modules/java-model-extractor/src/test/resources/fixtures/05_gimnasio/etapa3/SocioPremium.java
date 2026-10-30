public class SocioPremium extends Socio {
    private String entrenador;
    private String planNutricional;

    public SocioPremium(String nombre, String password) { super(nombre, password); }
    public String getEntrenador() { return entrenador; }
    public void setEntrenador(String entrenador) { this.entrenador = entrenador; }
    public String getPlanNutricional() { return planNutricional; }
    public void setPlanNutricional(String planNutricional) { this.planNutricional = planNutricional; }

    @Override
    public void imprimeInformacion() {
        super.imprimeInformacion();
        System.out.println("     Entrenador personal: " + entrenador);
        System.out.println("     Plan nutricional: " + planNutricional);
    }
}

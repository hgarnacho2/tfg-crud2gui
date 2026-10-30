public class LectorInvestigador extends Lector {
    private String lineaInvestigacion;
    private String centro;

    public LectorInvestigador(String nombre, String password) { super(nombre, password); }
    public String getLineaInvestigacion() { return lineaInvestigacion; }
    public void setLineaInvestigacion(String lineaInvestigacion) { this.lineaInvestigacion = lineaInvestigacion; }
    public String getCentro() { return centro; }
    public void setCentro(String centro) { this.centro = centro; }

    @Override
    public void imprimeInformacion() {
        super.imprimeInformacion();
        System.out.println("     Línea de investigación: " + lineaInvestigacion);
        System.out.println("     Centro de investigación: " + centro);
    }
}

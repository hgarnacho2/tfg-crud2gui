public class AlumnoBecado extends Alumno {
    private String entidadBecaria;
    private String importeBeca;

    public AlumnoBecado(String nombre, String password) { super(nombre, password); }
    public String getEntidadBecaria() { return entidadBecaria; }
    public void setEntidadBecaria(String entidadBecaria) { this.entidadBecaria = entidadBecaria; }
    public String getImporteBeca() { return importeBeca; }
    public void setImporteBeca(String importeBeca) { this.importeBeca = importeBeca; }

    @Override
    public void imprimeInformacion() {
        super.imprimeInformacion();
        System.out.println("     Entidad becaria: " + entidadBecaria);
        System.out.println("     Importe de la beca: " + importeBeca);
    }
}

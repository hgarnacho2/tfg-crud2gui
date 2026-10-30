public class Lector {
    private String nombre;
    private String idLector;
    private String password;
    private String URL;
    private FichaLector ficha;

    public Lector() { }

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

    public void imprimeInformacion() {
        System.out.println("Identificador de lector: " + idLector);
        System.out.println("     Nombre: " + nombre);
        System.out.println("     URL: " + URL);
        if (ficha != null) {
            System.out.println("     FICHA:");
            ficha.imprimeInformacion();
        }
    }
}

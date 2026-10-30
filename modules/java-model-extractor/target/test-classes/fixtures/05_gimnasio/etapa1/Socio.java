public class Socio {
    private String nombre;
    private String idSocio;
    private String password;
    private String URL;
    private FichaSocio ficha;

    public Socio() { }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getIdSocio() { return idSocio; }
    public void setIdSocio(String idSocio) { this.idSocio = idSocio; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getURL() { return URL; }
    public void setURL(String URL) { this.URL = URL; }
    public FichaSocio getFicha() { return ficha; }
    public void setFicha(FichaSocio ficha) { this.ficha = ficha; }

    public void imprimeInformacion() {
        System.out.println("Identificador de socio: " + idSocio);
        System.out.println("     Nombre: " + nombre);
        System.out.println("     URL: " + URL);
        if (ficha != null) {
            System.out.println("     FICHA:");
            ficha.imprimeInformacion();
        }
    }
}

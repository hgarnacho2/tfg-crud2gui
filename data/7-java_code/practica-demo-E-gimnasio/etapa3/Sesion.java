public class Sesion {
    private String texto;
    private String URL;

    public Sesion(String texto) { this.texto = texto; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public String getURL() { return URL; }
    public void setURL(String URL) { this.URL = URL; }

    public void imprimeInformacion() {
        System.out.println("          " + URL + " " + texto);
    }
}

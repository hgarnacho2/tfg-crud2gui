/**
 * Mensaje publicado por un usuario.
 */
public class Post {
    private String texto;
    private String url;

    public Post(String texto) {
        this.texto = texto;
    }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getURL() { return url; }
    public void setURL(String url) { this.url = url; }

    /** Imprime la URL y el texto del post. */
    public void imprimeInformacion() {
        System.out.println(url + " " + texto);
    }
}

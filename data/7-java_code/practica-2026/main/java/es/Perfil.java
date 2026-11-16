import java.util.ArrayList;

/**
 * Perfil de un usuario de la red social. Se relaciona por composicion con
 * Biografia y con una lista de Post.
 */
public class Perfil {
    private String nombre;
    private String idUsuario;
    private String password;
    private String URL;
    private Biografia biografia;
    private ArrayList<Post> posts;

    public Perfil() {
        this.posts = new ArrayList<Post>();
    }

    public Perfil(String nombre, String password) {
        this();
        this.nombre = nombre;
        this.password = password;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getIdUsuario() { return idUsuario; }
    public void setIdUsuario(String idUsuario) { this.idUsuario = idUsuario; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getURL() { return URL; }
    public void setURL(String URL) { this.URL = URL; }

    public Biografia getBiografia() { return biografia; }
    public void setBiografia(Biografia biografia) { this.biografia = biografia; }

    public ArrayList<Post> getPosts() { return posts; }

    /**
     * Anade un post al perfil y le asigna la URL
     * https://www.miRed.com/post/idUsuario.numero, usando el primer numero libre.
     */
    public void anadirPost(String texto) {
        Post post = new Post(texto);
        int numero = 1;
        while (buscarPost("https://www.miRed.com/post/" + idUsuario + "." + numero) != null) {
            numero++;
        }
        post.setURL("https://www.miRed.com/post/" + idUsuario + "." + numero);
        posts.add(post);
    }

    /** Devuelve el post con esa URL o null si no existe. */
    public Post buscarPost(String urlPost) {
        for (Post p : posts) {
            if (p.getURL().equals(urlPost)) {
                return p;
            }
        }
        return null;
    }

    /** Elimina el post con esa URL. Devuelve true si existia. */
    public boolean eliminarPost(String urlPost) {
        Post p = buscarPost(urlPost);
        if (p == null) {
            return false;
        }
        posts.remove(p);
        return true;
    }

    /** Imprime en pantalla toda la informacion del perfil, incluidos sus posts. */
    public void imprimeInformacion() {
        System.out.println("Identificador de usuario: " + idUsuario);
        System.out.println("Nombre: " + nombre);
        System.out.println("URL: " + URL);
        System.out.println("BIO:");
        if (biografia != null) {
            biografia.imprimeInformacion();
        }
        if (posts != null && !posts.isEmpty()) {
            System.out.println("POSTS:");
            for (Post p : posts) {
                p.imprimeInformacion();
            }
        }
    }
}

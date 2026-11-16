import java.util.ArrayList;

/**
 * Red social: gestiona los perfiles dados de alta.
 */
public class MiRed {
    private ArrayList<Perfil> perfiles;

    public MiRed() {
        this.perfiles = new ArrayList<Perfil>();
    }

    /** Da de alta un perfil asignandole identificador (primer numero libre) y URL. */
    public void anadirPerfil(Perfil perfil) {
        int numero = 1;
        while (buscarUsuario("user" + numero) != null) {
            numero++;
        }
        String id = "user" + numero;
        perfil.setIdUsuario(id);
        perfil.setURL("https://www.miRed.com/" + id);
        perfiles.add(perfil);
    }

    /** Devuelve el perfil con ese identificador o null si no existe. */
    private Perfil buscarUsuario(String idUsuario) {
        for (Perfil p : perfiles) {
            if (p.getIdUsuario().equals(idUsuario)) {
                return p;
            }
        }
        return null;
    }

    public boolean comprobarPassword(String idUsuario, String password) {
        Perfil p = buscarUsuario(idUsuario);
        if (p == null) {
            System.out.println("No existe el usuario " + idUsuario);
            return false;
        }
        if (p.getPassword().equals(password)) {
            System.out.println("Password correcta");
            return true;
        }
        System.out.println("La password es incorrecta");
        return false;
    }

    public void visualizarRed() {
        System.out.println("Usuarios en la red:");
        for (Perfil p : perfiles) {
            System.out.println(p.getIdUsuario() + ": " + p.getNombre());
        }
    }

    public void visualizarPerfil(String idUsuario) {
        Perfil p = buscarUsuario(idUsuario);
        if (p == null) {
            System.out.println("No existe el usuario " + idUsuario);
        } else {
            p.imprimeInformacion();
        }
    }

    public void anadirPost(String idUsuario, String texto) {
        Perfil p = buscarUsuario(idUsuario);
        if (p == null) {
            System.out.println("No existe el identificador de usuario " + idUsuario);
        } else {
            p.anadirPost(texto);
        }
    }

    /** ETAPA 4: elimina un perfil. Devuelve true si existia. */
    public boolean eliminarPerfil(String idUsuario) {
        Perfil p = buscarUsuario(idUsuario);
        if (p == null) {
            System.out.println("No existe el usuario " + idUsuario);
            return false;
        }
        perfiles.remove(p);
        return true;
    }

    /** ETAPA 4: elimina un post de un usuario a partir de su URL. */
    public boolean eliminarPost(String idUsuario, String urlPost) {
        Perfil p = buscarUsuario(idUsuario);
        if (p == null) {
            System.out.println("No existe el usuario " + idUsuario);
            return false;
        }
        if (!p.eliminarPost(urlPost)) {
            System.out.println("No existe el post " + urlPost);
            return false;
        }
        return true;
    }
}

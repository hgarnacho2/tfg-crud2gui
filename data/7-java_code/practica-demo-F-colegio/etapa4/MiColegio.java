import java.util.ArrayList;

public class MiColegio {
    private ArrayList<Alumno> alumnos = new ArrayList<Alumno>();

    public void anadirAlumno(Alumno x) {
        int n = 1;
        while (buscarAlumno("alumno" + n) != null) { n++; }
        x.setIdAlumno("alumno" + n);
        x.setURL("https://www.miColegio.com/alumno" + n);
        alumnos.add(x);
    }

    private Alumno buscarAlumno(String id) {
        for (Alumno x : alumnos) {
            if (x.getIdAlumno().equals(id)) { return x; }
        }
        return null;
    }

    public boolean comprobarPassword(String id, String password) {
        Alumno x = buscarAlumno(id);
        if (x == null) { System.out.println("No existe el alumno " + id); return false; }
        if (!x.getPassword().equals(password)) { System.out.println("La password es incorrecta"); return false; }
        System.out.println("Password correcta");
        return true;
    }

    public void visualizarColegio() {
        System.out.println("Alumnos en el colegio:");
        for (Alumno x : alumnos) { System.out.println("     " + x.getIdAlumno() + ": " + x.getNombre()); }
    }

    public void visualizarAlumno(String id) {
        Alumno x = buscarAlumno(id);
        if (x == null) { System.out.println("No existe el alumno " + id); } else { x.imprimeInformacion(); }
    }

    public void anadirAnotacion(String id, String texto) {
        Alumno x = buscarAlumno(id);
        if (x == null) { System.out.println("No existe el identificador de alumno " + id); } else { x.anadirAnotacion(texto); }
    }

    public void eliminarAlumno(String id) {
        Alumno x = buscarAlumno(id);
        if (x == null) {
            System.out.println("No existe el alumno " + id);
        } else {
            alumnos.remove(x);
            System.out.println("El alumno " + id + " ha sido eliminado");
        }
    }

    public void eliminarAnotacion(String id, String url) {
        Alumno x = buscarAlumno(id);
        if (x == null) {
            System.out.println("No existe el identificador de alumno " + id);
        } else if (x.eliminarAnotacion(url)) {
            System.out.println("La anotación con URL " + url + " ha sido eliminada");
        } else {
            System.out.println("No existe la anotación con URL " + url);
        }
    }
}

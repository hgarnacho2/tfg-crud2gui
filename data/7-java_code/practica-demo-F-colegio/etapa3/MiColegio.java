import java.util.ArrayList;

public class MiColegio {
    private ArrayList<Alumno> alumnos = new ArrayList<Alumno>();

    public void anadirAlumno(Alumno x) {
        int n = alumnos.size() + 1;
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
}

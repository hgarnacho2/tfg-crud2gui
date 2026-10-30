import java.util.ArrayList;

public class Instructor {
    private String nombre;
    private String email;
    private ArrayList<Curso> cursos;

    public Instructor(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
        this.cursos = new ArrayList<Curso>();
        System.out.println("Creado instructor " + email);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public ArrayList<Curso> getCursos() { return cursos; }

    /** Añade el curso a la lista y lo deja en estado ABIERTO. */
    public void addCurso(Curso x) {
        x.setEstado(T_Estado.ABIERTO);
        cursos.add(x);
    }

    /** Elimina de la lista el curso con el id indicado. Devuelve true si existía. */
    public boolean removeCurso(int id) {
        for (int k = 0; k < cursos.size(); k++) {
            if (cursos.get(k).getId() == id) {
                cursos.remove(k);
                return true;
            }
        }
        return false;
    }

    public void print() {
        System.out.println("------------");
        System.out.println("Nombre: " + nombre);
        System.out.println("Email: " + email);
        System.out.println("------------");
        for (Curso x : cursos) {
            x.print();
        }
    }
}

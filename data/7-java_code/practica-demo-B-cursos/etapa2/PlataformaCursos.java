import java.util.ArrayList;

public class PlataformaCursos {
    private String nombre;
    private ArrayList<Instructor> instructores;

    public PlataformaCursos(String nombre) {
        this.nombre = nombre;
        this.instructores = new ArrayList<Instructor>();
        System.out.println("Creada plataforma de cursos " + nombre);
    }

    private Instructor buscarInstructor(String email) {
        for (Instructor x : instructores) {
            if (x.getEmail().equals(email)) { return x; }
        }
        return null;
    }

    public void addInstructor(Instructor x) {
        instructores.add(x);
        System.out.println("El instructor con email " + x.getEmail() + " ha sido añadido");
    }

    public void removeInstructor(String email) {
        Instructor x = buscarInstructor(email);
        if (x != null) {
            instructores.remove(x);
            System.out.println("El instructor con email " + email + " ha sido eliminado");
        }
    }

    public void addCurso2Instructor(Curso a, String email) {
        Instructor x = buscarInstructor(email);
        if (x != null) {
            x.addCurso(a);
            System.out.println("El curso con id " + a.getId() + " se ha añadido al instructor " + email);
        }
    }

    public void removeCurso2Instructor(int id, String email) {
        Instructor x = buscarInstructor(email);
        if (x != null && x.removeCurso(id)) {
            System.out.println("El curso con id " + id + " se ha eliminado de la lista del instructor " + email);
        }
    }

    public void printAllInstructores() {
        for (Instructor x : instructores) { x.print(); }
    }

    public void printAllCursos() {
        for (Instructor x : instructores) {
            for (Curso a : x.getCursos()) { a.print(); }
        }
    }

    public void printCursosByInstructor(String email) {
        Instructor x = buscarInstructor(email);
        if (x != null) {
            for (Curso a : x.getCursos()) { a.print(); }
        }
    }

    // ---- Parte opcional ----
    public void printCursosByState(T_Estado estado) {
        for (Instructor x : instructores) {
            for (Curso a : x.getCursos()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void printCursosByInstructor(String email, T_Estado estado) {
        Instructor x = buscarInstructor(email);
        if (x != null) {
            for (Curso a : x.getCursos()) {
                if (a.getEstado() == estado) { a.print(); }
            }
        }
    }

    public void searchCursosByKeyword(String palabra) {
        for (Instructor x : instructores) {
            for (Curso a : x.getCursos()) {
                if (a.getDescripcion().toLowerCase().contains(palabra.toLowerCase())) { a.print(); }
            }
        }
    }
}

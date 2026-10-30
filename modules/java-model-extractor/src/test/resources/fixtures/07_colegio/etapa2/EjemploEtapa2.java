import java.util.Date;

public class EjemploEtapa2 {
    public static void main(String[] args) {
        Alumno p1=new Alumno("Sofía Navarro Lara","estudio01");
        FichaAlumno f=new FichaAlumno();
        Date fecha = new Date(108, 1, 12);
        f.setFechaDeNacimiento(fecha);
        f.setAficiones("Dibujo y baloncesto");
        f.setAsignaturaFavorita("Matemáticas");
        f.setObjetivosAcademicos("Estudiar Medicina");
        p1.setFicha(f);
        Alumno p2=new Alumno("Diego Herrero Soto","mates2024");
        FichaAlumno f2=new FichaAlumno();
        Date fecha2 = new Date(107, 10, 27);
        f2.setFechaDeNacimiento(fecha2);
        f2.setAficiones("Videojuegos y guitarra");
        f2.setAsignaturaFavorita("Música");
        f2.setObjetivosAcademicos("Cursar un grado en Ingeniería");
        p2.setFicha(f2);
        MiColegio r=new MiColegio();
        r.anadirAlumno(p1);
        r.anadirAlumno(p2);
        r.visualizarColegio();
        r.visualizarAlumno("alumno56");
        r.visualizarAlumno("alumno1");
        boolean comprobacion1 = r.comprobarPassword("alumno56","estudio");
        boolean comprobacion2 = r.comprobarPassword("alumno1","estudio");
        boolean comprobacion3 = r.comprobarPassword("alumno1","estudio01");
        r.anadirAnotacion("alumno56","Participa activamente en clase");
        r.anadirAnotacion("alumno1","Participa activamente en clase");
        r.anadirAnotacion("alumno1","Entrega los trabajos con retraso");
        r.visualizarAlumno("alumno1");
    }
}

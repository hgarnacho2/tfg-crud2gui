import java.util.Date;

public class EjemploEtapa3 {
    public static void main(String[] args) {
        Alumno p1=new Alumno("Sofía Navarro Lara","estudio01");
        FichaAlumno f=new FichaAlumno();
        Date fecha = new Date(108, 1, 12);
        f.setFechaDeNacimiento(fecha);
        f.setAficiones("Dibujo y baloncesto");
        f.setAsignaturaFavorita("Matemáticas");
        f.setObjetivosAcademicos("Estudiar Medicina");
        p1.setFicha(f);
        AlumnoBecado p2=new AlumnoBecado("Diego Herrero Soto","mates2024");
        FichaAlumno f2=new FichaAlumno();
        Date fecha2 = new Date(107, 10, 27);
        f2.setFechaDeNacimiento(fecha2);
        f2.setAficiones("Videojuegos y guitarra");
        f2.setAsignaturaFavorita("Música");
        f2.setObjetivosAcademicos("Cursar un grado en Ingeniería");
        p2.setFicha(f2);
        p2.setEntidadBecaria("Fundación Educa Futuro");
        p2.setImporteBeca("1200 euros anuales");
        MiColegio r=new MiColegio();
        r.anadirAlumno(p1);
        r.anadirAlumno(p2);
        r.visualizarColegio();
        r.visualizarAlumno("alumno2");
        boolean comprobacion1 = r.comprobarPassword("alumno56","estudio");
        boolean comprobacion2 = r.comprobarPassword("alumno2","mat");
        boolean comprobacion3 = r.comprobarPassword("alumno2","mates2024");
        r.anadirAnotacion("alumno2","Excelente actitud en el laboratorio");
        r.anadirAnotacion("alumno2","Ha mejorado su nota de matemáticas");
        r.visualizarAlumno("alumno2");
    }
}

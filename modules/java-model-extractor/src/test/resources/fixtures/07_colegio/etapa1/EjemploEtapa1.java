import java.util.Date;

public class EjemploEtapa1 {
    public static void main(String[] args) {
        Alumno p=new Alumno();
        p.setIdAlumno("alumno1");
        p.setNombre("Sofía Navarro Lara");
        p.setURL("https://www.miColegio.com/alumno1");
        p.setPassword("estudio01");
        FichaAlumno f=new FichaAlumno();
        Date fecha = new Date(108, 1, 12);
        f.setFechaDeNacimiento(fecha);
        f.setAficiones("Dibujo y baloncesto");
        f.setAsignaturaFavorita("Matemáticas");
        f.setObjetivosAcademicos("Estudiar Medicina");
        p.setFicha(f);
        p.imprimeInformacion();
    }
}

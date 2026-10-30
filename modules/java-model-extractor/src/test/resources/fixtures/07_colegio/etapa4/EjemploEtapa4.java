public class EjemploEtapa4 {
    public static void main(String[] args) {
        MiColegio r = new MiColegio();
        Alumno s1 = new Alumno("Sofía Navarro Lara","clave1");
        Alumno s2 = new Alumno("Diego Herrero Soto","clave2");
        Alumno s3 = new Alumno("Nombre Prueba Tres","clave3");
        r.anadirAlumno(s1);
        r.anadirAlumno(s2);
        r.anadirAlumno(s3);
        r.visualizarColegio();
        r.eliminarAlumno("alumno2");
        r.eliminarAlumno("alumno2");
        r.visualizarColegio();
        Alumno s4 = new Alumno("Nombre Prueba Cuatro","clave4");
        r.anadirAlumno(s4);
        r.visualizarColegio();
        r.anadirAnotacion("alumno1","Participa activamente en clase");
        r.anadirAnotacion("alumno1","Entrega los trabajos con retraso");
        r.anadirAnotacion("alumno1","Excelente actitud en el laboratorio");
        r.eliminarAnotacion("alumno1","https://www.miColegio.com/anotacion/alumno1.2");
        r.eliminarAnotacion("alumno1","https://www.miColegio.com/anotacion/alumno1.2");
        r.anadirAnotacion("alumno1","Ha mejorado su nota de matemáticas");
        r.eliminarAnotacion("alumno56","https://www.miColegio.com/anotacion/alumno1.1");
        r.visualizarAlumno("alumno1");
    }
}

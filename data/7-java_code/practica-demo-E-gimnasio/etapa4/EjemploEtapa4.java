public class EjemploEtapa4 {
    public static void main(String[] args) {
        MiGym r = new MiGym();
        Socio s1 = new Socio("Laura Prieto Gil","clave1");
        Socio s2 = new Socio("Álvaro Muñoz Peña","clave2");
        Socio s3 = new Socio("Nombre Prueba Tres","clave3");
        r.anadirSocio(s1);
        r.anadirSocio(s2);
        r.anadirSocio(s3);
        r.visualizarGimnasio();
        r.eliminarSocio("socio2");
        r.eliminarSocio("socio2");
        r.visualizarGimnasio();
        Socio s4 = new Socio("Nombre Prueba Cuatro","clave4");
        r.anadirSocio(s4);
        r.visualizarGimnasio();
        r.anadirSesion("socio1","Entrenamiento de fuerza: pecho y espalda");
        r.anadirSesion("socio1","Clase de spinning de 45 minutos");
        r.anadirSesion("socio1","Rutina de piernas y glúteos");
        r.eliminarSesion("socio1","https://www.miGym.com/sesion/socio1.2");
        r.eliminarSesion("socio1","https://www.miGym.com/sesion/socio1.2");
        r.anadirSesion("socio1","Sesión de natación suave");
        r.eliminarSesion("socio56","https://www.miGym.com/sesion/socio1.1");
        r.visualizarSocio("socio1");
    }
}

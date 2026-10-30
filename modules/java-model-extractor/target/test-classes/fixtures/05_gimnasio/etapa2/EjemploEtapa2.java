import java.util.Date;

public class EjemploEtapa2 {
    public static void main(String[] args) {
        Socio p1=new Socio("Laura Prieto Gil","pesas2024");
        FichaSocio f=new FichaSocio();
        Date fecha = new Date(88, 0, 24);
        f.setFechaDeNacimiento(fecha);
        f.setObjetivo("Ganar masa muscular");
        f.setLesiones("Ninguna");
        f.setNivelActividad("Intermedio");
        p1.setFicha(f);
        Socio p2=new Socio("Álvaro Muñoz Peña","cardio99");
        FichaSocio f2=new FichaSocio();
        Date fecha2 = new Date(95, 1, 9);
        f2.setFechaDeNacimiento(fecha2);
        f2.setObjetivo("Perder peso");
        f2.setLesiones("Rodilla derecha");
        f2.setNivelActividad("Principiante");
        p2.setFicha(f2);
        MiGym r=new MiGym();
        r.anadirSocio(p1);
        r.anadirSocio(p2);
        r.visualizarGimnasio();
        r.visualizarSocio("socio56");
        r.visualizarSocio("socio1");
        boolean comprobacion1 = r.comprobarPassword("socio56","pesas");
        boolean comprobacion2 = r.comprobarPassword("socio1","pesas");
        boolean comprobacion3 = r.comprobarPassword("socio1","pesas2024");
        r.anadirSesion("socio56","Entrenamiento de fuerza: pecho y espalda");
        r.anadirSesion("socio1","Entrenamiento de fuerza: pecho y espalda");
        r.anadirSesion("socio1","Clase de spinning de 45 minutos");
        r.visualizarSocio("socio1");
    }
}

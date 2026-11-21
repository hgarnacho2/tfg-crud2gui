import java.util.Date;

public class EjemploEtapa3 {
    public static void main(String[] args) {
        Socio p1=new Socio("Laura Prieto Gil","pesas2024");
        FichaSocio f=new FichaSocio();
        Date fecha = new Date(88, 0, 24);
        f.setFechaDeNacimiento(fecha);
        f.setObjetivo("Ganar masa muscular");
        f.setLesiones("Ninguna");
        f.setNivelActividad("Intermedio");
        p1.setFicha(f);
        SocioPremium p2=new SocioPremium("Álvaro Muñoz Peña","cardio99");
        FichaSocio f2=new FichaSocio();
        Date fecha2 = new Date(95, 1, 9);
        f2.setFechaDeNacimiento(fecha2);
        f2.setObjetivo("Perder peso");
        f2.setLesiones("Rodilla derecha");
        f2.setNivelActividad("Principiante");
        p2.setFicha(f2);
        p2.setEntrenador("Marcos Vidal Ortiz");
        p2.setPlanNutricional("Dieta de definición de 2200 calorías");
        MiGym r=new MiGym();
        r.anadirSocio(p1);
        r.anadirSocio(p2);
        r.visualizarGimnasio();
        r.visualizarSocio("socio2");
        boolean comprobacion1 = r.comprobarPassword("socio56","pesas");
        boolean comprobacion2 = r.comprobarPassword("socio2","card");
        boolean comprobacion3 = r.comprobarPassword("socio2","cardio99");
        r.anadirSesion("socio2","Rutina de piernas y glúteos");
        r.anadirSesion("socio2","Sesión de natación suave");
        r.visualizarSocio("socio2");
    }
}

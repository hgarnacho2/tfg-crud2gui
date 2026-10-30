import java.util.Date;

public class EjemploEtapa1 {
    public static void main(String[] args) {
        Socio p=new Socio();
        p.setIdSocio("socio1");
        p.setNombre("Laura Prieto Gil");
        p.setURL("https://www.miGym.com/socio1");
        p.setPassword("pesas2024");
        FichaSocio f=new FichaSocio();
        Date fecha = new Date(88, 0, 24);
        f.setFechaDeNacimiento(fecha);
        f.setObjetivo("Ganar masa muscular");
        f.setLesiones("Ninguna");
        f.setNivelActividad("Intermedio");
        p.setFicha(f);
        p.imprimeInformacion();
    }
}

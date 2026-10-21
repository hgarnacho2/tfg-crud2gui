import java.util.Date;

public class EjemploEtapa3 {
    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        Perfil p1 = new Perfil("Pedro González", "migatoluis0");
        Biografia b = new Biografia();
        b.setFechaDeNacimiento(new Date(74, 11, 17));
        b.setAficiones("los gatos");
        b.setProfesion("Informático");
        b.setObjetivosVitales("Salvar animales");
        p1.setBiografia(b);

        PerfilProfesional p2 = new PerfilProfesional("Juan Pérez", "lalalala");
        Biografia b2 = new Biografia();
        b2.setFechaDeNacimiento(new Date(94, 2, 1));
        b2.setAficiones("Música");
        b2.setProfesion("Albañil");
        b2.setObjetivosVitales("Recorrer el mundo");
        p2.setBiografia(b2);
        p2.setCurriculum("Experiencia en múltiples empresas");
        p2.setOcupacion("Encargado de hormigonera en Construcciones S.L.");

        MiRed r = new MiRed();
        r.anadirPerfil(p1);
        r.anadirPerfil(p2);
        r.visualizarRed();
        r.visualizarPerfil("user2");
        r.comprobarPassword("user56", "migato");
        r.comprobarPassword("user2", "lala");
        r.comprobarPassword("user2", "lalalala");
        r.anadirPost("user56", "Hoy me siento bien");
        r.anadirPost("user2", "Hoy me siento bien");
        r.anadirPost("user2", "Hoy me siento mal");
        r.visualizarPerfil("user2");
    }
}

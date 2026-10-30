public class LanzadorEtapa3_1 {
    public static void main(String[] args) {
        System.out.println("--------------------------------");
        System.out.println("Creamos la plataforma de eventos");
        System.out.println("--------------------------------");
        PlataformaEventos gestor = new PlataformaEventos("Tickly");
        System.out.println("---------------------");
        System.out.println("Creamos organizadores");
        System.out.println("---------------------");
        Organizador organizador1 = new Organizador("Sergio Navarro Bel","sergionavarro@correo.es");
        Organizador organizador2 = new Organizador("Carmen Lozano Rey","carmenlozano@mimail.com");
        System.out.println("----------------------");
        System.out.println("Añadimos organizadores");
        System.out.println("----------------------");
        gestor.addOrganizador(organizador1);
        gestor.addOrganizador(organizador2);
        System.out.println("---------------");
        System.out.println("Creamos eventos");
        System.out.println("---------------");
        Evento evento1 = new Concierto(1,"Concierto de jazz en el Retiro",25,
                "Jazz al aire libre con cuarteto en directo. Puertas 20:00.",
                T_Estado.DISPONIBLE,"Cuarteto Nocturno",800);
        Evento evento2 = new Conferencia(2,"Conferencia sobre inteligencia artificial",15,
                "Charla divulgativa con preguntas. Incluye certificado.",
                T_Estado.DISPONIBLE,"Ana Beltrán",1.5,true);
        Evento evento3 = new Conferencia(3,"Charla sobre sostenibilidad urbana",10,
                "Ponencia abierta con debate final.",
                T_Estado.DISPONIBLE,"Pablo Ruiz",2,false);
        Evento evento4 = new Concierto(4,"Concierto de rock en la sala Apolo",22,
                "Tres grupos locales en directo. Incluye consumición.",
                T_Estado.DISPONIBLE,"Los Tejados",450);
        System.out.println("----------------------------------------");
        System.out.println("Añadimos los eventos a los organizadores");
        System.out.println("----------------------------------------");
        gestor.addEvento2Organizador(evento1,"sergionavarro@correo.es");
        gestor.addEvento2Organizador(evento2,"carmenlozano@mimail.com");
        gestor.addEvento2Organizador(evento3,"carmenlozano@mimail.com");
        gestor.addEvento2Organizador(evento4,"sergionavarro@correo.es");
        System.out.println("-------------------------------------");
        System.out.println("Eliminamos un evento a un organizador");
        System.out.println("-------------------------------------");
        gestor.removeEvento2Organizador(1,organizador1.getEmail());
        gestor.printEventosByOrganizador(organizador1.getEmail());
        System.out.println("----------------------------");
        System.out.println("Imprimimos todos los eventos");
        System.out.println("----------------------------");
        gestor.printAllEventos();
        // **************
        // PARTE OPCIONAL
        // **************
        evento2.setEstado(T_Estado.AGOTADO);
        System.out.println("----------------------------------");
        System.out.println("Imprimimos los eventos disponibles");
        System.out.println("----------------------------------");
        gestor.printEventosByState(T_Estado.DISPONIBLE);
        System.out.println("-------------------------------");
        System.out.println("Imprimimos los eventos agotados");
        System.out.println("-------------------------------");
        gestor.printEventosByState(T_Estado.AGOTADO);
        System.out.println("----------------------------------------------------");
        System.out.println("Imprimimos los eventos disponibles de un organizador");
        System.out.println("----------------------------------------------------");
        gestor.printEventosByOrganizador(organizador1.getEmail(),T_Estado.DISPONIBLE);
    }
}

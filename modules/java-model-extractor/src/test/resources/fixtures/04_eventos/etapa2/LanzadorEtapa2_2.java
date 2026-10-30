public class LanzadorEtapa2_2 {
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
        Organizador organizador3 = new Organizador("Inés Vidal Mora","inesvidal@otromail.com");
        System.out.println("----------------------");
        System.out.println("Añadimos organizadores");
        System.out.println("----------------------");
        gestor.addOrganizador(organizador1);
        gestor.addOrganizador(organizador2);
        gestor.addOrganizador(organizador3);
        System.out.println("---------------");
        System.out.println("Creamos eventos");
        System.out.println("---------------");
        Evento evento1 = new Evento(1,"Concierto de jazz en el Retiro",25,
                "Jazz al aire libre con cuarteto en directo. Puertas 20:00.",T_Estado.DISPONIBLE);
        Evento evento2 = new Evento(2,"Conferencia sobre inteligencia artificial",15,
                "Charla divulgativa con preguntas. Incluye certificado.",T_Estado.DISPONIBLE);
        Evento evento3 = new Evento(3,"Festival de cine independiente",12,
                "Cortometrajes y coloquio con los directores.",T_Estado.DISPONIBLE);
        Evento evento4 = new Evento(4,"Taller de cocina mediterránea",30.5,
                "Taller práctico de cuatro horas. Incluye degustación.",T_Estado.DISPONIBLE);
        Evento evento5 = new Evento(5,"Concierto de rock en la sala Apolo",22,
                "Tres grupos locales en directo. Incluye consumición.",T_Estado.DISPONIBLE);
        System.out.println("----------------------------------------");
        System.out.println("Añadimos los eventos a los organizadores");
        System.out.println("----------------------------------------");
        gestor.addEvento2Organizador(evento1,"sergionavarro@correo.es");
        gestor.addEvento2Organizador(evento2,"carmenlozano@mimail.com");
        gestor.addEvento2Organizador(evento3,"inesvidal@otromail.com");
        gestor.addEvento2Organizador(evento4,"sergionavarro@correo.es");
        gestor.addEvento2Organizador(evento5,"inesvidal@otromail.com");
        System.out.println("-------------------------------");
        System.out.println("Imprimimos la info de un evento");
        System.out.println("-------------------------------");
        evento2.print();
        System.out.println("------------------------------------");
        System.out.println("Imprimimos la info de un organizador");
        System.out.println("------------------------------------");
        organizador3.print();
        System.out.println("----------------------------------------------");
        System.out.println("Eliminamos un organizador a partir de su email");
        System.out.println("----------------------------------------------");
        gestor.removeOrganizador("inesvidal@otromail.com");
        System.out.println("------------------------------------");
        System.out.println("Imprimimos la lista de organizadores");
        System.out.println("------------------------------------");
        gestor.printAllOrganizadores();
        System.out.println("-----------------------------------------------");
        System.out.println("Imprimimos el total de eventos en la plataforma");
        System.out.println("-----------------------------------------------");
        gestor.printAllEventos();
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
        System.out.println("-------------------------------------------------------------");
        System.out.println("Imprimimos los eventos que contienen la palabra \"certificado\"");
        System.out.println("-------------------------------------------------------------");
        gestor.searchEventosByKeyword("certificado");
    }
}

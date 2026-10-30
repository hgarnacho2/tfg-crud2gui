public class LanzadorEtapa3_1 {
    public static void main(String[] args) {
        System.out.println("-------------------------------------");
        System.out.println("Creamos la plataforma de alojamientos");
        System.out.println("-------------------------------------");
        PlataformaAlojamientos gestor = new PlataformaAlojamientos("Vacanza");
        System.out.println("-------------------");
        System.out.println("Creamos anfitriones");
        System.out.println("-------------------");
        Anfitrion anfitrion1 = new Anfitrion("Lucía Mora Vidal","lucia.mora@correo.es");
        Anfitrion anfitrion2 = new Anfitrion("Pablo Sanz Rey","pablosanz@mimail.com");
        System.out.println("--------------------");
        System.out.println("Añadimos anfitriones");
        System.out.println("--------------------");
        gestor.addAnfitrion(anfitrion1);
        gestor.addAnfitrion(anfitrion2);
        System.out.println("--------------------");
        System.out.println("Creamos alojamientos");
        System.out.println("--------------------");
        Alojamiento alojamiento1 = new AlojamientoUrbano(1,"Apartamento céntrico en Madrid",95,
                "Luminoso, junto a la Puerta del Sol. Wifi y aire acondicionado.",
                T_Estado.DISPONIBLE,2,60.5,3);
        Alojamiento alojamiento2 = new AlojamientoRural(2,"Casa rural en la sierra de Gredos",140,
                "Casa de piedra con chimenea y barbacoa. Se admiten mascotas.",
                T_Estado.DISPONIBLE,1200,true);
        Alojamiento alojamiento3 = new AlojamientoRural(3,"Cabaña junto al lago",85,
                "Cabaña de madera junto al lago con zona de pesca.",
                T_Estado.DISPONIBLE,800,false);
        Alojamiento alojamiento4 = new AlojamientoUrbano(4,"Loft con vistas en Barcelona",120.5,
                "Loft moderno con vistas a la ciudad, cerca del metro.",
                T_Estado.DISPONIBLE,1,45,5);
        System.out.println("-------------------------------------------");
        System.out.println("Añadimos los alojamientos a los anfitriones");
        System.out.println("-------------------------------------------");
        gestor.addAlojamiento2Anfitrion(alojamiento1,"lucia.mora@correo.es");
        gestor.addAlojamiento2Anfitrion(alojamiento2,"pablosanz@mimail.com");
        gestor.addAlojamiento2Anfitrion(alojamiento3,"pablosanz@mimail.com");
        gestor.addAlojamiento2Anfitrion(alojamiento4,"lucia.mora@correo.es");
        System.out.println("----------------------------------------");
        System.out.println("Eliminamos un alojamiento a un anfitrion");
        System.out.println("----------------------------------------");
        gestor.removeAlojamiento2Anfitrion(1,anfitrion1.getEmail());
        gestor.printAlojamientosByAnfitrion(anfitrion1.getEmail());
        System.out.println("---------------------------------");
        System.out.println("Imprimimos todos los alojamientos");
        System.out.println("---------------------------------");
        gestor.printAllAlojamientos();
        // **************
        // PARTE OPCIONAL
        // **************
        alojamiento2.setEstado(T_Estado.OCUPADO);
        System.out.println("---------------------------------------");
        System.out.println("Imprimimos los alojamientos disponibles");
        System.out.println("---------------------------------------");
        gestor.printAlojamientosByState(T_Estado.DISPONIBLE);
        System.out.println("------------------------------------");
        System.out.println("Imprimimos los alojamientos ocupados");
        System.out.println("------------------------------------");
        gestor.printAlojamientosByState(T_Estado.OCUPADO);
        System.out.println("-------------------------------------------------------");
        System.out.println("Imprimimos los alojamientos disponibles de un anfitrion");
        System.out.println("-------------------------------------------------------");
        gestor.printAlojamientosByAnfitrion(anfitrion1.getEmail(),T_Estado.DISPONIBLE);
    }
}

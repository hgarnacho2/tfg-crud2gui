public class LanzadorEtapa2_1 {
    public static void main(String[] args) {
        System.out.println("-----------------------------------");
        System.out.println("Creamos la plataforma de adopciones");
        System.out.println("-----------------------------------");
        PlataformaAdopciones gestor = new PlataformaAdopciones("Huellas");
        System.out.println("----------------");
        System.out.println("Creamos refugios");
        System.out.println("----------------");
        Refugio refugio1 = new Refugio("Refugio Huellas del Sur","refugio.huellas@correo.es");
        Refugio refugio2 = new Refugio("Protectora Amigos Peludos","amigospeludos@mimail.com");
        Refugio refugio3 = new Refugio("Asociación Segunda Oportunidad","segundaoportunidad@otromail.com");
        System.out.println("-----------------");
        System.out.println("Añadimos refugios");
        System.out.println("-----------------");
        gestor.addRefugio(refugio1);
        gestor.addRefugio(refugio2);
        gestor.addRefugio(refugio3);
        System.out.println("----------------");
        System.out.println("Creamos animales");
        System.out.println("----------------");
        Animal animal1 = new Animal(1,"Luna, perra mestiza cariñosa",60,
                "Perra de tres años, sociable con niños. Con chip.",T_Estado.DISPONIBLE);
        Animal animal2 = new Animal(2,"Simba, gato atigrado",40,
                "Gato joven y juguetón. Esterilizado y vacunado.",T_Estado.DISPONIBLE);
        Animal animal3 = new Animal(3,"Rocky, pastor alemán",90,
                "Perro enérgico que necesita jardín. Adiestrado.",T_Estado.DISPONIBLE);
        Animal animal4 = new Animal(4,"Nala, gata siamesa",45,
                "Gata tranquila, ideal para piso. Vacunada y con chip.",T_Estado.DISPONIBLE);
        Animal animal5 = new Animal(5,"Toby, cachorro de labrador",75,
                "Cachorro de cuatro meses. Vacunado, sin esterilizar.",T_Estado.DISPONIBLE);
        System.out.println("------------------------------------");
        System.out.println("Añadimos los animales a los refugios");
        System.out.println("------------------------------------");
        gestor.addAnimal2Refugio(animal1,"refugio.huellas@correo.es");
        gestor.addAnimal2Refugio(animal2,"amigospeludos@mimail.com");
        gestor.addAnimal2Refugio(animal3,"segundaoportunidad@otromail.com");
        gestor.addAnimal2Refugio(animal4,"refugio.huellas@correo.es");
        gestor.addAnimal2Refugio(animal5,"segundaoportunidad@otromail.com");
        System.out.println("-------------------------------");
        System.out.println("Imprimimos la info de un animal");
        System.out.println("-------------------------------");
        animal2.print();
        System.out.println("--------------------------------");
        System.out.println("Imprimimos la info de un refugio");
        System.out.println("--------------------------------");
        refugio3.print();
        System.out.println("------------------------------------------");
        System.out.println("Eliminamos un refugio a partir de su email");
        System.out.println("------------------------------------------");
        gestor.removeRefugio("segundaoportunidad@otromail.com");
        System.out.println("-------------------------------");
        System.out.println("Imprimimos la lista de refugios");
        System.out.println("-------------------------------");
        gestor.printAllRefugios();
        System.out.println("------------------------------------------------");
        System.out.println("Imprimimos el total de animales en la plataforma");
        System.out.println("------------------------------------------------");
        gestor.printAllAnimales();
    }
}

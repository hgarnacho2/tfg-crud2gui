public class LanzadorEtapa3_1 {
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
        System.out.println("-----------------");
        System.out.println("Añadimos refugios");
        System.out.println("-----------------");
        gestor.addRefugio(refugio1);
        gestor.addRefugio(refugio2);
        System.out.println("----------------");
        System.out.println("Creamos animales");
        System.out.println("----------------");
        Animal animal1 = new Perro(1,"Luna, perra mestiza cariñosa",60,
                "Perra de tres años, sociable con niños. Con chip.",
                T_Estado.DISPONIBLE,"Mestiza",18.5,true);
        Animal animal2 = new Gato(2,"Simba, gato atigrado",40,
                "Gato joven y juguetón. Esterilizado y vacunado.",
                T_Estado.DISPONIBLE,14,true);
        Animal animal3 = new Gato(3,"Nala, gata siamesa",45,
                "Gata tranquila, ideal para piso. Vacunada y con chip.",
                T_Estado.DISPONIBLE,30,false);
        Animal animal4 = new Perro(4,"Toby, cachorro de labrador",75,
                "Cachorro de cuatro meses. Vacunado, sin esterilizar.",
                T_Estado.DISPONIBLE,"Labrador",9.5,false);
        System.out.println("------------------------------------");
        System.out.println("Añadimos los animales a los refugios");
        System.out.println("------------------------------------");
        gestor.addAnimal2Refugio(animal1,"refugio.huellas@correo.es");
        gestor.addAnimal2Refugio(animal2,"amigospeludos@mimail.com");
        gestor.addAnimal2Refugio(animal3,"amigospeludos@mimail.com");
        gestor.addAnimal2Refugio(animal4,"refugio.huellas@correo.es");
        System.out.println("---------------------------------");
        System.out.println("Eliminamos un animal a un refugio");
        System.out.println("---------------------------------");
        gestor.removeAnimal2Refugio(1,refugio1.getEmail());
        gestor.printAnimalesByRefugio(refugio1.getEmail());
        System.out.println("-----------------------------");
        System.out.println("Imprimimos todos los animales");
        System.out.println("-----------------------------");
        gestor.printAllAnimales();
        // **************
        // PARTE OPCIONAL
        // **************
        animal2.setEstado(T_Estado.ADOPTADO);
        System.out.println("-----------------------------------");
        System.out.println("Imprimimos los animales disponibles");
        System.out.println("-----------------------------------");
        gestor.printAnimalesByState(T_Estado.DISPONIBLE);
        System.out.println("---------------------------------");
        System.out.println("Imprimimos los animales adoptados");
        System.out.println("---------------------------------");
        gestor.printAnimalesByState(T_Estado.ADOPTADO);
        System.out.println("-------------------------------------------------");
        System.out.println("Imprimimos los animales disponibles de un refugio");
        System.out.println("-------------------------------------------------");
        gestor.printAnimalesByRefugio(refugio1.getEmail(),T_Estado.DISPONIBLE);
    }
}

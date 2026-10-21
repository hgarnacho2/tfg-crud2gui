public class LanzadorEtapa1 {
    public static void main(String[] args) {
        Usuario user1 = new Usuario("Mariano Pérez Oso", "marian0@hismail.com");
        Usuario user2 = new Usuario("Javier Carro Ñero", "javicarronyero@othermail.com");
        Usuario user3 = new Usuario("Antonio N. Migo", "antonomigo@mymail.com");
        user1.print();
        user2.print();
        user3.print();
    }
}
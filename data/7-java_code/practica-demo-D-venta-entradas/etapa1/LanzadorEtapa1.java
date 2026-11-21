public class LanzadorEtapa1 {
    public static void main(String[] args) {
        Organizador organizador1 = new Organizador("Sergio Navarro Bel","sergionavarro@correo.es");
        Organizador organizador2 = new Organizador("Carmen Lozano Rey","carmenlozano@mimail.com");
        Organizador organizador3 = new Organizador("Inés Vidal Mora","inesvidal@otromail.com");
        organizador1.print();
        organizador2.print();
        organizador3.print();
    }
}

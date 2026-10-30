public class LanzadorEtapa1 {
    public static void main(String[] args) {
        Instructor instructor1 = new Instructor("Marta Iglesias Roca","marta.iglesias@correo.es");
        Instructor instructor2 = new Instructor("Raúl Ortega Pons","raulortega@mimail.com");
        Instructor instructor3 = new Instructor("Nuria Campos Vega","nuriacampos@otromail.com");
        instructor1.print();
        instructor2.print();
        instructor3.print();
    }
}

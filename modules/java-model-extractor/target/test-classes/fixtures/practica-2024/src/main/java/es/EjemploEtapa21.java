public class EjemploEtapa21 {
    public static void main(String[] args) {
        Pueblo pueblo = new Pueblo("Villanueva");
        pueblo.setPrecioTransporte(20.0);
        Cliente cliente = new Cliente("Juan López", pueblo);
        cliente.setDireccion("C/ Quijote 3");
        cliente.imprimeInformacion();
    }
}
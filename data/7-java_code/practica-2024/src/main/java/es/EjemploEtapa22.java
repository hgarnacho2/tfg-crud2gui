public class EjemploEtapa22 {
    public static void main(String[] args) {
        Pueblo pueblo = new Pueblo("Villanueva");
        pueblo.setPrecioTransporte(20);
        Cliente cliente = new Cliente("Juan López", pueblo);
        cliente.setDireccion("C/ Quijote 3");
        Pedido.setPrecioPorBolsa(7);
        Pedido.setPrecioPorKilo(0.75);
        Pedido pedido = new Pedido(cliente);
        pedido.setCantidad(100);
        pedido.setBolsasAstillas(2);
        pedido.imprimeInformacion();
    }
}
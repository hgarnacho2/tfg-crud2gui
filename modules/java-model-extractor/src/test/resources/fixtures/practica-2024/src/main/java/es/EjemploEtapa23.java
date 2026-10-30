public class EjemploEtapa23 {
    public static void main(String[] args) {
        Pueblo pueblo = new Pueblo("Villanueva");
        pueblo.setPrecioTransporte(20);
        Cliente cliente1 = new Cliente("Juan Lopez", pueblo);
        cliente1.setDireccion("C/ Quijote 3");
        Cliente cliente2 = new Cliente("Luis Ramirez", pueblo);
        cliente2.setDireccion("C/ Dulcinea 9");

        Pedido.setPrecioPorBolsa(7);
        Pedido.setPrecioPorKilo(0.75);
        Pedido pedido = new Pedido(cliente1);
        pedido.setCantidad(100);
        pedido.setBolsasAstillas(2);

        AlmacenLenya almacen = new AlmacenLenya();
        almacen.anyadirPueblo(pueblo);
        almacen.anyadirCliente(cliente1);
        almacen.anyadirCliente(cliente2);
        almacen.anyadirLenya(1000);
        almacen.anyadirPedido(pedido);
        almacen.anotarServido("Juan Lopez");
        almacen.anotarServido("Luis Ramirez");
    }
}
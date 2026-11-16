public class EjemploEtapa32 {
    public static void main(String[] args) {
        Pueblo p = new Pueblo("Villanueva");
        Pueblo otro = new Pueblo("Alcor");
        Cliente c1 = new Cliente("Juan Lopez", p);
        c1.setDireccion("C/ Quijote 3");
        Pedido.setPrecioPorKilo(0.75);
        Pedido.setPrecioPorBolsa(7);

        AlmacenLenyaV2 g = new AlmacenLenyaV2();
        g.anyadirPueblo(p);
        g.anyadirPueblo(new Pueblo("Villanueva"));   // repetido
        g.anyadirCliente(c1);
        g.anyadirLenya(150);

        Pedido a = new Pedido(c1);
        a.setCantidad(100);
        g.anyadirPedido(a);
        Pedido b = new Pedido(c1);                    // ya hay uno sin servir
        b.setCantidad(10);
        g.anyadirPedido(b);
        Pedido c = new Pedido(new Cliente("Ana", otro)); // pueblo no registrado
        c.setCantidad(10);
        g.anyadirPedido(c);

        g.imprimirLenyaPendientePorPueblo();
        g.anotarServido("Juan Lopez");
        System.out.println("Ganancias: " + g.calcularGanancias());
        System.out.println("Leña a pedir al proveedor: " + g.calcularLenyaParaProveedor());
    }
}

import java.util.ArrayList;

/**
 * Almacen de leña: gestiona pueblos, clientes, pedidos y el stock.
 */
public class AlmacenLenya {
    private ArrayList<Pueblo> pueblos;
    private ArrayList<Cliente> clientes;
    private ArrayList<Pedido> pedidos;
    private int cantidadDisponible;

    public AlmacenLenya() {
        pueblos = new ArrayList<Pueblo>();
        clientes = new ArrayList<Cliente>();
        pedidos = new ArrayList<Pedido>();
        cantidadDisponible = 0;
    }

    public ArrayList<Pueblo> getPueblos() { return pueblos; }
    public void setPueblos(ArrayList<Pueblo> pueblos) { this.pueblos = pueblos; }

    public ArrayList<Cliente> getClientes() { return clientes; }
    public void setClientes(ArrayList<Cliente> clientes) { this.clientes = clientes; }

    public ArrayList<Pedido> getPedidos() { return pedidos; }
    public void setPedidos(ArrayList<Pedido> pedidos) { this.pedidos = pedidos; }

    public int getCantidadDisponible() { return cantidadDisponible; }
    public void setCantidadDisponible(int cantidadDisponible) { this.cantidadDisponible = cantidadDisponible; }

    public void anyadirPueblo(Pueblo p) {
        pueblos.add(p);
        System.out.println("PUEBLO AÑADIDO");
        p.imprimeInformacion();
    }

    public void anyadirCliente(Cliente c) {
        clientes.add(c);
        System.out.println("CLIENTE AÑADIDO");
        c.imprimeInformacion();
    }

    /** Anade el pedido y resta su cantidad del stock. */
    public void anyadirPedido(Pedido p) {
        pedidos.add(p);
        cantidadDisponible -= p.getCantidad();
        System.out.println("PEDIDO AÑADIDO");
        p.imprimeInformacion();
    }

    public void anyadirLenya(int cantidad) {
        cantidadDisponible += cantidad;
        System.out.println("SE HA AÑADIDO " + cantidad + " KILOS DE LEÑA AL ALMACÉN");
        System.out.println("CANTIDAD ACTUAL: " + cantidadDisponible + " KILOS");
    }

    /** Marca como servidos todos los pedidos pendientes del cliente con ese nombre. */
    public void anotarServido(String nombreCliente) {
        boolean encontrado = false;
        for (Pedido p : pedidos) {
            if (!p.getServido() && p.getCliente().getNombre().equals(nombreCliente)) {
                p.setServido(true);
                System.out.println("PEDIDO SERVIDO:");
                p.imprimeInformacion();
                encontrado = true;
            }
        }
        if (!encontrado) {
            System.out.println("NO EXISTE NINGÚN PEDIDO POR SERVIR A NOMBRE DE: " + nombreCliente);
        }
    }
}

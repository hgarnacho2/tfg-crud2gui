import java.util.Calendar;
import java.util.Date;

/**
 * Etapa 3.2 (opcional): almacen con comprobaciones y calculos adicionales.
 */
public class AlmacenLenyaV2 extends AlmacenLenya {

    /** (1) No permite pueblos con nombre repetido. */
    @Override
    public void anyadirPueblo(Pueblo p) {
        for (Pueblo existente : getPueblos()) {
            if (existente.getNombre().equals(p.getNombre())) {
                System.out.println("YA EXISTE UN PUEBLO CON EL NOMBRE: " + p.getNombre());
                return;
            }
        }
        super.anyadirPueblo(p);
    }

    /** (3) No permite clientes con nombre repetido. */
    @Override
    public void anyadirCliente(Cliente c) {
        for (Cliente existente : getClientes()) {
            if (existente.getNombre().equals(c.getNombre())) {
                System.out.println("YA EXISTE UN CLIENTE CON EL NOMBRE: " + c.getNombre());
                return;
            }
        }
        super.anyadirCliente(c);
    }

    /** (2), (4) y (5): comprobaciones antes de anadir un pedido. */
    @Override
    public void anyadirPedido(Pedido p) {
        if (!getPueblos().contains(p.getCliente().getPueblo())) {
            System.out.println("EL PUEBLO DEL PEDIDO NO EXISTE EN EL SISTEMA. PEDIDO NO AÑADIDO");
            return;
        }
        for (Pedido existente : getPedidos()) {
            if (!existente.getServido()
                    && existente.getCliente().getNombre().equals(p.getCliente().getNombre())) {
                System.out.println("YA EXISTE UN PEDIDO NO SERVIDO PARA EL CLIENTE: "
                        + p.getCliente().getNombre());
                return;
            }
        }
        if (p.getCantidad() > getCantidadDisponible()) {
            System.out.println("CANTIDAD SOLICITADA (" + p.getCantidad()
                    + ") SUPERIOR A LA DISPONIBLE (" + getCantidadDisponible() + ")");
            return;
        }
        super.anyadirPedido(p);
    }

    /** (6) Suma de los precios de los pedidos servidos. */
    public float calcularGanancias() {
        float total = 0;
        for (Pedido p : getPedidos()) {
            if (p.getServido()) {
                total += p.calcularPrecio();
            }
        }
        return total;
    }

    /** (7) Imprime la leña pendiente de servir en cada pueblo. */
    public void imprimirLenyaPendientePorPueblo() {
        for (Pueblo pueblo : getPueblos()) {
            int total = 0;
            for (Pedido p : getPedidos()) {
                if (!p.getServido() && p.getCliente().getPueblo() == pueblo) {
                    total += p.getCantidad();
                }
            }
            System.out.println(pueblo.getNombre() + ": " + total + " kilos pendientes");
        }
    }

    /** (8) Leña servida en el ultimo mes (cantidad a pedir al proveedor). */
    public int calcularLenyaParaProveedor() {
        Calendar cal = Calendar.getInstance();
        Date ahora = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        Date haceUnMes = cal.getTime();
        int total = 0;
        for (Pedido p : getPedidos()) {
            Date f = p.getFecha();
            if (p.getServido() && !f.before(haceUnMes) && !f.after(ahora)) {
                total += p.getCantidad();
            }
        }
        return total;
    }
}

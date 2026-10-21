import java.util.Date;

/**
 * Pedido de leña (y opcionalmente bolsas de astillas) de un cliente.
 */
public class Pedido {
    private static double precioPorKilo;
    private static double precioPorBolsa;

    private Cliente cliente;
    private Date fecha;
    private int cantidad;
    private int bolsasAstillas;
    private boolean servido;

    public Pedido(Cliente cliente) {
        this.cliente = cliente;
        this.fecha = new Date();
        this.cantidad = 0;
        this.bolsasAstillas = 0;
        this.servido = false;
    }

    public static double getPrecioPorKilo() { return precioPorKilo; }

    public static void setPrecioPorKilo(double p) {
        precioPorKilo = p;
        System.out.println("Precio por kilo modificado a: " + p + " euros");
    }

    public static double getPrecioPorBolsa() { return precioPorBolsa; }

    public static void setPrecioPorBolsa(double p) {
        precioPorBolsa = p;
        System.out.println("Precio por bolsa de astillas modificado a: " + p + " euros");
    }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Date getFecha() { return fecha; }
    public void setFecha(Date fecha) { this.fecha = fecha; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public int getBolsasAstillas() { return bolsasAstillas; }
    public void setBolsasAstillas(int bolsasAstillas) { this.bolsasAstillas = bolsasAstillas; }

    public boolean getServido() { return servido; }
    public void setServido(boolean servido) { this.servido = servido; }

    /** Precio = kilos * precio/kilo + transporte + bolsas * precio/bolsa. */
    public float calcularPrecio() {
        double precio = cantidad * precioPorKilo
                + cliente.getPueblo().getPrecioTransporte()
                + bolsasAstillas * precioPorBolsa;
        return (float) precio;
    }

    /** Imprime toda la informacion del pedido. */
    public void imprimeInformacion() {
        System.out.println("INFORMACIÓN DE PEDIDO");
        System.out.println("Fecha: " + fecha);
        System.out.println("Cantidad: " + cantidad);
        System.out.println("Número de bolsas de astillas: " + bolsasAstillas);
        cliente.imprimeInformacion();
        System.out.println("Estado del pedido: " + (servido ? "Servido" : "No servido"));
        System.out.println("Precio total del pedido: " + calcularPrecio() + " euros");
    }
}

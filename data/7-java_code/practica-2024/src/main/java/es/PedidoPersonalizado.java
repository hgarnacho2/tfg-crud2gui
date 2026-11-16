/**
 * Pedido con proporcion de leña pequeña, mediana y grande y coste adicional.
 */
public class PedidoPersonalizado extends Pedido {
    private static double costeAdicional = 30;

    private int proporcionPequena;
    private int proporcionMediana;
    private int proporcionGrande;

    public PedidoPersonalizado(Cliente cliente) {
        super(cliente);
    }

    public static double getCosteAdicional() { return costeAdicional; }
    public static void setCosteAdicional(double c) { costeAdicional = c; }

    /** Fija las proporciones (en %) solo si suman 100. */
    public void setProporciones(int pequena, int mediana, int grande) {
        if (pequena + mediana + grande != 100) {
            System.out.println("ERROR: las proporciones deben sumar 100");
            return;
        }
        this.proporcionPequena = pequena;
        this.proporcionMediana = mediana;
        this.proporcionGrande = grande;
    }

    public int getProporcionPequena() { return proporcionPequena; }
    public int getProporcionMediana() { return proporcionMediana; }
    public int getProporcionGrande() { return proporcionGrande; }

    @Override
    public float calcularPrecio() {
        return super.calcularPrecio() + (float) costeAdicional;
    }

    @Override
    public void imprimeInformacion() {
        super.imprimeInformacion();
        System.out.println("Proporción de leña pequeña: " + proporcionPequena + " %");
        System.out.println("Proporción de leña mediana: " + proporcionMediana + " %");
        System.out.println("Proporción de leña grande: " + proporcionGrande + " %");
        System.out.println("Coste adicional por pedido personalizado: " + costeAdicional + " euros");
    }
}

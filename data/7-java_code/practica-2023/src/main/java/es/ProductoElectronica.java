/** Producto electrónico con peso y dimensiones físicas. */
public class ProductoElectronica extends Producto {
    private double peso;
    private double alto;
    private double largo;
    private double ancho;

    public ProductoElectronica(int id, String titulo, double precio, String descripcion,
                               T_Estado estado, double peso, double alto, double largo, double ancho) {
        super(id, titulo, precio, descripcion, estado);
        this.peso = peso;
        this.alto = alto;
        this.largo = largo;
        this.ancho = ancho;
    }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }
    public double getAlto() { return alto; }
    public void setAlto(double alto) { this.alto = alto; }
    public double getLargo() { return largo; }
    public void setLargo(double largo) { this.largo = largo; }
    public double getAncho() { return ancho; }
    public void setAncho(double ancho) { this.ancho = ancho; }

    @Override
    protected void printCaracteristicas() {
        System.out.println("Peso: " + peso + " kg");
        System.out.println("Dimensiones (Alto x Largo x Ancho): " +
                alto + " cm x " + largo + " cm x " + ancho + " cm");
    }
}

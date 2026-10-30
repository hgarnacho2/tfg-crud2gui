public class AlojamientoRural extends Alojamiento {
    private double superficieParcela;
    private boolean piscina;

    public AlojamientoRural(int id, String titulo, double precioNoche, String descripcion, T_Estado estado, double superficieParcela, boolean piscina) {
        super(id, titulo, precioNoche, descripcion, estado);
        this.superficieParcela = superficieParcela;
        this.piscina = piscina;
    }

    public double getSuperficieParcela() { return superficieParcela; }
    public void setSuperficieParcela(double superficieParcela) { this.superficieParcela = superficieParcela; }
    public boolean isPiscina() { return piscina; }
    public void setPiscina(boolean piscina) { this.piscina = piscina; }

    @Override
    public void print() {
        super.print();
        System.out.println("Superficie de parcela: " + superficieParcela + " m2");
        System.out.println("Piscina: " + (piscina ? "Si" : "No"));
    }
}

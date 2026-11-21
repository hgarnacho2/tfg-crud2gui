public class Gato extends Animal {
    private int edadMeses;
    private boolean esterilizado;

    public Gato(int id, String titulo, double tasaAdopcion, String descripcion, T_Estado estado, int edadMeses, boolean esterilizado) {
        super(id, titulo, tasaAdopcion, descripcion, estado);
        this.edadMeses = edadMeses;
        this.esterilizado = esterilizado;
    }

    public int getEdadMeses() { return edadMeses; }
    public void setEdadMeses(int edadMeses) { this.edadMeses = edadMeses; }
    public boolean isEsterilizado() { return esterilizado; }
    public void setEsterilizado(boolean esterilizado) { this.esterilizado = esterilizado; }

    @Override
    public void print() {
        super.print();
        System.out.println("Edad: " + edadMeses + " meses");
        System.out.println("Esterilizado: " + (esterilizado ? "Si" : "No"));
    }
}

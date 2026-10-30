public class Perro extends Animal {
    private String raza;
    private double pesoKg;
    private boolean adiestrado;

    public Perro(int id, String titulo, double tasaAdopcion, String descripcion, T_Estado estado, String raza, double pesoKg, boolean adiestrado) {
        super(id, titulo, tasaAdopcion, descripcion, estado);
        this.raza = raza;
        this.pesoKg = pesoKg;
        this.adiestrado = adiestrado;
    }

    public String getRaza() { return raza; }
    public void setRaza(String raza) { this.raza = raza; }
    public double getPesoKg() { return pesoKg; }
    public void setPesoKg(double pesoKg) { this.pesoKg = pesoKg; }
    public boolean isAdiestrado() { return adiestrado; }
    public void setAdiestrado(boolean adiestrado) { this.adiestrado = adiestrado; }

    @Override
    public void print() {
        super.print();
        System.out.println("Raza: " + raza);
        System.out.println("Peso: " + pesoKg + " kg");
        System.out.println("Adiestrado: " + (adiestrado ? "Si" : "No"));
    }
}

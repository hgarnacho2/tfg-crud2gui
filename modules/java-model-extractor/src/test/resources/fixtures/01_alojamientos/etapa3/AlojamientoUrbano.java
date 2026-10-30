public class AlojamientoUrbano extends Alojamiento {
    private int numHabitaciones;
    private double superficie;
    private int planta;

    public AlojamientoUrbano(int id, String titulo, double precioNoche, String descripcion, T_Estado estado, int numHabitaciones, double superficie, int planta) {
        super(id, titulo, precioNoche, descripcion, estado);
        this.numHabitaciones = numHabitaciones;
        this.superficie = superficie;
        this.planta = planta;
    }

    public int getNumHabitaciones() { return numHabitaciones; }
    public void setNumHabitaciones(int numHabitaciones) { this.numHabitaciones = numHabitaciones; }
    public double getSuperficie() { return superficie; }
    public void setSuperficie(double superficie) { this.superficie = superficie; }
    public int getPlanta() { return planta; }
    public void setPlanta(int planta) { this.planta = planta; }

    @Override
    public void print() {
        super.print();
        System.out.println("Habitaciones: " + numHabitaciones);
        System.out.println("Superficie: " + superficie + " m2");
        System.out.println("Planta: " + planta);
    }
}

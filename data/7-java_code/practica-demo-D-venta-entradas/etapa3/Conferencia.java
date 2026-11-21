public class Conferencia extends Evento {
    private String ponente;
    private double duracionHoras;
    private boolean online;

    public Conferencia(int id, String titulo, double precioEntrada, String descripcion, T_Estado estado, String ponente, double duracionHoras, boolean online) {
        super(id, titulo, precioEntrada, descripcion, estado);
        this.ponente = ponente;
        this.duracionHoras = duracionHoras;
        this.online = online;
    }

    public String getPonente() { return ponente; }
    public void setPonente(String ponente) { this.ponente = ponente; }
    public double getDuracionHoras() { return duracionHoras; }
    public void setDuracionHoras(double duracionHoras) { this.duracionHoras = duracionHoras; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    @Override
    public void print() {
        super.print();
        System.out.println("Ponente: " + ponente);
        System.out.println("Duracion: " + duracionHoras + " h");
        System.out.println("Online: " + (online ? "Si" : "No"));
    }
}

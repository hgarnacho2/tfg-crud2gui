public class Concierto extends Evento {
    private String artista;
    private int aforo;

    public Concierto(int id, String titulo, double precioEntrada, String descripcion, T_Estado estado, String artista, int aforo) {
        super(id, titulo, precioEntrada, descripcion, estado);
        this.artista = artista;
        this.aforo = aforo;
    }

    public String getArtista() { return artista; }
    public void setArtista(String artista) { this.artista = artista; }
    public int getAforo() { return aforo; }
    public void setAforo(int aforo) { this.aforo = aforo; }

    @Override
    public void print() {
        super.print();
        System.out.println("Artista: " + artista);
        System.out.println("Aforo: " + aforo + " personas");
    }
}

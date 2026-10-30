public class CursoEnDirecto extends Curso {
    private int numSesiones;
    private int plazas;

    public CursoEnDirecto(int id, String titulo, double precioMatricula, String descripcion, T_Estado estado, int numSesiones, int plazas) {
        super(id, titulo, precioMatricula, descripcion, estado);
        this.numSesiones = numSesiones;
        this.plazas = plazas;
    }

    public int getNumSesiones() { return numSesiones; }
    public void setNumSesiones(int numSesiones) { this.numSesiones = numSesiones; }
    public int getPlazas() { return plazas; }
    public void setPlazas(int plazas) { this.plazas = plazas; }

    @Override
    public void print() {
        super.print();
        System.out.println("Sesiones: " + numSesiones);
        System.out.println("Plazas: " + plazas);
    }
}

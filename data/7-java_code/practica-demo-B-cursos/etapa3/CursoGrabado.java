public class CursoGrabado extends Curso {
    private double horasVideo;
    private int numLecciones;
    private boolean certificado;

    public CursoGrabado(int id, String titulo, double precioMatricula, String descripcion, T_Estado estado, double horasVideo, int numLecciones, boolean certificado) {
        super(id, titulo, precioMatricula, descripcion, estado);
        this.horasVideo = horasVideo;
        this.numLecciones = numLecciones;
        this.certificado = certificado;
    }

    public double getHorasVideo() { return horasVideo; }
    public void setHorasVideo(double horasVideo) { this.horasVideo = horasVideo; }
    public int getNumLecciones() { return numLecciones; }
    public void setNumLecciones(int numLecciones) { this.numLecciones = numLecciones; }
    public boolean isCertificado() { return certificado; }
    public void setCertificado(boolean certificado) { this.certificado = certificado; }

    @Override
    public void print() {
        super.print();
        System.out.println("Horas de video: " + horasVideo + " h");
        System.out.println("Lecciones: " + numLecciones);
        System.out.println("Certificado: " + (certificado ? "Si" : "No"));
    }
}

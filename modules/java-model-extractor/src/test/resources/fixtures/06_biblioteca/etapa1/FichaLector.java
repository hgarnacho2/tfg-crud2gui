import java.util.Date;

public class FichaLector {
    private Date fechaDeNacimiento;
    private String generosFavoritos;
    private String profesion;
    private String libroFavorito;

    public Date getFechaDeNacimiento() { return fechaDeNacimiento; }
    public void setFechaDeNacimiento(Date fechaDeNacimiento) { this.fechaDeNacimiento = fechaDeNacimiento; }
    public String getGenerosFavoritos() { return generosFavoritos; }
    public void setGenerosFavoritos(String generosFavoritos) { this.generosFavoritos = generosFavoritos; }
    public String getProfesion() { return profesion; }
    public void setProfesion(String profesion) { this.profesion = profesion; }
    public String getLibroFavorito() { return libroFavorito; }
    public void setLibroFavorito(String libroFavorito) { this.libroFavorito = libroFavorito; }

    public void imprimeInformacion() {
        System.out.println("          Fecha de nacimiento: " + fechaDeNacimiento);
        System.out.println("          Géneros favoritos: " + generosFavoritos);
        System.out.println("          Profesión: " + profesion);
        System.out.println("          Libro favorito: " + libroFavorito);
    }
}

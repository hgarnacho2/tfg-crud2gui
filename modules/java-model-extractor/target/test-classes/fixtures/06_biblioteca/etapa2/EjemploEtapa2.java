import java.util.Date;

public class EjemploEtapa2 {
    public static void main(String[] args) {
        Lector p1=new Lector("Inés Carrasco Bravo","libros2025");
        FichaLector f=new FichaLector();
        Date fecha = new Date(79, 11, 3);
        f.setFechaDeNacimiento(fecha);
        f.setGenerosFavoritos("Novela histórica");
        f.setProfesion("Bibliotecaria");
        f.setLibroFavorito("El nombre de la rosa");
        p1.setFicha(f);
        Lector p2=new Lector("Tomás Ibáñez Roca","lectura77");
        FichaLector f2=new FichaLector();
        Date fecha2 = new Date(91, 1, 14);
        f2.setFechaDeNacimiento(fecha2);
        f2.setGenerosFavoritos("Ensayo y poesía");
        f2.setProfesion("Profesor universitario");
        f2.setLibroFavorito("Rayuela");
        p2.setFicha(f2);
        MiBiblioteca r=new MiBiblioteca();
        r.anadirLector(p1);
        r.anadirLector(p2);
        r.visualizarBiblioteca();
        r.visualizarLector("lector56");
        r.visualizarLector("lector1");
        boolean comprobacion1 = r.comprobarPassword("lector56","libros");
        boolean comprobacion2 = r.comprobarPassword("lector1","libros");
        boolean comprobacion3 = r.comprobarPassword("lector1","libros2025");
        r.anadirResena("lector56","Una lectura imprescindible");
        r.anadirResena("lector1","Una lectura imprescindible");
        r.anadirResena("lector1","Final algo decepcionante");
        r.visualizarLector("lector1");
    }
}

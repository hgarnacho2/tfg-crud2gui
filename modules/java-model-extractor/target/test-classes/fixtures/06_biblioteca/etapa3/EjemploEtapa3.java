import java.util.Date;

public class EjemploEtapa3 {
    public static void main(String[] args) {
        Lector p1=new Lector("Inés Carrasco Bravo","libros2025");
        FichaLector f=new FichaLector();
        Date fecha = new Date(79, 11, 3);
        f.setFechaDeNacimiento(fecha);
        f.setGenerosFavoritos("Novela histórica");
        f.setProfesion("Bibliotecaria");
        f.setLibroFavorito("El nombre de la rosa");
        p1.setFicha(f);
        LectorInvestigador p2=new LectorInvestigador("Tomás Ibáñez Roca","lectura77");
        FichaLector f2=new FichaLector();
        Date fecha2 = new Date(91, 1, 14);
        f2.setFechaDeNacimiento(fecha2);
        f2.setGenerosFavoritos("Ensayo y poesía");
        f2.setProfesion("Profesor universitario");
        f2.setLibroFavorito("Rayuela");
        p2.setFicha(f2);
        p2.setLineaInvestigacion("Literatura comparada del siglo XX");
        p2.setCentro("Instituto de Estudios Literarios");
        MiBiblioteca r=new MiBiblioteca();
        r.anadirLector(p1);
        r.anadirLector(p2);
        r.visualizarBiblioteca();
        r.visualizarLector("lector2");
        boolean comprobacion1 = r.comprobarPassword("lector56","libros");
        boolean comprobacion2 = r.comprobarPassword("lector2","lect");
        boolean comprobacion3 = r.comprobarPassword("lector2","lectura77");
        r.anadirResena("lector2","Reseña muy recomendable");
        r.anadirResena("lector2","Personajes inolvidables");
        r.visualizarLector("lector2");
    }
}

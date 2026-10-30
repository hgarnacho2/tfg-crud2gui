public class EjemploEtapa4 {
    public static void main(String[] args) {
        MiBiblioteca r = new MiBiblioteca();
        Lector s1 = new Lector("Inés Carrasco Bravo","clave1");
        Lector s2 = new Lector("Tomás Ibáñez Roca","clave2");
        Lector s3 = new Lector("Nombre Prueba Tres","clave3");
        r.anadirLector(s1);
        r.anadirLector(s2);
        r.anadirLector(s3);
        r.visualizarBiblioteca();
        r.eliminarLector("lector2");
        r.eliminarLector("lector2");
        r.visualizarBiblioteca();
        Lector s4 = new Lector("Nombre Prueba Cuatro","clave4");
        r.anadirLector(s4);
        r.visualizarBiblioteca();
        r.anadirResena("lector1","Una lectura imprescindible");
        r.anadirResena("lector1","Final algo decepcionante");
        r.anadirResena("lector1","Reseña muy recomendable");
        r.eliminarResena("lector1","https://www.miBiblioteca.com/resena/lector1.2");
        r.eliminarResena("lector1","https://www.miBiblioteca.com/resena/lector1.2");
        r.anadirResena("lector1","Personajes inolvidables");
        r.eliminarResena("lector56","https://www.miBiblioteca.com/resena/lector1.1");
        r.visualizarLector("lector1");
    }
}

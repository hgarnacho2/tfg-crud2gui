public class LanzadorEtapa1 {
    public static void main(String[] args) {
        Refugio refugio1 = new Refugio("Refugio Huellas del Sur","refugio.huellas@correo.es");
        Refugio refugio2 = new Refugio("Protectora Amigos Peludos","amigospeludos@mimail.com");
        Refugio refugio3 = new Refugio("Asociación Segunda Oportunidad","segundaoportunidad@otromail.com");
        refugio1.print();
        refugio2.print();
        refugio3.print();
    }
}

public class LanzadorEtapa2_1 {
    public static void main(String[] args) {
        PlataformaCompraVenta gestor = new PlataformaCompraVenta("Wallarock");
        Usuario user1 = gestor.crearUsuario("Mariano Pérez Oso", "marian0@hismail.com");
        Usuario user2 = gestor.crearUsuario("Javier Carro Ñero", "javicarronyero@othermail.com");
        Usuario user3 = gestor.crearUsuario("Antoine Val Ondeoro", "antoine@hismail.com");
        Usuario user4 = gestor.crearUsuario("Martín Antonio García", "mantoniog@othermail.com");

        Producto producto1 = user1.crearProductoElectronica(1, "Cámara digital Samsung", 85,
                "Cámara digital Samsung full HD, 11,9 megapíxeles + wifi.", 6.5, 11.01, 2.15, 0.6);
        Producto producto2 = user2.crearProductoRopa(2, "Cazadora vaquera Levis Hombre", 170.95,
                "Sin estrenar. Talla M", "M");
        Producto producto3 = user3.crearProductoRopa(3, "Cortavientos ADIDAS", 250,
                "Color negro. Talla S. Nuevo con etiqueta.", "S");
        Producto producto4 = user3.crearProductoElectronica(4, "Mini video cámara digital deportiva", 20,
                "Video cámara deportiva Unotec Gartner.", 6.5, 11.01, 2.15, 4.25);
        Producto producto5 = user3.crearProductoElectronica(5, "Cámara foto digital", 40,
                "No hago envíos; el resto de la cámara está como nueva.", 1.0, 8.0, 5.0, 3.0);
        Producto producto6 = user4.crearProductoElectronica(6, "Cámara Sony Alpha 6400", 649.49,
                "Cámara digital con objetivo zoom y dos baterías.", 1.0, 12.0, 10.0, 8.0);

        producto2.print();
        user3.print();
        gestor.removeUser(user3.getEmail());
        gestor.printAllUsers();
        gestor.printAllProducts();
    }
}
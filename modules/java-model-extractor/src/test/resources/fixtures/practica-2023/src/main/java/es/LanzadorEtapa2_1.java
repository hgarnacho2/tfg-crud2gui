public class LanzadorEtapa2_1 {
    public static void main(String[] args) {
        PlataformaCompraVenta gestor = new PlataformaCompraVenta("Wallarock");
        Usuario user1 = new Usuario("Mariano Pérez Oso", "marian0@hismail.com");
        Usuario user2 = new Usuario("Javier Carro Ñero", "javicarronyero@othermail.com");
        Usuario user3 = new Usuario("Antoine Val Ondeoro", "antoine@hismail.com");
        Usuario user4 = new Usuario("Martín Antonio García", "mantoniog@othermail.com");
        gestor.addUser(user1);
        gestor.addUser(user2);
        gestor.addUser(user3);
        gestor.addUser(user4);

        Producto producto1 = new ProductoElectronica(1, "Cámara digital Samsung", 85,
                "Cámara digital Samsung full HD, 11,9 megapíxeles + wifi.",
                T_Estado.DISPONIBLE, 6.5, 11.01, 2.15, 0.6);
        Producto producto2 = new ProductoRopa(2, "Cazadora vaquera Levis Hombre", 170.95,
                "Sin estrenar. Talla M", T_Estado.DISPONIBLE, "M");
        Producto producto3 = new ProductoRopa(3, "Cortavientos ADIDAS", 250,
                "Color negro. Talla S. Nuevo con etiqueta.", T_Estado.DISPONIBLE, "S");
        Producto producto4 = new ProductoElectronica(4, "Mini video cámara digital deportiva", 20,
                "Video cámara deportiva Unotec Gartner.",
                T_Estado.DISPONIBLE, 6.5, 11.01, 2.15, 4.25);
        Producto producto5 = new ProductoElectronica(5, "Cámara foto digital", 40,
                "No hago envíos; el resto de la cámara está como nueva.",
                T_Estado.DISPONIBLE, 1.0, 8.0, 5.0, 3.0);
        Producto producto6 = new ProductoElectronica(6, "Cámara Sony Alpha 6400", 649.49,
                "Cámara digital con objetivo zoom y dos baterías.",
                T_Estado.DISPONIBLE, 1.0, 12.0, 10.0, 8.0);

        gestor.addProduct2User(producto1, user1.getEmail());
        gestor.addProduct2User(producto2, user2.getEmail());
        gestor.addProduct2User(producto3, user3.getEmail());
        gestor.addProduct2User(producto4, user3.getEmail());
        gestor.addProduct2User(producto5, user3.getEmail());
        gestor.addProduct2User(producto6, user4.getEmail());

        producto2.print();
        user3.print();
        gestor.removeUser(user3.getEmail());
        gestor.printAllUsers();
        gestor.printAllProducts();
    }
}
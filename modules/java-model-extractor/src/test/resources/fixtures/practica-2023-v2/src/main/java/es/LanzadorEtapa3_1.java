/** Programa de prueba compatible con el ejemplo de la etapa 3 del enunciado. */
public class LanzadorEtapa3_1 {
    public static void main(String[] args) {
        PlataformaCompraVenta gestor = new PlataformaCompraVenta("Wallarock");
        Usuario user1 = gestor.crearUsuario("Mariano Pérez Oso", "marian0@hismail.com");
        Usuario user2 = gestor.crearUsuario("Javier Carro Ñero", "javicarronyero@othermail.com");

        Producto producto1 = user1.crearProductoElectronica(1, "Cámara digital Samsung", 85,
                "Cámara de vídeo digital Samsung (wb800f) full HD 11,9 megapíxeles + wifi.",
                6.5, 11.01, 2.15, 0.6);
        Producto producto2 = user2.crearProductoRopa(2, "Cazadora vaquera Levis Hombre", 170.95,
                "Sin estrenar. Talla M", "M");
        Producto producto3 = user2.crearProductoRopa(3, "Cortavientos/chubasquero ADIDAS", 250,
                "Color negro. Cremallera lateral de cuello a hombro. Talla S. Nuevo con etiqueta.",
                "S");
        Producto producto4 = user1.crearProductoElectronica(4, "Mini video cámara digital deportiva", 20,
                "video cámara unotec gartner", 6.5, 11.01, 2.15, 4.25);

        gestor.removeProduct2User(1, user1.getEmail());
        gestor.printProductsByUser(user1.getEmail());
        gestor.printAllProducts();

        producto2.setEstado(T_Estado.RESERVADO);
        gestor.printProductsByState(T_Estado.DISPONIBLE);
        gestor.printProductsByState(T_Estado.RESERVADO);
        gestor.printProductsByUser(user1.getEmail(), T_Estado.DISPONIBLE);
    }
}

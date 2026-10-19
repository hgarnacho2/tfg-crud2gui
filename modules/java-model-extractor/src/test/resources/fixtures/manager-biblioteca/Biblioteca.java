package fixtures.biblioteca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Biblioteca {
    private final List<Material> materiales = new ArrayList<>();
    private final Set<Socio> socios = new HashSet<>();
    private final Map<Integer, Prestamo> prestamos = new HashMap<>();

    void registrar(Material material) { materiales.add(material); }
    void registrar(Socio socio) { socios.add(socio); }
    Prestamo buscarPrestamo(int id) { return prestamos.get(id); }
}

abstract class Material { }
class Libro extends Material { }
class Revista extends Material { }
class Socio { }
class Prestamo { }
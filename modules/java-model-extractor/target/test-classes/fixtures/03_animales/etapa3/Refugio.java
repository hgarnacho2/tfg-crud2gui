import java.util.ArrayList;

public class Refugio {
    private String nombre;
    private String email;
    private ArrayList<Animal> animales;

    public Refugio(String nombre, String email) {
        this.nombre = nombre;
        this.email = email;
        this.animales = new ArrayList<Animal>();
        System.out.println("Creado refugio " + email);
    }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public ArrayList<Animal> getAnimales() { return animales; }

    /** Añade el animal a la lista y lo deja en estado DISPONIBLE. */
    public void addAnimal(Animal x) {
        x.setEstado(T_Estado.DISPONIBLE);
        animales.add(x);
    }

    /** Elimina de la lista el animal con el id indicado. Devuelve true si existía. */
    public boolean removeAnimal(int id) {
        for (int k = 0; k < animales.size(); k++) {
            if (animales.get(k).getId() == id) {
                animales.remove(k);
                return true;
            }
        }
        return false;
    }

    public void print() {
        System.out.println("------------");
        System.out.println("Nombre: " + nombre);
        System.out.println("Email: " + email);
        System.out.println("------------");
        for (Animal x : animales) {
            x.print();
        }
    }
}

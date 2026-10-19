package com.crud2gui.javamodelextractor;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.crud2gui.model.*;
import com.crud2gui.javamodelextractor.ManagerDetectorConfig;
import com.crud2gui.javamodelextractor.ProjectParser;

/**
 * Pruebas del analizador sobre tres proyectos de ejemplo en {@code src/test/resources/fixtures}:
 * {@code tienda} (abstractas, enums, interfaces, genericos, clases internas y records),
 * {@code redsocial} (la practica de la asignatura, en paquete por defecto) y {@code roto}
 * (un fichero con errores de sintaxis).
 */
class ProjectParserTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");
    private static ProjectModel tienda;
    private static ProjectModel redSocial;

    /** Analiza los proyectos de ejemplo una sola vez para todas las pruebas. */
    @BeforeAll
    static void parseFixtures() throws Exception {
        tienda = new ProjectParser().parse(FIXTURES.resolve("tienda"));
        redSocial = new ProjectParser().parse(FIXTURES.resolve("redsocial"));
    }

    /** Busca un tipo por id; falla la prueba si no existe. */
    private static TypeInfo type(ProjectModel m, String id) {
        return m.types().stream().filter(t -> t.id().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("No existe el tipo " + id));
    }

    /** Busca un atributo por nombre; falla la prueba si no existe. */
    private static FieldInfo field(TypeInfo t, String name) {
        return t.fields().stream().filter(f -> f.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No existe el campo " + name));
    }

    /** Busca un metodo por nombre; falla la prueba si no existe. */
    private static MethodInfo method(TypeInfo t, String name) {
        return t.methods().stream().filter(m -> m.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No existe el metodo " + name));
    }

    /** Se encuentran los 9 tipos (incluidos los internos) y se ignora la carpeta target. */
    @Test
    void encuentraTodosLosTiposEIgnoraTarget() {
        List<String> ids = tienda.types().stream().map(TypeInfo::id).collect(Collectors.toList());
        assertEquals(9, ids.size(), ids.toString());
        assertTrue(ids.contains("com.tienda.Tienda.Linea"));
        assertTrue(ids.contains("com.tienda.Tienda.Resumen"));
        assertFalse(ids.contains("com.tienda.Ignorado"));
    }

    /** Clase abstracta: modificadores, javadoc de tipo y de constructor, y metodo abstracto. */
    @Test
    void claseAbstractaConJavadoc() {
        TypeInfo p = type(tienda, "com.tienda.Producto");
        assertEquals(TypeKind.CLASS, p.kind());
        assertTrue(p.modifiers().contains(Modifier.ABSTRACT));
        assertEquals("Producto generico de la tienda.", p.javadoc().description());
        assertTrue(p.javadoc().tags().containsKey("author"));
        assertEquals("src/main/java/com/tienda/Producto.java", p.location().file());

        assertEquals(2, p.constructors().get(0).parameters().size());
        assertEquals(2, p.constructors().get(0).javadoc().tags().get("param").size());

        MethodInfo m = method(p, "printCaracteristicas");
        assertEquals(Visibility.PROTECTED, m.visibility());
        assertTrue(m.modifiers().contains(Modifier.ABSTRACT));
        assertFalse(m.hasBody());
    }

    /** Un atributo cuyo tipo esta en el proyecto se resuelve a su nombre completo. */
    @Test
    void tipoDelProyectoResueltoEnCampo() {
        FieldInfo estado = field(type(tienda, "com.tienda.Producto"), "estado");
        assertTrue(estado.type().projectType());
        assertEquals("com.tienda.T_Estado", estado.type().qualifiedName());
    }

    /** Un enum conserva sus constantes. */
    @Test
    void enumConstantes() {
        TypeInfo e = type(tienda, "com.tienda.T_Estado");
        assertEquals(TypeKind.ENUM, e.kind());
        assertEquals(3, e.enumConstants().size());
    }

    /** Herencia, realizacion de interfaz y reglas implicitas de los miembros de una interfaz. */
    @Test
    void herenciaEInterfaz() {
        TypeInfo r = type(tienda, "com.tienda.ProductoRopa");
        assertEquals("com.tienda.Producto", r.superclass().qualifiedName());
        assertEquals("com.tienda.Imprimible", r.interfaces().get(0).qualifiedName());

        TypeInfo i = type(tienda, "com.tienda.Imprimible");
        assertEquals(TypeKind.INTERFACE, i.kind());
        MethodInfo imprimir = method(i, "imprimir");
        assertEquals(Visibility.PUBLIC, imprimir.visibility());
        assertTrue(imprimir.modifiers().contains(Modifier.ABSTRACT));
        assertTrue(method(i, "etiqueta").modifiers().contains(Modifier.DEFAULT));
    }

    /** Genericos, mapas y arrays se representan con sus argumentos y dimensiones. */
    @Test
    void genericosYArrays() {
        TypeInfo u = type(tienda, "com.tienda.Usuario");
        FieldInfo productos = field(u, "productos");
        assertEquals("List<Producto>", productos.type().display());
        assertTrue(productos.type().arguments().get(0).projectType());
        assertFalse(productos.type().projectType());
        assertTrue(productos.initializedInline());

        assertEquals("Map<String, Producto>", field(u, "indice").type().display());
        FieldInfo destacados = field(u, "destacados");
        assertEquals("Producto[]", destacados.type().display());
        assertEquals(1, destacados.type().arrayDimensions());
    }

    /** Las clases internas conservan su contenedor y los records exponen sus componentes. */
    @Test
    void tiposInternosYRecords() {
        TypeInfo linea = type(tienda, "com.tienda.Tienda.Linea");
        assertEquals("com.tienda.Tienda", linea.enclosingType());
        assertTrue(linea.modifiers().contains(Modifier.STATIC));

        TypeInfo resumen = type(tienda, "com.tienda.Tienda.Resumen");
        assertEquals(TypeKind.RECORD, resumen.kind());
        assertEquals(2, resumen.fields().size());
    }

    /** Se distingue el lanzador (con main) del resto de clases. */
    @Test
    void detectaMain() {
        assertTrue(type(tienda, "com.tienda.Lanzador").hasMainMethod());
        assertFalse(type(tienda, "com.tienda.Tienda").hasMainMethod());
    }

    /** Un fichero con errores de sintaxis genera un aviso y no impide analizar los demas. */
    @Test
    void ficherosConErroresGeneranAvisoSinAbortar() throws Exception {
        ProjectModel m = new ProjectParser().parse(FIXTURES.resolve("roto"));
        assertFalse(m.source().warnings().isEmpty());
        assertNotNull(type(m, "Bueno"));
    }

    /** La practica de la asignatura, en paquete por defecto, se analiza correctamente. */
    @Test
    void paquetePorDefectoYPracticaRedSocial() {
        TypeInfo profesional = type(redSocial, "PerfilProfesional");
        assertEquals("Perfil", profesional.superclass().qualifiedName());

        TypeInfo perfil = type(redSocial, "Perfil");
        FieldInfo posts = field(perfil, "posts");
        assertEquals("ArrayList<Post>", posts.type().display());
        assertTrue(posts.type().arguments().get(0).projectType());
        assertEquals("Biografia", field(perfil, "biografia").type().qualifiedName());

        TypeInfo bio = type(redSocial, "Biografia");
        assertFalse(field(bio, "fechaDeNacimiento").type().projectType()); // java.util.Date

        assertNotNull(method(type(redSocial, "MiRed"), "anadirPerfil").javadoc());
        assertTrue(type(redSocial, "EjemploEtapa3").hasMainMethod());
    }

    /** El parser clasifica relaciones UML usando herencia, atributos y evidencia del codigo. */
    @Test
    void calculaRelacionesUmlConEvidencias() throws Exception {
        ProjectModel demo = new ProjectParser().parse(FIXTURES.resolve("demo"));

        Relation inheritance = relation(demo, "com.demo.ClaseHijo1", "com.demo.ClasePadre1", null);
        assertEquals(RelationKind.INHERITANCE, inheritance.kind());

        Relation realization = relation(tienda, "com.tienda.ProductoRopa", "com.tienda.Imprimible", null);
        assertEquals(RelationKind.REALIZATION, realization.kind());

        Relation aggregation = relation(demo, "com.demo.ClaseGestora", "com.demo.ClasePadre1", "objetoMiembro");
        assertEquals(RelationKind.AGGREGATION, aggregation.kind());
        assertTrue(aggregation.evidence().assignedFromParameter());

        Relation composition = relation(demo, "com.demo.ClaseRelacion2", "com.demo.ClaseHijo1", "objetoCompuesto");
        assertEquals(RelationKind.COMPOSITION, composition.kind());
        assertTrue(composition.evidence().createdWithNew());
        assertEquals(Confidence.MEDIUM, composition.confidence());

        Relation collection = relation(demo, "com.demo.ClaseGestora", "com.demo.ClaseRelacionN", "relaciones");
        assertEquals(RelationKind.AGGREGATION, collection.kind());
        assertEquals("0..*", collection.multiplicity());

        Relation dependency = demo.relations().stream()
                .filter(r -> r.from().equals("com.demo.ClaseRelacion4"))
                .filter(r -> r.to().equals("com.demo.ClaseHijo1"))
                .filter(r -> r.kind() == RelationKind.DEPENDENCY)
                .findFirst().orElseThrow();
        assertEquals("parameter", dependency.evidence().usedIn());

        Relation post = relation(redSocial, "Perfil", "Post", "posts");
        assertEquals(RelationKind.COMPOSITION, post.kind());
        assertTrue(post.evidence().createdWithNew());

        Relation association = relation(tienda, "com.tienda.Usuario", "com.tienda.Producto", "destacados");
        assertEquals(RelationKind.ASSOCIATION, association.kind());
        assertEquals("0..*", association.multiplicity());
    }

    @Test
    void enumObligatorioEnConstructorEsAsociacionDeMultiplicidadUno() throws Exception {
        ProjectModel practice = parse("practica-2023");

        Relation state = relation(practice, "Producto", "T_Estado", "estado");

        assertEquals(RelationKind.ASSOCIATION, state.kind());
        assertEquals("1", state.multiplicity());
    }

    /** El detector generaliza a colecciones variadas, jerarquias, ciclos y gestores sin colecciones. */
    @Test
    void detectaGestorasEnProyectosEstructuralmenteDistintos() throws Exception {
        assertManager("manager-biblioteca", "fixtures.biblioteca.Biblioteca");
        assertManager("manager-hospital", "fixtures.hospital.Hospital");

        ProjectModel university = parse("manager-universidad");
        assertEquals("fixtures.universidad.University", university.analysis().managerClass().id());
        assertTrue(university.analysis().subManagers().contains("fixtures.universidad.Faculty"));

        assertManager("manager-game", "fixtures.game.Game");
        ProjectModel game = parse("manager-game");
        assertTrue(game.analysis().entryPoints().contains("fixtures.game.GameLauncher"));
        assertFalse(game.analysis().alternatives().stream()
            .anyMatch(candidate -> candidate.id().equals("fixtures.game.GameLauncher")));

        ProjectModel portfolio = parse("manager-portfolio");
        assertEquals("fixtures.portfolio.Portfolio", portfolio.analysis().managerClass().id());
        assertTrue(portfolio.analysis().subManagers().contains("fixtures.portfolio.Project"));
        assertFalse(portfolio.analysis().subManagers().contains("fixtures.portfolio.Team"));
    }

    /** Los ejemplos conocidos siguen detectandose y un override manual sustituye la heuristica. */
    @Test
    void managerRegressionYOverrideManual() throws Exception {
        ProjectModel demo = parse("demo");
        assertEquals("com.demo.ClaseGestora", demo.analysis().managerClass().id());
        assertEquals("MiRed", redSocial.analysis().managerClass().id());
        assertTrue(demo.analysis().alternatives().stream()
            .filter(candidate -> candidate.score() == 8.428571428571429)
            .allMatch(candidate -> candidate.confidence() == Confidence.LOW));

        ProjectModel forced = new ProjectParser().parse(FIXTURES.resolve("demo"), "com.demo.ClasePadre1");
        assertEquals("com.demo.ClasePadre1", forced.analysis().managerClass().id());
        assertTrue(forced.analysis().manualOverride());
    }

    @Test
    void cargaPesosYTerminosDesdeConfiguracionExterna() throws Exception {
        Path configFile = Files.createTempFile("manager-analysis", ".json");
        Files.writeString(configFile, "{\"weights\":{\"reachability\":2.5},\"managerNameTerms\":[]}");

        ManagerDetectorConfig config = ManagerDetectorConfig.load(configFile);

        assertEquals(2.5, config.reachabilityWeight);
        assertTrue(config.managerNameTerms.isEmpty());
        assertEquals(7.0, config.rootWeight);
        assertFalse(config.managerMethodTerms.isEmpty());
    }

    private static void assertManager(String fixture, String expectedId) throws Exception {
        ProjectModel model = parse(fixture);
        assertEquals(expectedId, model.analysis().managerClass().id(),
                () -> "Razones: " + model.analysis().managerClass().reasons());
    }

    private static ProjectModel parse(String fixture) throws Exception {
        return new ProjectParser().parse(FIXTURES.resolve(fixture));
    }

    private static Relation relation(ProjectModel model, String from, String to, String via) {
        return model.relations().stream()
                .filter(r -> r.from().equals(from) && r.to().equals(to) && java.util.Objects.equals(r.via(), via))
                .findFirst().orElseThrow(() -> new AssertionError("No existe la relacion " + from + " -> " + to
                        + " via " + via));
    }
}

package com.crud2gui.javamodelextractor;

import com.crud2gui.model.Analysis;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.model.Relation;
import com.crud2gui.model.SourceInfo;
import com.crud2gui.model.TypeInfo;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordina el analisis de una carpeta con codigo fuente Java y devuelve un
 * {@link ProjectModel} con sus tipos, miembros, herencia, Javadoc, relaciones UML y clase gestora.
 *
 * <p>Localiza los fuentes mediante {@link FolderScanner} y utiliza JavaParser para obtener el arbol
 * de sintaxis abstracta (AST) de cada fichero. Realiza dos pasadas: primero registra los tipos en
 * {@link TypeIndex}; despues prepara el contexto de cada fichero y delega la extraccion de tipos
 * y miembros en {@link FileTypeExtractor}, con ayuda de {@link TypeResolver}. Asi puede resolver
 * referencias entre clases sin depender del orden de lectura de los archivos.</p>
 *
 * <p>Finalmente invoca a {@link RelationAnalyzer} y {@link ManagerDetector} y reune sus resultados,
 * junto con la procedencia y los avisos, en el modelo. No compila ni ejecuta la solucion, ni
 * serializa, valida contra el esquema o guarda el JSON: esas operaciones corresponden a otras fases.</p>
 *
 * <p>Los problemas de lectura o sintaxis de cada fichero se registran en {@link SourceInfo#warnings()}
 * sin interrumpir el analisis de los demas ficheros.</p>
 */
public final class ProjectParser {

    private final FolderScanner scanner;

    /** Crea un analizador con la configuracion por defecto de {@link FolderScanner}. */
    public ProjectParser() {
        // Llama al constructor principal con un escaner de carpetas por defecto.
        // FolderScanner se encarga de buscar recursivamente los ficheros Java en la carpeta raíz.
        // El root del FolderScanner se establecer al buscar los ficheros Java.
        this(new FolderScanner());
    }

    /**
     * Crea un analizador con un escaner de carpetas propio.
     *
     * @param scanner escaner que decide que ficheros se analizan
     */
    public ProjectParser(FolderScanner scanner) {
        this.scanner = scanner;
    }

    /** Fichero parseado junto con su ruta relativa a la raiz. */
    private record Parsed(String relativeFile, CompilationUnit cu) { }

    /** Modelo analizado y rutas relativas de los ficheros seleccionados para el análisis. */
    record ProjectParseResult(ProjectModel model, List<String> files) {
        ProjectParseResult {
            files = List.copyOf(files);
        }
    }

    /**
     * Analiza todos los ficheros {@code .java} bajo una carpeta.
     *
     * @param root carpeta raiz del proyecto; puede contener subcarpetas y cualquier estructura de paquetes
     * @return el modelo con todos los tipos encontrados
     * @throws IOException si {@code root} no es una carpeta o no se puede recorrer
     */
    public ProjectModel parse(Path root) throws IOException {
        return parse(root, null);
    }

    /** Analiza el proyecto y permite fijar manualmente el id de la clase gestora. */
    public ProjectModel parse(Path root, String managerOverride) throws IOException {
        return parse(root, managerOverride, ManagerDetectorConfig.loadDefault());
    }

    /**
     * Analiza un proyecto Java completo mediante dos pasadas sobre los ficheros fuente.
     *
     * <p><b>Proceso de análisis:</b></p>
     * <ol>
     *   <li><b>Pasada 1 - Registro de tipos:</b> Localiza todos los ficheros {@code .java} en la
     *       carpeta raíz, los parsea con JavaParser (soporte Java 21) y registra los tipos de cada
     *       fichero en un índice. Los errores de sintaxis se capturan como avisos sin interrumpir
     *       el análisis de otros ficheros.</li>
     *   <li><b>Pasada 2 - Extracción de tipos:</b> Para cada fichero, construye el contexto de
     *       resolución (paquete e imports), y delega en {@link FileTypeExtractor} la extracción
     *       detallada de tipos, miembros, herencia y anotaciones. El {@link TypeResolver} usa el
     *       índice para resolver referencias cruzadas entre clases.</li>
     *   <li><b>Análisis de relaciones:</b> Invoca a {@link RelationAnalyzer} para detectar
     *       herencia, asociaciones y composiciones en el modelo de tipos extraído.</li>
     *   <li><b>Detección de clase gestora:</b> Usa {@link ManagerDetector} con la configuración
     *       proporcionada para identificar la clase coordinadora del proyecto. Si
     *       {@code managerOverride} es no nulo, fuerza esa clase como gestora.</li>
     * </ol>
     *
     * <p><b>Manejo de errores:</b> Los problemas de lectura o sintaxis se registran como avisos
     * en la información de procedencia del modelo, permitiendo que el análisis continúe incluso
     * si algunos ficheros no se pueden procesar correctamente.</p>
     *
     * @param root carpeta raíz del proyecto; se normalizará a ruta absoluta. Puede contener
     *             subcarpetas y cualquier estructura de paquetes
     * @param managerOverride identificador totalmente cualificado de la clase gestora
     *                        (ej: {@code "com.example.MyManager"}), o {@code null} para permitir
     *                        que el detector identifique automáticamente la clase coordinadora
     * @param config configuración para el algoritmo de detección automática de clase gestora
     *               (p.ej., patrones de nombre, anotaciones, heurísticas)
     *
     * @return {@link ProjectModel} poblado con tipos, miembros, relaciones, análisis y procedencia
     *
     * @throws IOException si {@code root} no es una carpeta accesible o no se puede recorrer
     *
     * @see TypeIndex
     * @see FileTypeExtractor
     * @see TypeResolver
     * @see RelationAnalyzer
     * @see ManagerDetector
     */
    ProjectModel parse(Path root, String managerOverride, ManagerDetectorConfig config) throws IOException {
        return parseWithFiles(root, managerOverride, config).model();
    }

    ProjectParseResult parseWithFiles(Path root, String managerOverride, ManagerDetectorConfig config) throws IOException {
        // Normalizar la ruta: convertir a absoluta y resolver enlaces/referencias relativas
        root = root.toAbsolutePath().normalize();
        
        // Localizar todos los ficheros .java bajo la carpeta raíz
        List<Path> files = scanner.findJavaFiles(root);
        
        // Almacenar problemas encontrados durante el análisis (errores de sintaxis, IO, etc.)
        List<String> warnings = new ArrayList<>();

        // Configurar el parseador de Java con soporte a Java 21 y codificación UTF-8
        JavaParser javaParser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setCharacterEncoding(StandardCharsets.UTF_8));

        // ===== PASADA 1: Parsear ficheros y registrar tipos =====
        List<Parsed> parsed = new ArrayList<>();
        TypeIndex index = new TypeIndex();
        for (Path file : files) {
            String rel = root.relativize(file).toString().replace('\\', '/');
            try {
                ParseResult<CompilationUnit> result = javaParser.parse(file);
                
                // Si el parseado tuvo problemas (ej: errores de sintaxis), registrarlos
                if (!result.isSuccessful()) {
                    String detail = result.getProblems().isEmpty() ? "error de sintaxis"
                            : result.getProblems().get(0).getMessage();
                    warnings.add(rel + ": " + detail);
                }
                
                // Procesar el resultado si existe (aunque haya habido problemas menores)
                if (result.getResult().isPresent()) {
                    CompilationUnit cu = result.getResult().get();
                    parsed.add(new Parsed(rel, cu));
                    
                    // Registrar todos los tipos (clases, interfaces, enums) de este fichero en el índice
                    String pkg = packageOf(cu);
                    for (TypeDeclaration<?> td : cu.getTypes()) {
                        register(index, pkg, null, td);
                    }
                }
            } catch (IOException | ParseProblemException e) {
                // Capturar excepciones sin interrumpir el análisis de otros ficheros
                warnings.add(rel + ": " + e.getMessage());
            }
        }

        // ===== PASADA 2: Extraer información detallada de tipos y miembros =====
        TypeResolver resolver = new TypeResolver(index);
        List<TypeInfo> types = new ArrayList<>();
        for (Parsed p : parsed) {
            // Construir el contexto de resolución (paquete + imports) para este fichero
            ResolutionContext base = contextOf(p.cu());
            
            // Extractor específico por fichero
            FileTypeExtractor extractor = new FileTypeExtractor(resolver, p.relativeFile());
            
            // Extraer información detallada de cada tipo raíz del fichero
            for (TypeDeclaration<?> td : p.cu().getTypes()) {
                extractor.extract(td, null, base, types);
            }
        }

        // ===== Análisis de relaciones UML =====
        List<CompilationUnit> units = parsed.stream().map(Parsed::cu).toList();
        List<Relation> relations = new RelationAnalyzer(resolver).analyze(types, units);
        
        // ===== Detección de clase gestora =====
        Analysis analysis = new ManagerDetector(resolver, config).analyze(types, relations, units, managerOverride);
        
        // ===== Compilar metadatos de procedencia =====
        SourceInfo source = new SourceInfo(root.toString(), Instant.now().toString(), files.size(), warnings);
        
        // Retornar el modelo y las rutas encontradas para que la CLI pueda informar del análisis.
        ProjectModel model = new ProjectModel(ProjectModel.CURRENT_SCHEMA, source, types, relations, analysis, null);
        List<String> analyzedFiles = new ArrayList<>(files.size());
        for (Path file : files) {
            analyzedFiles.add(root.relativize(file).toString().replace('\\', '/'));
        }
        return new ProjectParseResult(model, analyzedFiles);
    }

    /**
     * Obtiene el paquete de un fichero.
     *
     * @param cu unidad de compilacion
     * @return el nombre del paquete, o cadena vacia si es el paquete por defecto
     */
    static String packageOf(CompilationUnit cu) {
        return cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
    }

    /**
     * Registra un tipo y, recursivamente, sus tipos internos en el indice.
     *
     * @param index       registro donde anadir los ids
     * @param pkg         paquete del fichero
     * @param enclosingId id del tipo contenedor, o {@code null} si es de primer nivel
     * @param td          declaracion a registrar
     */
    private static void register(TypeIndex index, String pkg, String enclosingId, TypeDeclaration<?> td) {
        String id = enclosingId != null ? enclosingId + "." + td.getNameAsString()
                : (pkg.isEmpty() ? td.getNameAsString() : pkg + "." + td.getNameAsString());
        index.add(id);
        for (BodyDeclaration<?> member : td.getMembers()) {
            if (member instanceof TypeDeclaration<?> inner) {
                register(index, pkg, id, inner);
            }
        }
    }

    /**
     * Construye el contexto de resolucion de un fichero a partir de su paquete y sus imports.
     * Los imports estaticos se ignoran.
     *
     * @param cu unidad de compilacion
     * @return el contexto, sin tipos contenedores todavia
     */
    static ResolutionContext contextOf(CompilationUnit cu) {
        Map<String, String> explicit = new HashMap<>();
        List<String> wildcard = new ArrayList<>();
        for (ImportDeclaration imp : cu.getImports()) {
            if (imp.isStatic()) continue;
            String name = imp.getNameAsString();
            if (imp.isAsterisk()) {
                wildcard.add(name);
            } else {
                explicit.put(name.substring(name.lastIndexOf('.') + 1), name);
            }
        }
        return new ResolutionContext(packageOf(cu), explicit, wildcard, List.of());
    }
}

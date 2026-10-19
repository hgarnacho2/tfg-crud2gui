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
 * Analiza una carpeta con codigo fuente Java y construye un {@link ProjectModel} con los tipos,
 * sus miembros, la herencia, el javadoc, las relaciones UML y la clase gestora detectada.
 *
 * <p>El analisis tiene dos pasadas: primero se parsean todos los ficheros y se registran los tipos
 * declarados; despues se extrae cada tipo resolviendo sus nombres contra ese registro. Los ficheros
 * con errores de sintaxis generan un aviso en {@link SourceInfo#warnings()} y no interrumpen el proceso.</p>
 */
public final class ProjectParser {

    private final FolderScanner scanner;

    /** Crea un analizador con la configuracion por defecto de {@link FolderScanner}. */
    public ProjectParser() {
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

    ProjectModel parse(Path root, String managerOverride, ManagerDetectorConfig config) throws IOException {
        root = root.toAbsolutePath().normalize();
        List<Path> files = scanner.findJavaFiles(root);
        List<String> warnings = new ArrayList<>();

        JavaParser javaParser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setCharacterEncoding(StandardCharsets.UTF_8));

        // Pasada 1: parsear y registrar tipos
        List<Parsed> parsed = new ArrayList<>();
        TypeIndex index = new TypeIndex();
        for (Path file : files) {
            String rel = root.relativize(file).toString().replace('\\', '/');
            try {
                ParseResult<CompilationUnit> result = javaParser.parse(file);
                if (!result.isSuccessful()) {
                    String detail = result.getProblems().isEmpty() ? "error de sintaxis"
                            : result.getProblems().get(0).getMessage();
                    warnings.add(rel + ": " + detail);
                }
                if (result.getResult().isPresent()) {
                    CompilationUnit cu = result.getResult().get();
                    parsed.add(new Parsed(rel, cu));
                    String pkg = packageOf(cu);
                    for (TypeDeclaration<?> td : cu.getTypes()) {
                        register(index, pkg, null, td);
                    }
                }
            } catch (IOException | ParseProblemException e) {
                warnings.add(rel + ": " + e.getMessage());
            }
        }

        // Pasada 2: extraer tipos
        TypeResolver resolver = new TypeResolver(index);
        List<TypeInfo> types = new ArrayList<>();
        for (Parsed p : parsed) {
            ResolutionContext base = contextOf(p.cu());
            FileTypeExtractor extractor = new FileTypeExtractor(resolver, p.relativeFile());
            for (TypeDeclaration<?> td : p.cu().getTypes()) {
                extractor.extract(td, null, base, types);
            }
        }

        List<CompilationUnit> units = parsed.stream().map(Parsed::cu).toList();
        List<Relation> relations = new RelationAnalyzer(resolver).analyze(types, units);
        Analysis analysis = new ManagerDetector(resolver, config).analyze(types, relations, units, managerOverride);
        SourceInfo source = new SourceInfo(root.toString(), Instant.now().toString(), files.size(), warnings);
        return new ProjectModel(ProjectModel.CURRENT_SCHEMA, source, types, relations, analysis, null);
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

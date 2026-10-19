package com.crud2gui.javamodelextractor;

import com.crud2gui.model.Analysis;
import com.crud2gui.model.Confidence;
import com.crud2gui.model.FieldInfo;
import com.crud2gui.model.ManagerCandidate;
import com.crud2gui.model.Modifier;
import com.crud2gui.model.Relation;
import com.crud2gui.model.RelationKind;
import com.crud2gui.model.TypeInfo;
import com.crud2gui.model.TypeKind;
import com.crud2gui.model.TypeRef;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Detecta y puntua candidatas a clase gestora despues de extraer tipos y relaciones UML.
 *
 * <p>Recibe el modelo de tipos, las relaciones calculadas por {@link RelationAnalyzer} y las unidades
 * AST de JavaParser. Primero crea un grafo dirigido solo con relaciones de atributos (composicion,
 * agregacion y asociacion). Para calcular el alcance, anade aristas desde cada abstraccion hacia sus
 * subclases o implementaciones, de modo que una referencia a una interfaz tambien alcance las
 * implementaciones concretas. Las dependencias de parametros y variables locales no cuentan como
 * alcance estructural.</p>
 *
 * <p>Los componentes fuertemente conexos del grafo de atributos se condensan para decidir que
 * componentes son raices. Todos los tipos de una componente raiz reciben la señal de raiz; una
 * relacion bidireccional, por tanto, no cancela esa señal. El algoritmo solo puntua clases y records
 * con al menos un atributo de tipo del proyecto. Excluye tipos abstractos, interfaces, enums,
 * lanzadores, tests y clases de utilidades estaticas.</p>
 *
 * <p>La puntuacion es una suma ponderada, no una probabilidad:</p>
 * <pre>{@code
 * score = reachabilityWeight * reachableDomainFraction
 *       + rootWeight * isRoot
 *       + collectionWeight * min(domainCollectionFields, 2)
 *       + collectionUsageWeight * min(usedDomainCollections, 2)
 *       + entryPointWeight * instantiatedFromMainOrTest
 *       + lexicalWeight * hasConfiguredLexicalHint
 * }</pre>
 * <p>La fraccion de alcance es el numero de otros tipos concretos del dominio alcanzables dividido
 * por el numero de otros tipos concretos del dominio (como minimo uno). Los campos de coleccion
 * incluyen sus argumentos genericos, tanto claves como valores de mapas. El uso se reconoce en el
 * AST por llamadas a operaciones conocidas de coleccion e iteraciones {@code for-each}. Pesos,
 * terminos lexicos y umbrales de confianza vienen de {@code manager-analysis.json}.</p>
 *
 * <p>La candidata se elige por puntuacion descendente. La confianza compara la diferencia de
 * puntuacion entre candidatas vecinas con los umbrales configurados. Un override manual valido
 * sustituye la seleccion heuristica; si no hay candidatas estructurales se usa una candidata de
 * respaldo con puntuacion cero y se explica en sus razones. El resultado incluye alternativas,
 * puntos de entrada y candidatas descendientes que pueden ser subgestoras.</p>
 */
final class ManagerDetector {
    private static final Set<String> COLLECTION_TYPES = Set.of(
            "Collection", "Iterable", "List", "Set", "Queue", "Deque", "Map",
            "ArrayList", "LinkedList", "Vector", "Stack", "HashSet", "LinkedHashSet",
            "TreeSet", "HashMap", "LinkedHashMap", "TreeMap", "ConcurrentMap",
            "ConcurrentHashMap", "Optional");
    private static final Set<String> COLLECTION_OPERATIONS = Set.of(
            "add", "addAll", "put", "putAll", "remove", "removeAll", "retainAll", "clear",
            "contains", "containsAll", "containsKey", "containsValue", "get", "getOrDefault",
            "compute", "computeIfAbsent", "computeIfPresent", "merge", "replace", "replaceAll",
            "size", "isEmpty", "iterator", "forEach", "stream", "parallelStream", "values",
            "keySet", "entrySet", "toArray", "sort", "offer", "poll", "push", "pop", "peek");
    private static final Set<RelationKind> ATTRIBUTE_RELATIONS = Set.of(
            RelationKind.COMPOSITION, RelationKind.AGGREGATION, RelationKind.ASSOCIATION);

    private final TypeResolver resolver;
    private final ManagerDetectorConfig config;

    /**
     * Crea el detector con el resolutor y la configuracion del proyecto.
     *
     * @param resolver resuelve referencias AST a tipos declarados en el proyecto
     * @param config pesos, umbrales y terminos lexicos usados al puntuar
     */
    ManagerDetector(TypeResolver resolver, ManagerDetectorConfig config) {
        this.resolver = resolver;
        this.config = config;
    }

    /**
     * Analiza las candidatas y compone el resultado que se serializa como {@code ProjectModel.analysis}.
     * Construye los grafos, puntua los tipos elegibles, ordena alternativas y aplica un override valido.
     *
     * @param types tipos extraidos del proyecto
     * @param relations relaciones UML calculadas para esos tipos
     * @param units unidades AST usadas para inspeccionar cuerpos de metodos y constructores
     * @param manualOverride id completo de una clase elegida manualmente, o {@code null}
     * @return analisis con candidata principal, alternativas, subgestoras y puntos de entrada
     * @throws IllegalArgumentException si el override no identifica un tipo del proyecto
     */
    Analysis analyze(List<TypeInfo> types, List<Relation> relations, List<CompilationUnit> units,
                     String manualOverride) {
        Map<String, TypeInfo> typesById = new LinkedHashMap<>();
        types.forEach(type -> typesById.put(type.id(), type));
        List<Relation> attributeRelations = relations.stream()
                .filter(relation -> ATTRIBUTE_RELATIONS.contains(relation.kind()))
                .toList();
        Map<String, Set<String>> attributeGraph = graph(typesById.keySet(), attributeRelations);
        Map<String, Set<String>> reachabilityGraph = graph(typesById.keySet(), attributeRelations);
        addImplementationEdges(relations, reachabilityGraph);
        Map<String, Integer> componentById = new Tarjan(attributeGraph).components(typesById.keySet());
        Set<String> roots = findRootComponents(attributeGraph, componentById);
        Map<String, DeclarationContext> declarations = declarations(units);
        Set<String> domainIds = new LinkedHashSet<>();
        for (TypeInfo type : types) {
            if (isDomainType(type)) domainIds.add(type.id());
        }

        Map<String, CandidateScore> scores = new LinkedHashMap<>();
        for (TypeInfo type : types) {
            if (!isEligible(type, typesById, attributeRelations)) continue;
            scores.put(type.id(), score(type, domainIds, roots, reachabilityGraph,
                    declarations.get(type.id()), declarations, typesById));
        }

        List<TypeInfo> entryPoints = types.stream().filter(TypeInfo::hasMainMethod).toList();
        if (scores.isEmpty() && !types.isEmpty()) {
            TypeInfo fallback = types.stream().filter(type -> !isTestType(type)).findFirst().orElse(types.get(0));
            scores.put(fallback.id(), new CandidateScore(0.0,
                    List.of("No se encontraron candidatas estructurales; seleccion de respaldo."), 0, 0, 0));
        }
        if (scores.isEmpty()) {
            return new Analysis(null, List.of(), List.of(), entryPoints.stream().map(TypeInfo::id).toList(), false);
        }

        List<Map.Entry<String, CandidateScore>> ranked = scores.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, CandidateScore>>comparingDouble(entry -> entry.getValue().score())
                        .reversed().thenComparing(Map.Entry::getKey))
                .toList();
        String selectedId = ranked.get(0).getKey();
        boolean manual = manualOverride != null && !manualOverride.isBlank();
        if (manual) {
            if (!typesById.containsKey(manualOverride)) {
                throw new IllegalArgumentException("La clase gestora indicada no existe en el proyecto: " + manualOverride);
            }
            selectedId = manualOverride;
        }

        String selectedManagerId = selectedId;
        CandidateScore selectedScore = scores.getOrDefault(selectedManagerId,
                new CandidateScore(0.0, List.of("Seleccion fijada manualmente."), 0, 0, 0));
        List<String> selectedReasons = new ArrayList<>(selectedScore.reasons());
        if (manual) selectedReasons.add("Seleccion fijada manualmente; sustituye la heuristica.");
        Confidence selectedConfidence = manual ? Confidence.HIGH
                : confidence(ranked, 0, selectedScore.score());
        ManagerCandidate manager = candidate(selectedManagerId, selectedScore, selectedConfidence, selectedReasons);

        List<ManagerCandidate> alternatives = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<String, CandidateScore> entry = ranked.get(i);
            if (entry.getKey().equals(selectedManagerId)) continue;
            alternatives.add(candidate(entry.getKey(), entry.getValue(), confidence(ranked, i,
                    entry.getValue().score()), entry.getValue().reasons()));
        }
        alternatives.sort(Comparator.comparingDouble(ManagerCandidate::score).reversed().thenComparing(ManagerCandidate::id));

        Set<String> subManagers = reachable(attributeGraph, selectedManagerId);
        List<String> subManagerIds = scores.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(selectedManagerId) && subManagers.contains(entry.getKey()))
                .filter(entry -> !java.util.Objects.equals(componentById.get(entry.getKey()),
                        componentById.get(selectedManagerId)))
                .filter(entry -> entry.getValue().collections() > 0 || entry.getValue().collectionUsage() > 0
                        || entry.getValue().reachability() >= 2)
                .sorted(Comparator.<Map.Entry<String, CandidateScore>>comparingDouble(entry -> entry.getValue().score())
                        .reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).toList();

        return new Analysis(manager, alternatives, subManagerIds,
                entryPoints.stream().map(TypeInfo::id).toList(), manual);
    }

    /**
     * Calcula la puntuacion ponderada de una candidata y conserva las razones de cada señal positiva.
     *
     * @param type candidata que se puntua
     * @param domainIds ids de tipos concretos considerados parte del dominio
     * @param roots ids que pertenecen a componentes raiz del grafo de atributos
     * @param reachabilityGraph grafo usado para medir el alcance estructural
     * @param declaration declaracion AST de la candidata, o {@code null} si no se encontro
     * @param declarations declaraciones AST del proyecto indexadas por id
     * @param typesById tipos del proyecto indexados por id
     * @return puntuacion, razones y contadores usados para clasificar la candidata
     */
    private CandidateScore score(TypeInfo type, Set<String> domainIds, Set<String> roots,
                                 Map<String, Set<String>> reachabilityGraph, DeclarationContext declaration,
                                 Map<String, DeclarationContext> declarations, Map<String, TypeInfo> typesById) {
        List<String> reasons = new ArrayList<>();
        Set<String> reachable = reachable(reachabilityGraph, type.id());
        reachable.retainAll(domainIds);
        reachable.remove(type.id());
        int domainDenominator = Math.max(1, domainIds.size() - (domainIds.contains(type.id()) ? 1 : 0));
        double reachability = (double) reachable.size() / domainDenominator;
        double total = config.reachabilityWeight * reachability;
        if (!reachable.isEmpty()) {
            reasons.add("Alcance estructural: alcanza " + reachable.size() + " tipos de dominio.");
        }

        boolean isRoot = roots.contains(type.id());
        if (isRoot) {
            total += config.rootWeight;
            reasons.add("Raiz estructural: nadie la contiene como atributo (considerando ciclos).");
        }

        List<FieldInfo> collectionFields = type.fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .filter(field -> isCollectionField(field.type()) && !projectTypes(field.type()).isEmpty())
                .toList();
        int collections = collectionFields.size();
        if (collections > 0) {
            total += config.collectionWeight * Math.min(2, collections);
            reasons.add("Contiene " + collections + " atributo(s) de coleccion con tipos del proyecto.");
        }

        Set<String> usedCollections = declaration == null ? Set.of()
                : usedCollectionFields(declaration.declaration(), collectionFields);
        int collectionUsage = usedCollections.size();
        if (collectionUsage > 0) {
            total += config.collectionUsageWeight * Math.min(2, collectionUsage);
            reasons.add("Sus metodos operan sobre " + collectionUsage + " coleccion(es) del dominio.");
        }

        if (instantiatedFromEntryPointOrTest(type.id(), declarations, typesById)) {
            total += config.entryPointWeight;
            reasons.add("Instanciada desde un punto de entrada o una prueba.");
        }

        if (hasLexicalHint(type)) {
            total += config.lexicalWeight;
            reasons.add("Coincidencia lexica de baja ponderacion (solo desempate).");
        }
        if (reasons.isEmpty()) reasons.add("Candidata por contener atributos de tipos del proyecto.");
        return new CandidateScore(total, List.copyOf(reasons), reachable.size(), collections, collectionUsage);
    }

    /**
     * Comprueba si un tipo puede competir por el puesto de gestora.
     *
     * @param type tipo que se evalua
     * @param typesById tipos declarados en el proyecto
     * @param relations relaciones disponibles para comprobar que posee enlaces de dominio
     * @return {@code true} si es una clase concreta/record con atributos de dominio y no esta excluida
     */
    private boolean isEligible(TypeInfo type, Map<String, TypeInfo> typesById, List<Relation> relations) {
        if (type.kind() != TypeKind.CLASS && type.kind() != TypeKind.RECORD) return false;
        if (type.modifiers().contains(Modifier.ABSTRACT) || type.hasMainMethod() || isTestType(type)) return false;
        List<FieldInfo> domainFields = type.fields().stream()
                .filter(field -> !field.modifiers().contains(Modifier.STATIC))
                .filter(field -> !projectTypes(field.type()).isEmpty())
                .toList();
        if (domainFields.isEmpty()) return false;
        boolean onlyStaticDomainFields = type.fields().stream()
                .filter(field -> !projectTypes(field.type()).isEmpty())
                .allMatch(field -> field.modifiers().contains(Modifier.STATIC));
        boolean onlyStaticMethods = type.methods().stream().filter(method -> method.hasBody())
                .allMatch(method -> method.modifiers().contains(Modifier.STATIC));
        if (onlyStaticDomainFields && onlyStaticMethods) return false;
        return relations.stream().anyMatch(relation -> relation.from().equals(type.id())
                && typesById.containsKey(relation.to()));
    }

    /**
     * Indica si un tipo concreto puede contarse en el denominador del alcance.
     *
     * @param type tipo que se evalua
     * @return {@code true} para clases/records concretos que no son lanzadores ni tests
     */
    private static boolean isDomainType(TypeInfo type) {
        return (type.kind() == TypeKind.CLASS || type.kind() == TypeKind.RECORD)
                && !type.modifiers().contains(Modifier.ABSTRACT) && !type.hasMainMethod() && !isTestType(type);
    }

    /**
     * Detecta tests por ruta, nombre o anotaciones cuyo nombre termina en {@code Test}.
     *
     * @param type tipo que se inspecciona
     * @return {@code true} si hay indicios de que el tipo es una clase de pruebas
     */
    private static boolean isTestType(TypeInfo type) {
        String file = type.location() == null ? "" : type.location().file().replace('\\', '/').toLowerCase(Locale.ROOT);
        String name = type.name().toLowerCase(Locale.ROOT);
        return file.contains("/test/") || file.startsWith("test/") || name.startsWith("test") || name.endsWith("test")
                || type.methods().stream().flatMap(method -> method.annotations().stream())
                .anyMatch(annotation -> annotation.name().toLowerCase(Locale.ROOT).endsWith("test"));
    }

    /**
     * Busca instanciaciones directas de la candidata dentro de metodos {@code main} o de pruebas.
     *
     * @param candidateId id completo de la candidata
     * @param declarations AST del proyecto indexado por id de tipo
     * @param typesById tipos del proyecto, usados para reconocer clases de prueba
     * @return {@code true} si encuentra una expresion {@code new} de la candidata
     */
    private boolean instantiatedFromEntryPointOrTest(String candidateId,
                                                      Map<String, DeclarationContext> declarations,
                                                      Map<String, TypeInfo> typesById) {
        for (Map.Entry<String, DeclarationContext> entry : declarations.entrySet()) {
            TypeInfo owner = typesById.get(entry.getKey());
            boolean testType = owner != null && isTestType(owner);
            for (BodyDeclaration<?> member : entry.getValue().declaration().getMembers()) {
                if (!(member instanceof MethodDeclaration method) || method.getBody().isEmpty()) continue;
                boolean entryPoint = method.getNameAsString().equals("main") && method.isStatic();
                boolean testMethod = testType || method.getAnnotations().stream()
                        .anyMatch(annotation -> annotation.getNameAsString().toLowerCase(Locale.ROOT).endsWith("test"));
                if (!entryPoint && !testMethod) continue;
                for (ObjectCreationExpr creation : method.getBody().get().findAll(ObjectCreationExpr.class)) {
                    TypeRef constructed = resolver.resolve(creation.getType(), entry.getValue().context());
                    if (constructed.projectType() && candidateId.equals(constructed.qualifiedName())) return true;
                }
            }
        }
        return false;
    }

    /**
     * Encuentra campos de coleccion del dominio operados o recorridos por metodos con cuerpo.
     * Reconoce referencias directas al campo y variables locales que lo aliasan directamente.
     *
     * @param declaration declaracion AST de la candidata
     * @param fields campos ya clasificados como colecciones de tipos del proyecto
     * @return nombres de campos usados en llamadas reconocidas o bucles {@code for-each}
     */
    private Set<String> usedCollectionFields(TypeDeclaration<?> declaration, List<FieldInfo> fields) {
        Set<String> fieldNames = new LinkedHashSet<>();
        fields.forEach(field -> fieldNames.add(field.name()));
        Set<String> used = new LinkedHashSet<>();
        for (MethodDeclaration method : declaration.getMethods()) {
            if (method.getBody().isEmpty()) continue;
            Node body = method.getBody().get();
            Map<String, String> aliases = new HashMap<>();
            for (VariableDeclarator variable : body.findAll(VariableDeclarator.class)) {
                if (variable.getInitializer().isPresent()) {
                    String source = fieldReference(variable.getInitializer().get());
                    if (fieldNames.contains(source)) aliases.put(variable.getNameAsString(), source);
                }
            }
            for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
                if (!COLLECTION_OPERATIONS.contains(call.getNameAsString())) continue;
                call.getScope().ifPresent(scope -> {
                    String reference = fieldReference(scope);
                    if (fieldNames.contains(reference)) used.add(reference);
                    else if (aliases.containsKey(reference)) used.add(aliases.get(reference));
                });
            }
            for (ForEachStmt loop : body.findAll(ForEachStmt.class)) {
                String reference = fieldReference(loop.getIterable());
                if (fieldNames.contains(reference)) used.add(reference);
                else if (aliases.containsKey(reference)) used.add(aliases.get(reference));
            }
        }
        return used;
    }

    /**
     * Comprueba si el nombre de clase o algun metodo contiene terminos lexicos configurados.
     *
     * @param type candidata cuyos nombres se comparan
     * @return {@code true} si hay una coincidencia; solo se anade como desempate de bajo peso
     */
    private boolean hasLexicalHint(TypeInfo type) {
        String className = type.name().toLowerCase(Locale.ROOT);
        if (containsTerm(className, config.managerNameTerms)) return true;
        return type.methods().stream().map(method -> method.name().toLowerCase(Locale.ROOT))
                .anyMatch(name -> containsTerm(name, config.managerMethodTerms));
    }

    /** Busca cualquiera de los terminos configurados como subcadena del texto dado. */
    private static boolean containsTerm(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }

    /**
     * Determina si un tipo de campo es una coleccion, un array o contiene uno en sus genericos.
     *
     * @param type tipo declarado del campo
     * @return {@code true} si su nombre o estructura generica corresponde a una coleccion reconocida
     */
    private static boolean isCollectionField(TypeRef type) {
        if (type.arrayDimensions() > 0 || COLLECTION_TYPES.contains(simpleName(type.name()))) return true;
        return type.arguments().stream().anyMatch(ManagerDetector::containsDomainCollection);
    }

    /** Busca recursivamente una coleccion o array dentro de los argumentos genericos de un tipo. */
    private static boolean containsDomainCollection(TypeRef type) {
        return COLLECTION_TYPES.contains(simpleName(type.name())) || type.arrayDimensions() > 0
                || type.arguments().stream().anyMatch(ManagerDetector::containsDomainCollection);
    }

    /** Devuelve la parte simple de un nombre cualificado, conservandolo si no contiene puntos. */
    private static String simpleName(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }

    /**
     * Recoge los ids de proyecto de un tipo y de todos sus argumentos genericos.
     *
     * @param type referencia de tipo que se inspecciona
     * @return ids unicos de tipos del proyecto contenidos en la referencia
     */
    private static Set<String> projectTypes(TypeRef type) {
        Set<String> ids = new LinkedHashSet<>();
        if (type.projectType() && type.qualifiedName() != null) ids.add(type.qualifiedName());
        type.arguments().forEach(argument -> ids.addAll(projectTypes(argument)));
        return ids;
    }

    /**
     * Extrae una referencia simple a campo desde {@code campo} o {@code this.campo}.
     *
     * @param expression expresion AST a inspeccionar
     * @return nombre del campo, o cadena vacia si la expresion no es una referencia directa
     */
    private static String fieldReference(Expression expression) {
        if (expression instanceof NameExpr name) return name.getNameAsString();
        if (expression instanceof FieldAccessExpr access && access.getScope().isThisExpr()) {
            return access.getNameAsString();
        }
        return "";
    }

    /**
     * Crea una lista de adyacencia vacia para los ids y anade las aristas internas al conjunto.
     *
     * @param ids ids que formaran los nodos del grafo
     * @param relations relaciones dirigidas que se incorporan si ambos extremos estan presentes
     * @return grafo dirigido de origen a destino
     */
    private static Map<String, Set<String>> graph(Set<String> ids, List<Relation> relations) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        ids.forEach(id -> graph.put(id, new LinkedHashSet<>()));
        for (Relation relation : relations) {
            if (graph.containsKey(relation.from()) && graph.containsKey(relation.to())) {
                graph.get(relation.from()).add(relation.to());
            }
        }
        return graph;
    }

    /**
     * Permite recorrer de una abstraccion hacia sus subclases e implementaciones concretas.
     *
     * @param relations relaciones de herencia/realizacion originales (subtipo a supertipo)
     * @param graph grafo de alcance al que se agregan aristas supertipo a subtipo
     */
    private static void addImplementationEdges(List<Relation> relations, Map<String, Set<String>> graph) {
        for (Relation relation : relations) {
            if (relation.kind() == RelationKind.INHERITANCE || relation.kind() == RelationKind.REALIZATION) {
                if (graph.containsKey(relation.from()) && graph.containsKey(relation.to())) {
                    graph.get(relation.to()).add(relation.from());
                }
            }
        }
    }

    /**
     * Calcula iterativamente todos los nodos alcanzables desde un origen, evitando ciclos.
     *
     * @param graph lista de adyacencia del grafo dirigido
     * @param start id desde el que comienza el recorrido
     * @return ids alcanzables; no incluye el propio origen salvo que exista un ciclo hacia el
     */
    private static Set<String> reachable(Map<String, Set<String>> graph, String start) {
        Set<String> visited = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>(graph.getOrDefault(start, Set.of()));
        while (!pending.isEmpty()) {
            String current = pending.removeFirst();
            if (visited.add(current)) pending.addAll(graph.getOrDefault(current, Set.of()));
        }
        return visited;
    }

    /**
     * Identifica los nodos cuyas componentes fuertemente conexas no reciben aristas entrantes.
     *
     * @param graph grafo de atributos original
     * @param componentById componente SCC asignada a cada nodo por Tarjan
     * @return ids que pertenecen a una componente raiz
     */
    private static Set<String> findRootComponents(Map<String, Set<String>> graph,
                                                  Map<String, Integer> componentById) {
        Set<Integer> incoming = new HashSet<>();
        for (Map.Entry<String, Set<String>> edge : graph.entrySet()) {
            int source = componentById.get(edge.getKey());
            for (String targetId : edge.getValue()) {
                int target = componentById.get(targetId);
                if (source != target) incoming.add(target);
            }
        }
        Set<String> roots = new LinkedHashSet<>();
        componentById.forEach((id, component) -> {
            if (!incoming.contains(component)) roots.add(id);
        });
        return roots;
    }

    /**
     * Indexa las declaraciones de tipo AST por id, conservando su contexto de paquete e imports.
     *
     * @param units unidades de compilacion parseadas
     * @return contexto AST de cada tipo de primer nivel e interno
     */
    private static Map<String, DeclarationContext> declarations(List<CompilationUnit> units) {
        Map<String, DeclarationContext> declarations = new LinkedHashMap<>();
        for (CompilationUnit unit : units) {
            ResolutionContext base = ProjectParser.contextOf(unit);
            for (TypeDeclaration<?> declaration : unit.getTypes()) {
                register(declaration, null, base, declarations);
            }
        }
        return declarations;
    }

    /** Registra recursivamente una declaracion y sus tipos internos con contexto de resolucion. */
    private static void register(TypeDeclaration<?> declaration, String enclosingId, ResolutionContext base,
                                 Map<String, DeclarationContext> declarations) {
        String name = declaration.getNameAsString();
        String id = enclosingId != null ? enclosingId + "." + name
                : (base.packageName().isEmpty() ? name : base.packageName() + "." + name);
        List<String> enclosing = new ArrayList<>();
        enclosing.add(id);
        enclosing.addAll(base.enclosing());
        ResolutionContext context = base.withEnclosing(enclosing);
        declarations.put(id, new DeclarationContext(declaration, context));
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) register(nested, id, context, declarations);
        }
    }

    /**
     * Asigna confianza por la distancia de puntuacion a las candidatas vecinas.
     *
     * @param ranked candidatas ordenadas de mayor a menor puntuacion
     * @param index posicion de la candidata dentro de la lista
     * @param score puntuacion de esa candidata
     * @return confianza configurada como alta, media o baja
     */
    private Confidence confidence(List<Map.Entry<String, CandidateScore>> ranked, int index, double score) {
        double gap;
        if (ranked.size() == 1) {
            gap = score;
        } else if (index == 0) {
            gap = score - ranked.get(1).getValue().score();
        } else {
            double higherGap = ranked.get(index - 1).getValue().score() - score;
            double lowerGap = index + 1 < ranked.size()
                    ? score - ranked.get(index + 1).getValue().score() : higherGap;
            gap = Math.min(higherGap, lowerGap);
        }
        if (gap >= config.highConfidenceGap && score >= config.highConfidenceMinimumScore) return Confidence.HIGH;
        if (gap >= config.mediumConfidenceGap || (ranked.size() == 1 && score >= config.mediumConfidenceGap)) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    /** Convierte datos internos de puntuacion al objeto que se guarda en el metamodelo. */
    private static ManagerCandidate candidate(String id, CandidateScore score, Confidence confidence,
                                              List<String> reasons) {
        return new ManagerCandidate(id, score.score(), confidence, reasons);
    }

    private record CandidateScore(double score, List<String> reasons, int reachability,
                                  int collections, int collectionUsage) { }

    private record DeclarationContext(TypeDeclaration<?> declaration, ResolutionContext context) { }

    private static final class Tarjan {
        private final Map<String, Set<String>> graph;
        private final Map<String, Integer> index = new HashMap<>();
        private final Map<String, Integer> lowLink = new HashMap<>();
        private final Deque<String> stack = new ArrayDeque<>();
        private final Set<String> onStack = new HashSet<>();
        private final Map<String, Integer> componentById = new HashMap<>();
        private int nextIndex;
        private int nextComponent;

        /** Crea el buscador SCC para un grafo dirigido. */
        private Tarjan(Map<String, Set<String>> graph) {
            this.graph = graph;
        }

        /**
         * Calcula las componentes fuertemente conexas de todos los ids del grafo.
         *
         * @param ids nodos que se deben incluir en el recorrido
         * @return mapa de id de nodo a identificador de componente
         */
        private Map<String, Integer> components(Set<String> ids) {
            ids.stream().sorted().filter(id -> !index.containsKey(id)).forEach(this::visit);
            return componentById;
        }

        /** Ejecuta Tarjan recursivamente desde un nodo aun no visitado. */
        private void visit(String id) {
            index.put(id, nextIndex);
            lowLink.put(id, nextIndex++);
            stack.push(id);
            onStack.add(id);
            for (String target : graph.getOrDefault(id, Set.of())) {
                if (!index.containsKey(target)) {
                    visit(target);
                    lowLink.put(id, Math.min(lowLink.get(id), lowLink.get(target)));
                } else if (onStack.contains(target)) {
                    lowLink.put(id, Math.min(lowLink.get(id), index.get(target)));
                }
            }
            if (lowLink.get(id).equals(index.get(id))) {
                String member;
                do {
                    member = stack.pop();
                    onStack.remove(member);
                    componentById.put(member, nextComponent);
                } while (!member.equals(id));
                nextComponent++;
            }
        }
    }
}
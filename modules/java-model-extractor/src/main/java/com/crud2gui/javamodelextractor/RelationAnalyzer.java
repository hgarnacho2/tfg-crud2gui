package com.crud2gui.javamodelextractor;

import com.crud2gui.model.Confidence;
import com.crud2gui.model.Evidence;
import com.crud2gui.model.FieldInfo;
import com.crud2gui.model.Relation;
import com.crud2gui.model.RelationKind;
import com.crud2gui.model.TypeInfo;
import com.crud2gui.model.TypeKind;
import com.crud2gui.model.TypeRef;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deriva relaciones UML entre los tipos del proyecto a partir del modelo extraido y de los AST
 * de JavaParser, sin compilar ni ejecutar el codigo.
 *
 * <p>Combina la herencia y las interfaces declaradas con las referencias de los atributos para
 * obtener relaciones de herencia, realizacion, asociacion y composicion. Tambien busca dependencias
 * en parametros, retornos y variables locales, omitiendolas cuando ya existe una relacion por
 * atributo entre los mismos tipos y en la misma direccion.</p>
 *
 * <p>Las relaciones por atributo incluyen una multiplicidad estimada, evidencias y un nivel de
 * confianza. La creacion con {@code new}, las asignaciones desde parametros y los setters aportan
 * indicios estructurales: son heuristicas, no una demostracion del ciclo de vida de los objetos.
 * Los destinos enumerados se representan como asociaciones, incluso si hay indicios de creacion.</p>
 *
 * <p>{@link ProjectParser} invoca este analizador despues de extraer los tipos y reune las relaciones
 * devueltas en el modelo final. Esta clase no serializa ni guarda el resultado.</p>
 */
final class RelationAnalyzer {
    /** Nombres simples de colecciones reconocidas para estimar la multiplicidad {@code 0..*}. */
    private static final Set<String> COLLECTIONS = Set.of(
            "Collection", "Iterable", "List", "Set", "Queue", "Deque", "Map",
            "ArrayList", "LinkedList", "Vector", "Stack", "HashSet", "LinkedHashSet",
            "TreeSet", "HashMap", "LinkedHashMap", "TreeMap", "ConcurrentMap",
            "ConcurrentHashMap");

    /** Resolutor que identifica referencias a tipos del proyecto usando el indice y el contexto. */
    private final TypeResolver resolver;

    /**
     * Crea el analizador con el resolutor compartido del proyecto.
     *
     * @param resolver resolutor usado para interpretar tipos presentes en los AST
     */
    RelationAnalyzer(TypeResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Calcula las relaciones, asociando cada tipo extraido con su declaracion y contexto en el AST.
     *
     * <p>Para cada tipo incorpora primero la herencia, despues las relaciones por atributos y,
     * finalmente, las dependencias de sus metodos y constructores. Elimina duplicados por origen,
     * destino, clase de relacion y miembro de procedencia, conservando la primera relacion.</p>
     *
     * @param types tipos del proyecto con sus miembros y referencias ya extraidos
     * @param units unidades de compilacion disponibles para consultar declaraciones y cuerpos
     * @return lista inmutable de relaciones en orden de insercion, vacia si no se encuentra ninguna
     */
    List<Relation> analyze(List<TypeInfo> types, List<CompilationUnit> units) {
        Map<String, TypeInfo> typesById = new LinkedHashMap<>();
        types.forEach(type -> typesById.put(type.id(), type));
        Map<String, DeclarationContext> declarations = new LinkedHashMap<>();
        for (CompilationUnit unit : units) {
            ResolutionContext base = ProjectParser.contextOf(unit);
            for (TypeDeclaration<?> declaration : unit.getTypes()) {
                register(declaration, null, base, declarations);
            }
        }

        Map<RelationKey, Relation> relations = new LinkedHashMap<>();
        Set<TypePair> structuralPairs = new LinkedHashSet<>();
        for (TypeInfo owner : types) {
            addGeneralizations(owner, typesById, relations);
            DeclarationContext declaration = declarations.get(owner.id());
            if (declaration == null) continue;
            for (FieldInfo field : owner.fields()) {
                for (String targetId : projectTypes(field.type())) {
                    Evidence evidence = inspectField(declaration.declaration(), declaration.context(), field,
                            targetId, typesById);
                    TypeInfo target = typesById.get(targetId);
                    RelationKind kind = target != null && target.kind() == TypeKind.ENUM
                        ? RelationKind.ASSOCIATION : fieldKind(evidence);
                    boolean requiredByConstructor = assignedInConstructor(
                        declaration.declaration(), declaration.context(), field, targetId, typesById);
                    String multiplicity = multiplicity(field.type(), requiredByConstructor);
                        Confidence confidence = kind == RelationKind.ASSOCIATION ? Confidence.LOW
                            : evidence.createdWithNew() && evidence.assignedFromParameter()
                                ? Confidence.MEDIUM : Confidence.HIGH;
                    add(relations, new Relation(kind, owner.id(), targetId, field.name(), multiplicity,
                            confidence, evidence));
                    structuralPairs.add(new TypePair(owner.id(), targetId));
                }
            }
            addDependencies(owner, declaration, structuralPairs, relations);
        }
        return List.copyOf(relations.values());
    }

    /**
     * Registra una declaracion y sus tipos anidados con el contexto necesario para resolver nombres.
     *
     * @param declaration declaracion de tipo que se registra
     * @param enclosingId identificador del tipo contenedor, o {@code null} para un tipo de primer nivel
     * @param base contexto de paquete, imports y tipos contenedores heredado
     * @param declarations mapa de salida, indexado por identificador completo del tipo
     */
    private void register(TypeDeclaration<?> declaration, String enclosingId, ResolutionContext base,
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
            if (member instanceof TypeDeclaration<?> nested) {
                register(nested, id, context, declarations);
            }
        }
    }

    /**
     * Incorpora las relaciones de herencia y realizacion declaradas hacia tipos del proyecto.
     *
     * @param type tipo de origen cuya superclase e interfaces se examinan
     * @param typesById tipos del proyecto por identificador, para consultar la clase de destino
     * @param relations acumulador de relaciones sin duplicados
     */
    private static void addGeneralizations(TypeInfo type, Map<String, TypeInfo> typesById,
                                           Map<RelationKey, Relation> relations) {
        if (type.superclass() != null && type.superclass().projectType()) {
            add(relations, generalization(type.id(), type.superclass().qualifiedName(), RelationKind.INHERITANCE));
        }
        for (TypeRef implemented : type.interfaces()) {
            if (!implemented.projectType()) continue;
            TypeInfo target = typesById.get(implemented.qualifiedName());
            RelationKind kind = type.kind() == TypeKind.INTERFACE ? RelationKind.INHERITANCE
                    : RelationKind.REALIZATION;
            if (target != null && target.kind() != TypeKind.INTERFACE && type.kind() == TypeKind.INTERFACE) {
                kind = RelationKind.REALIZATION;
            }
            add(relations, generalization(type.id(), implemented.qualifiedName(), kind));
        }
    }

    /**
     * Construye una relacion declarativa sin miembro ni multiplicidad, con confianza alta.
     *
     * @param from identificador del tipo de origen
     * @param to identificador del tipo de destino
     * @param kind clase de relacion de herencia o realizacion
     * @return relacion sin indicios de creacion o asignacion de objetos
     */
    private static Relation generalization(String from, String to, RelationKind kind) {
        return new Relation(kind, from, to, null, null, Confidence.HIGH,
                new Evidence(false, false, false, false, null));
    }

    /**
     * Reune los indicios de creacion y asignacion asociados a un atributo y a uno de sus tipos destino.
     *
     * <p>Examina su inicializador y los cuerpos de constructores y metodos. Considera como posible
     * setter un metodo publico cuyo nombre empieza por {@code set}.</p>
     *
     * @param declaration declaracion del tipo propietario del atributo
     * @param context contexto para resolver los tipos de la declaracion
     * @param field atributo que se inspecciona
     * @param targetId identificador del tipo del proyecto que se busca como destino
     * @param typesById tipos del proyecto, usados para reconocer tambien creaciones de subclases
     * @return evidencias acumuladas, incluida la presencia de inicializacion en la declaracion
     */
    private Evidence inspectField(TypeDeclaration<?> declaration, ResolutionContext context,
                                  FieldInfo field, String targetId, Map<String, TypeInfo> typesById) {
        boolean created = false;
        boolean assignedFromParameter = false;
        boolean exposedBySetter = false;

        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration fields) {
                for (VariableDeclarator variable : fields.getVariables()) {
                    if (variable.getNameAsString().equals(field.name()) && variable.getInitializer().isPresent()) {
                        created |= containsCreation(variable.getInitializer().get(), context, targetId, typesById);
                    }
                }
            } else if (member instanceof ConstructorDeclaration constructor) {
                BodyEvidence body = inspectBody(constructor.getBody(), constructor.getParameters(),
                    context, field.name(), targetId, false, typesById);
                created |= body.created();
                assignedFromParameter |= body.assignedFromParameter();
            } else if (member instanceof MethodDeclaration method && method.getBody().isPresent()) {
                BodyEvidence body = inspectBody(method.getBody().get(), method.getParameters(),
                        context, field.name(), targetId,
                        method.getAccessSpecifier() == com.github.javaparser.ast.AccessSpecifier.PUBLIC
                        && method.getNameAsString().startsWith("set"), typesById);
                created |= body.created();
                assignedFromParameter |= body.assignedFromParameter();
                exposedBySetter |= body.exposedBySetter();
            }
        }
        return new Evidence(created, assignedFromParameter, field.initializedInline(), exposedBySetter, null);
    }

    /**
     * Busca en algun constructor indicios de que el atributo recibe un objeto desde un parametro,
     * mediante asignacion o como argumento de una llamada sobre el atributo.
     *
     * @param declaration declaracion del tipo propietario
     * @param context contexto de resolucion de la declaracion
     * @param field atributo cuya inicializacion se examina
     * @param targetId identificador del tipo destino del atributo
     * @param typesById tipos del proyecto para interpretar las evidencias del cuerpo
     * @return {@code true} si algun constructor aporta ese indicio; no garantiza que todos lo hagan
     */
    private boolean assignedInConstructor(TypeDeclaration<?> declaration, ResolutionContext context,
                                          FieldInfo field, String targetId, Map<String, TypeInfo> typesById) {
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof ConstructorDeclaration constructor) {
                BodyEvidence body = inspectBody(constructor.getBody(), constructor.getParameters(),
                    context, field.name(), targetId, false, typesById);
                if (body.assignedFromParameter()) return true;
            }
        }
        return false;
    }

    /**
     * Examina asignaciones al atributo y argumentos de llamadas sobre el para rastrear origenes.
     *
     * <p>Relaciona parametros y variables locales con tipos creados o recibidos. El seguimiento de
     * variables locales usa sus inicializadores y alias simples; no realiza un analisis completo
     * de flujo de datos, ambitos ni ejecucion de metodos.</p>
     *
     * @param body cuerpo AST de un metodo o constructor
     * @param parameters parametros declarados por ese metodo o constructor
     * @param context contexto para resolver nombres de tipos
     * @param fieldName nombre del atributo examinado
     * @param targetId identificador del tipo destino que se busca
     * @param setter si el metodo se considera un posible setter publico
     * @param typesById tipos del proyecto para comprobar creaciones compatibles por herencia
     * @return indicios de creacion, recepcion desde parametros y exposicion por asignacion en setter
     */
    private BodyEvidence inspectBody(Node body, List<Parameter> parameters,
                                     ResolutionContext context, String fieldName, String targetId,
                                     boolean setter, Map<String, TypeInfo> typesById) {
        Map<String, Set<String>> parameterTypes = new LinkedHashMap<>();
        for (Parameter parameter : parameters) {
            parameterTypes.put(parameter.getNameAsString(), projectTypes(resolver.resolve(parameter.getType(), context)));
        }
        Map<String, Origins> localOrigins = new LinkedHashMap<>();
        for (VariableDeclarator variable : body.findAll(VariableDeclarator.class)) {
            Set<String> declaredTypes = projectTypes(resolver.resolve(variable.getType(), context));
            Origins origins = new Origins(new LinkedHashSet<>(), new LinkedHashSet<>());
            if (variable.getInitializer().isPresent()) {
                Expression initializer = variable.getInitializer().get();
                origins.created().addAll(createdTypes(initializer, context));
                if (initializer instanceof NameExpr name) {
                    origins.created().addAll(localOrigins.getOrDefault(name.getNameAsString(), Origins.EMPTY).created());
                    origins.parameters().addAll(parameterTypes.getOrDefault(name.getNameAsString(), Set.of()));
                    origins.parameters().addAll(localOrigins.getOrDefault(name.getNameAsString(), Origins.EMPTY).parameters());
                }
            }
            origins.created().retainAll(declaredTypes.isEmpty() ? origins.created() : declaredTypes);
            origins.parameters().retainAll(declaredTypes.isEmpty() ? origins.parameters() : declaredTypes);
            localOrigins.put(variable.getNameAsString(), origins);
        }

        boolean created = false;
        boolean assigned = false;
        boolean exposed = false;
        for (AssignExpr assignment : body.findAll(AssignExpr.class)) {
            if (!isField(assignment.getTarget(), fieldName)) continue;
            Origins origins = origins(assignment.getValue(), context, parameterTypes, localOrigins);
            created |= matchesTarget(origins.created(), targetId, typesById);
            assigned |= origins.parameters().contains(targetId);
            exposed |= setter && origins.parameters().contains(targetId);
        }
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            if (call.getScope().isEmpty() || !scopeName(call.getScope().get()).equals(fieldName)) continue;
            for (Expression argument : call.getArguments()) {
                Origins origins = origins(argument, context, parameterTypes, localOrigins);
                created |= matchesTarget(origins.created(), targetId, typesById);
                assigned |= origins.parameters().contains(targetId);
            }
        }
        return new BodyEvidence(created, assigned, exposed);
    }

    /**
     * Obtiene los tipos asociados a creaciones y parametros que pueden dar origen a una expresion.
     *
     * @param expression expresion asignada al atributo o pasada a una llamada sobre el
     * @param context contexto para interpretar las creaciones de objetos
     * @param parameterTypes tipos del proyecto asociados a cada nombre de parametro
     * @param localOrigins origenes previamente recogidos para las variables locales
     * @return conjuntos de origenes; para nombres simples incorpora parametros y alias locales
     */
    private Origins origins(Expression expression, ResolutionContext context,
                            Map<String, Set<String>> parameterTypes, Map<String, Origins> localOrigins) {
        Origins origins = new Origins(createdTypes(expression, context), new LinkedHashSet<>());
        if (expression instanceof NameExpr name) {
            origins.parameters().addAll(parameterTypes.getOrDefault(name.getNameAsString(), Set.of()));
            Origins local = localOrigins.get(name.getNameAsString());
            if (local != null) {
                origins.created().addAll(local.created());
                origins.parameters().addAll(local.parameters());
            }
        }
        return origins;
    }

    /**
     * Recoge tipos del proyecto presentes en las expresiones de creacion encontradas.
     *
     * <p>Incluye los argumentos genericos del tipo creado; no evalua la expresion.</p>
     *
     * @param expression expresion cuyo subarbol se recorre en busca de {@code new}
     * @param context contexto para resolver los tipos de las creaciones
     * @return conjunto de identificadores sin duplicados, vacio si no se reconocen tipos del proyecto
     */
    private Set<String> createdTypes(Expression expression, ResolutionContext context) {
        Set<String> targets = new LinkedHashSet<>();
        for (ObjectCreationExpr creation : expression.findAll(ObjectCreationExpr.class)) {
            targets.addAll(projectTypes(resolver.resolve(creation.getType(), context)));
        }
        return targets;
    }

    /**
     * Comprueba si una expresion contiene indicios de creacion compatibles con un tipo destino.
     *
     * @param expression expresion que se inspecciona
     * @param context contexto para resolver sus tipos
     * @param targetId identificador del tipo destino buscado
     * @param typesById tipos del proyecto usados para recorrer la cadena de superclases
     * @return {@code true} si se reconoce el destino o una subclase suya entre los tipos recogidos
     */
    private boolean containsCreation(Expression expression, ResolutionContext context, String targetId,
                                     Map<String, TypeInfo> typesById) {
        return matchesTarget(createdTypes(expression, context), targetId, typesById);
    }

    /**
     * Busca coincidencia con el destino recorriendo la cadena de superclases de cada candidato.
     *
     * <p>No recorre interfaces y evita ciclos mediante un registro de tipos visitados.</p>
     *
     * @param candidates identificadores de tipos candidatos
     * @param targetId identificador con el que se comparan los candidatos y sus superclases
     * @param typesById tipos del proyecto con su informacion de herencia
     * @return {@code true} si algun candidato coincide o hereda del destino por superclases
     */
    private static boolean matchesTarget(Set<String> candidates, String targetId,
                                         Map<String, TypeInfo> typesById) {
        for (String candidate : candidates) {
            Set<String> visited = new LinkedHashSet<>();
            String currentId = candidate;
            while (currentId != null && visited.add(currentId)) {
                if (currentId.equals(targetId)) return true;
                TypeInfo current = typesById.get(currentId);
                TypeRef superclass = current == null ? null : current.superclass();
                currentId = superclass != null && superclass.projectType()
                        ? superclass.qualifiedName() : null;
            }
        }
        return false;
    }

    /**
     * Incorpora dependencias por parametros, retornos y variables locales de metodos y constructores.
     *
     * @param owner tipo de origen de las dependencias
     * @param declaration declaracion AST y contexto del tipo de origen
     * @param structuralPairs pares dirigidos que ya tienen una relacion por atributo
     * @param relations acumulador de relaciones sin duplicados
     */
    private void addDependencies(TypeInfo owner, DeclarationContext declaration, Set<TypePair> structuralPairs,
                                 Map<RelationKey, Relation> relations) {
        for (BodyDeclaration<?> member : declaration.declaration().getMembers()) {
            if (member instanceof MethodDeclaration method) {
                for (Parameter parameter : method.getParameters()) {
                    addDependency(owner.id(), method.getNameAsString() + "(" + parameter.getNameAsString() + ")",
                            resolver.resolve(parameter.getType(), declaration.context()), "parameter",
                            structuralPairs, relations);
                }
                addDependency(owner.id(), method.getNameAsString() + "(return)",
                        resolver.resolve(method.getType(), declaration.context()), "return",
                        structuralPairs, relations);
                method.getBody().ifPresent(body -> addLocalDependencies(owner.id(), method.getNameAsString(),
                        body, declaration.context(), structuralPairs, relations));
            } else if (member instanceof ConstructorDeclaration constructor) {
                for (Parameter parameter : constructor.getParameters()) {
                    addDependency(owner.id(), "<init>(" + parameter.getNameAsString() + ")",
                            resolver.resolve(parameter.getType(), declaration.context()), "parameter",
                            structuralPairs, relations);
                }
                addLocalDependencies(owner.id(), "<init>", constructor.getBody(), declaration.context(),
                        structuralPairs, relations);
            }
        }
    }

    /**
     * Incorpora dependencias a partir de los tipos declarados por variables encontradas en un cuerpo.
     *
     * @param from identificador del tipo de origen
     * @param callable nombre del metodo, o {@code <init>} para un constructor
     * @param body subarbol AST donde se buscan declaraciones de variables
     * @param context contexto para resolver sus tipos
     * @param structuralPairs pares dirigidos con relacion por atributo, que no generan dependencia
     * @param relations acumulador de relaciones sin duplicados
     */
    private void addLocalDependencies(String from, String callable, Node body, ResolutionContext context,
                                      Set<TypePair> structuralPairs, Map<RelationKey, Relation> relations) {
        for (VariableDeclarator variable : body.findAll(VariableDeclarator.class)) {
            addDependency(from, callable + "(" + variable.getNameAsString() + ")",
                    resolver.resolve(variable.getType(), context), "local", structuralPairs, relations);
        }
    }

    /**
     * Anade dependencias para los tipos del proyecto presentes en una referencia y sus genericos.
     *
     * @param from identificador del tipo de origen
     * @param via descripcion del metodo o constructor y del parametro, retorno o variable de procedencia
     * @param type referencia cuyo tipo y argumentos genericos se examinan
     * @param usedIn categoria de evidencia: {@code parameter}, {@code return} o {@code local}
     * @param structuralPairs pares dirigidos que se excluyen por tener ya una relacion por atributo
     * @param relations acumulador donde se insertan las dependencias con confianza alta
     */
    private static void addDependency(String from, String via, TypeRef type, String usedIn,
                                      Set<TypePair> structuralPairs, Map<RelationKey, Relation> relations) {
        for (String target : projectTypes(type)) {
            if (structuralPairs.contains(new TypePair(from, target))) continue;
            add(relations, new Relation(RelationKind.DEPENDENCY, from, target, via, null, Confidence.HIGH,
                    new Evidence(false, false, false, false, usedIn)));
        }
    }

    /**
     * Propone composicion si hay indicios de creacion con {@code new}; en otro caso, asociacion.
     *
     * @param evidence evidencias acumuladas para un atributo y un destino
     * @return clase de relacion propuesta, sin aplicar la excepcion de los destinos enumerados
     */
    private static RelationKind fieldKind(Evidence evidence) {
        if (evidence.createdWithNew()) return RelationKind.COMPOSITION;
        return RelationKind.ASSOCIATION;
    }

    /**
     * Estima la multiplicidad a partir del tipo declarado y de las evidencias de constructor.
     *
     * @param type tipo del atributo, con sus dimensiones de array
     * @param requiredByConstructor si se detecto recepcion desde un parametro en algun constructor
     * @return {@code 0..*} para arrays o colecciones reconocidas, {@code 0..1} para {@code Optional};
     *         para otras referencias, {@code 1} si hay evidencia de constructor o {@code 0..1} si no
     */
    private static String multiplicity(TypeRef type, boolean requiredByConstructor) {
        if (type.arrayDimensions() > 0 || COLLECTIONS.contains(type.name())) return "0..*";
        if (type.name().equals("Optional")) return "0..1";
        return requiredByConstructor ? "1" : "0..1";
    }

    /**
     * Recoge recursivamente los tipos del proyecto presentes en una referencia y sus genericos.
     *
     * @param type referencia de tipo que se examina
     * @return identificadores completos sin duplicados, en orden de descubrimiento
     */
    private static Set<String> projectTypes(TypeRef type) {
        Set<String> result = new LinkedHashSet<>();
        if (type.projectType() && type.qualifiedName() != null) result.add(type.qualifiedName());
        for (TypeRef argument : type.arguments()) result.addAll(projectTypes(argument));
        return result;
    }

    /**
     * Reconoce una referencia sintactica al atributo por nombre simple o mediante {@code this}.
     *
     * @param expression destino de una asignacion
     * @param fieldName nombre del atributo buscado
     * @return {@code true} para {@code campo} o {@code this.campo} con ese nombre, sin resolver sombras
     */
    private static boolean isField(Expression expression, String fieldName) {
        if (expression instanceof NameExpr name) return name.getNameAsString().equals(fieldName);
        if (expression instanceof FieldAccessExpr access) {
            return access.getNameAsString().equals(fieldName) && access.getScope().isThisExpr();
        }
        return false;
    }

    /**
     * Extrae el nombre simple del receptor de una llamada cuando es directo o usa {@code this}.
     *
     * @param expression expresion receptora de una llamada a metodo
     * @return nombre para {@code campo} o {@code this.campo}, o cadena vacia para otras expresiones
     */
    private static String scopeName(Expression expression) {
        if (expression instanceof NameExpr name) return name.getNameAsString();
        if (expression instanceof FieldAccessExpr access && access.getScope().isThisExpr()) {
            return access.getNameAsString();
        }
        return "";
    }

    /**
     * Inserta una relacion solo si no existe otra con la misma clave.
     *
     * @param relations acumulador indexado por origen, destino, clase de relacion y procedencia
     * @param relation relacion candidata; no reemplaza la primera aunque difieran sus evidencias
     */
    private static void add(Map<RelationKey, Relation> relations, Relation relation) {
        relations.putIfAbsent(new RelationKey(relation.from(), relation.to(), relation.kind(), relation.via()), relation);
    }

    /**
     * Vincula una declaracion AST con la informacion necesaria para resolver sus tipos.
     *
     * @param declaration declaracion del tipo
     * @param context contexto de paquete, imports y tipos contenedores
     */
    private record DeclarationContext(TypeDeclaration<?> declaration, ResolutionContext context) { }
    /**
     * Clave que identifica una relacion y permite eliminar duplicados.
     *
     * @param from identificador del tipo de origen
     * @param to identificador del tipo de destino
     * @param kind clase de relacion UML
     * @param via miembro o uso de procedencia, o {@code null} en herencia y realizacion
     */
    private record RelationKey(String from, String to, RelationKind kind, String via) { }
    /**
     * Par dirigido de tipos con una relacion por atributo, usado para omitir dependencias redundantes.
     *
     * @param from identificador del tipo de origen
     * @param to identificador del tipo de destino
     */
    private record TypePair(String from, String to) { }
    /**
     * Resume los indicios encontrados en el cuerpo de un metodo o constructor para un atributo.
     *
     * @param created si se reconocio una creacion compatible vinculada al atributo
     * @param assignedFromParameter si se detecto recepcion desde un parametro del tipo destino
     * @param exposedBySetter si esa recepcion se produjo por asignacion en un posible setter publico
     */
    private record BodyEvidence(boolean created, boolean assignedFromParameter, boolean exposedBySetter) { }

    /**
     * Agrupa los identificadores de tipos asociados al origen de una expresion o variable local.
     *
     * @param created tipos vinculados a expresiones de creacion; mutable salvo en {@link #EMPTY}
     * @param parameters tipos vinculados a parametros; mutable salvo en {@link #EMPTY}
     */
    private record Origins(Set<String> created, Set<String> parameters) {
        /** Origenes vacios e inmutables usados cuando no hay informacion de una variable local. */
        private static final Origins EMPTY = new Origins(Set.of(), Set.of());
    }
}
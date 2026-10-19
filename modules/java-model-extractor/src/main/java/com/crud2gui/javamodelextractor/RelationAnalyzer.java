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

/** Deriva las relaciones UML de las declaraciones y usos del codigo fuente. */
final class RelationAnalyzer {
    private static final Set<String> COLLECTIONS = Set.of(
            "Collection", "Iterable", "List", "Set", "Queue", "Deque", "Map",
            "ArrayList", "LinkedList", "Vector", "Stack", "HashSet", "LinkedHashSet",
            "TreeSet", "HashMap", "LinkedHashMap", "TreeMap", "ConcurrentMap",
            "ConcurrentHashMap");

    private final TypeResolver resolver;

    RelationAnalyzer(TypeResolver resolver) {
        this.resolver = resolver;
    }

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
                    Evidence evidence = inspectField(declaration.declaration(), declaration.context(), field, targetId);
                    TypeInfo target = typesById.get(targetId);
                    RelationKind kind = target != null && target.kind() == TypeKind.ENUM
                        ? RelationKind.ASSOCIATION : fieldKind(evidence);
                    boolean requiredByConstructor = assignedInConstructor(
                        declaration.declaration(), declaration.context(), field, targetId);
                    String multiplicity = multiplicity(field.type(), requiredByConstructor);
                    add(relations, new Relation(kind, owner.id(), targetId, field.name(), multiplicity,
                            evidence.createdWithNew() && evidence.assignedFromParameter()
                                    ? Confidence.MEDIUM : Confidence.HIGH,
                            evidence));
                    structuralPairs.add(new TypePair(owner.id(), targetId));
                }
            }
            addDependencies(owner, declaration, structuralPairs, relations);
        }
        return List.copyOf(relations.values());
    }

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

    private static Relation generalization(String from, String to, RelationKind kind) {
        return new Relation(kind, from, to, null, null, Confidence.HIGH,
                new Evidence(false, false, false, false, null));
    }

    private Evidence inspectField(TypeDeclaration<?> declaration, ResolutionContext context,
                                  FieldInfo field, String targetId) {
        boolean created = false;
        boolean assignedFromParameter = false;
        boolean exposedBySetter = false;

        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof FieldDeclaration fields) {
                for (VariableDeclarator variable : fields.getVariables()) {
                    if (variable.getNameAsString().equals(field.name()) && variable.getInitializer().isPresent()) {
                        created |= containsCreation(variable.getInitializer().get(), context, targetId);
                    }
                }
            } else if (member instanceof ConstructorDeclaration constructor) {
                BodyEvidence body = inspectBody(constructor.getBody(), constructor.getParameters(),
                        context, field.name(), targetId, false);
                created |= body.created();
                assignedFromParameter |= body.assignedFromParameter();
            } else if (member instanceof MethodDeclaration method && method.getBody().isPresent()) {
                BodyEvidence body = inspectBody(method.getBody().get(), method.getParameters(),
                        context, field.name(), targetId,
                        method.getAccessSpecifier() == com.github.javaparser.ast.AccessSpecifier.PUBLIC
                                && method.getNameAsString().startsWith("set"));
                created |= body.created();
                assignedFromParameter |= body.assignedFromParameter();
                exposedBySetter |= body.exposedBySetter();
            }
        }
        return new Evidence(created, assignedFromParameter, field.initializedInline(), exposedBySetter, null);
    }

    private boolean assignedInConstructor(TypeDeclaration<?> declaration, ResolutionContext context,
                                          FieldInfo field, String targetId) {
        for (BodyDeclaration<?> member : declaration.getMembers()) {
            if (member instanceof ConstructorDeclaration constructor) {
                BodyEvidence body = inspectBody(constructor.getBody(), constructor.getParameters(),
                        context, field.name(), targetId, false);
                if (body.assignedFromParameter()) return true;
            }
        }
        return false;
    }

    private BodyEvidence inspectBody(Node body, List<Parameter> parameters,
                                     ResolutionContext context, String fieldName, String targetId,
                                     boolean setter) {
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
            created |= origins.created().contains(targetId);
            assigned |= origins.parameters().contains(targetId);
            exposed |= setter && origins.parameters().contains(targetId);
        }
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            if (call.getScope().isEmpty() || !scopeName(call.getScope().get()).equals(fieldName)) continue;
            for (Expression argument : call.getArguments()) {
                Origins origins = origins(argument, context, parameterTypes, localOrigins);
                created |= origins.created().contains(targetId);
                assigned |= origins.parameters().contains(targetId);
            }
        }
        return new BodyEvidence(created, assigned, exposed);
    }

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

    private Set<String> createdTypes(Expression expression, ResolutionContext context) {
        Set<String> targets = new LinkedHashSet<>();
        for (ObjectCreationExpr creation : expression.findAll(ObjectCreationExpr.class)) {
            targets.addAll(projectTypes(resolver.resolve(creation.getType(), context)));
        }
        return targets;
    }

    private boolean containsCreation(Expression expression, ResolutionContext context, String targetId) {
        return createdTypes(expression, context).contains(targetId);
    }

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

    private void addLocalDependencies(String from, String callable, Node body, ResolutionContext context,
                                      Set<TypePair> structuralPairs, Map<RelationKey, Relation> relations) {
        for (VariableDeclarator variable : body.findAll(VariableDeclarator.class)) {
            addDependency(from, callable + "(" + variable.getNameAsString() + ")",
                    resolver.resolve(variable.getType(), context), "local", structuralPairs, relations);
        }
    }

    private static void addDependency(String from, String via, TypeRef type, String usedIn,
                                      Set<TypePair> structuralPairs, Map<RelationKey, Relation> relations) {
        for (String target : projectTypes(type)) {
            if (structuralPairs.contains(new TypePair(from, target))) continue;
            add(relations, new Relation(RelationKind.DEPENDENCY, from, target, via, null, Confidence.HIGH,
                    new Evidence(false, false, false, false, usedIn)));
        }
    }

    private static RelationKind fieldKind(Evidence evidence) {
        if (evidence.createdWithNew()) return RelationKind.COMPOSITION;
        if (evidence.assignedFromParameter()) return RelationKind.AGGREGATION;
        return RelationKind.ASSOCIATION;
    }

    private static String multiplicity(TypeRef type, boolean requiredByConstructor) {
        if (type.arrayDimensions() > 0 || COLLECTIONS.contains(type.name())) return "0..*";
        if (type.name().equals("Optional")) return "0..1";
        return requiredByConstructor ? "1" : "0..1";
    }

    private static Set<String> projectTypes(TypeRef type) {
        Set<String> result = new LinkedHashSet<>();
        if (type.projectType() && type.qualifiedName() != null) result.add(type.qualifiedName());
        for (TypeRef argument : type.arguments()) result.addAll(projectTypes(argument));
        return result;
    }

    private static boolean isField(Expression expression, String fieldName) {
        if (expression instanceof NameExpr name) return name.getNameAsString().equals(fieldName);
        if (expression instanceof FieldAccessExpr access) {
            return access.getNameAsString().equals(fieldName) && access.getScope().isThisExpr();
        }
        return false;
    }

    private static String scopeName(Expression expression) {
        if (expression instanceof NameExpr name) return name.getNameAsString();
        if (expression instanceof FieldAccessExpr access && access.getScope().isThisExpr()) {
            return access.getNameAsString();
        }
        return "";
    }

    private static void add(Map<RelationKey, Relation> relations, Relation relation) {
        relations.putIfAbsent(new RelationKey(relation.from(), relation.to(), relation.kind(), relation.via()), relation);
    }

    private record DeclarationContext(TypeDeclaration<?> declaration, ResolutionContext context) { }
    private record RelationKey(String from, String to, RelationKind kind, String via) { }
    private record TypePair(String from, String to) { }
    private record BodyEvidence(boolean created, boolean assignedFromParameter, boolean exposedBySetter) { }

    private record Origins(Set<String> created, Set<String> parameters) {
        private static final Origins EMPTY = new Origins(Set.of(), Set.of());
    }
}
package com.crud2gui.modelvalidator;

import com.crud2gui.model.Analysis;
import com.crud2gui.model.Confidence;
import com.crud2gui.model.ConstructorInfo;
import com.crud2gui.model.EnumConstantInfo;
import com.crud2gui.model.FieldInfo;
import com.crud2gui.model.MethodInfo;
import com.crud2gui.model.Modifier;
import com.crud2gui.model.ParameterInfo;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.model.Relation;
import com.crud2gui.model.RelationKind;
import com.crud2gui.model.SourceInfo;
import com.crud2gui.model.TypeInfo;
import com.crud2gui.model.TypeRef;
import com.crud2gui.model.Visibility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Compara un modelo esperado con el modelo extraido de una implementacion Java. */
public final class ModelValidator {

    private static final Pattern OPTIONAL_METHOD = Pattern.compile(
            "^OPCIONAL:\\s*([\\w.$]+)\\.([\\w$]+)\\((.*)\\)$");
        private static final Pattern OPTIONAL_FIELD = Pattern.compile(
            "^OPCIONAL:\\s*([\\w.$]+)\\.([\\w$]+)$");
        private static final Pattern UNSPECIFIED_FIELD_NAME = Pattern.compile(
            "^NOMBRE_ATRIBUTO_NO_ESPECIFICADO:\\s*([\\w.$]+)\\.([\\w$]+)$");
            private static final Pattern COLLECTION_OPERATIONS = Pattern.compile(
                "^OPERACIONES_LISTA:\\s*([\\w.$]+)\\.([\\w$]+)\\(([^)]*)\\)$");
                private static final Set<String> MUTABLE_COLLECTION_TYPES = Set.of(
                    "List", "Set", "Collection", "ArrayList", "LinkedList", "HashSet", "LinkedHashSet", "TreeSet",
                    "Vector");

    /** Evita crear instancias de esta clase de utilidades. */
    private ModelValidator() { }

    /**
     * Comprueba que la implementacion satisface los elementos especificados en el modelo esperado.
     *
     * <p>El modelo esperado se interpreta como contrato minimo: se exigen sus tipos, atributos,
     * constructores, metodos y relaciones, pero se permiten miembros adicionales. Las visibilidades
     * nulas significan que el enunciado no las especifica. Se ignoran metadatos de origen, analisis,
     * layout, lanzadores y dependencias por uso local. La diferencia entre composicion y agregacion
     * se informa como advertencia porque no siempre puede determinarse a partir del codigo Java.</p>
     *
     * @param expected modelo derivado del enunciado
     * @param actual modelo derivado del codigo Java
     * @return resultado con errores bloqueantes y advertencias
     * @throws NullPointerException si alguno de los modelos es nulo
     */
    public static Result validate(ProjectModel expected, ProjectModel actual) {
        return validate(expected, actual, ValidationOptions.defaults());
    }

    public static Result validate(ProjectModel expected, ProjectModel actual, ValidationOptions options) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(options, "options");

        List<Finding> findings = new ArrayList<>();
        Set<String> optionalTypes = optionalTypes(expected.source());
        Set<String> optionalFields = optionalFields(expected.source());
        Set<String> optionalMethods = optionalMethods(expected.source());
        Set<String> unspecifiedFieldNames = unspecifiedFieldNames(expected.source());
        Map<String, TypeInfo> expectedTypes = indexTypes(expected.types(), "expected", findings);
        List<CollectionOperationRequirement> collectionOperations =
            collectionOperations(expected.source(), expectedTypes);
        Set<String> entryPoints = entryPoints(actual);
        Set<String> launcherTypes = actual.types().stream()
            .filter(TypeInfo::hasMainMethod)
            .map(type -> typeKey(type.id()))
            .collect(java.util.stream.Collectors.toSet());
        actual.types().stream().filter(TypeInfo::hasMainMethod)
            .forEach(type -> add(findings, Severity.INFO, "LAUNCHER_EXCLUDED", type.name(),
                "Clase lanzadora con metodo main; excluida de la validacion de dominio y del CRUD."));
        List<TypeInfo> actualDomainTypes = actual.types().stream()
                .filter(type -> !type.hasMainMethod())
                .filter(type -> !entryPoints.contains(typeKey(type.id())))
                .toList();
        Map<String, TypeInfo> actualTypes = indexTypes(actualDomainTypes, "actual", findings);
        List<RelationshipDecision> relationshipDecisions = new ArrayList<>();

        for (Map.Entry<String, TypeInfo> entry : expectedTypes.entrySet()) {
            String key = entry.getKey();
            TypeInfo expectedType = entry.getValue();
            TypeInfo actualType = actualTypes.get(key);
            if (actualType == null) {
                if (!options.validatesType(expectedType.kind())) continue;
                if (launcherTypes.contains(key)) continue;
                if (!optionalTypes.contains(key)) {
                    String code = expectedType.kind() == null
                        ? "MISSING_TYPE" : "MISSING_" + expectedType.kind().name();
                    String message = expectedType.kind() == null
                        ? "No se encontro el tipo requerido en la implementacion."
                        : "No se encontro la declaracion esperada de tipo " + expectedType.kind()
                            + " en la implementacion.";
                    add(findings, Severity.ERROR, code, expectedType.name(), message);
                }
                continue;
            }
                compareType(expectedType, actualType, actualTypes, collectionOperations,
                    optionalFields, optionalMethods, unspecifiedFieldNames, options, findings);
        }

        for (Map.Entry<String, TypeInfo> entry : actualTypes.entrySet()) {
            if (!expectedTypes.containsKey(entry.getKey()) && options.validatesType(entry.getValue().kind())) {
                if (optionalTypes.contains(entry.getKey())) {
                    add(findings, Severity.INFO, "OPTIONAL_TYPE_PRESENT", entry.getValue().name(),
                            "Tipo opcional implementado; se excluye de los errores por tipos adicionales.");
                } else {
                    add(findings, Severity.ERROR, "ADDITIONAL_TYPE", entry.getValue().name(),
                            "Tipo no contemplado en el modelo esperado.");
                }
            }
        }

        if (options.collectionOperations()) {
            validateCollectionOperations(collectionOperations, expectedTypes, actualTypes, findings);
        }
        if (options.relationships()) {
            compareRelations(expected.relations(), actual.relations(), entryPoints, actualTypes.keySet(), optionalTypes,
                    findings, relationshipDecisions);
        }
        return new Result(findings, relationshipDecisions);
    }

    /**
        * Compara la estructura de un tipo y sus miembros requeridos con la implementacion.
     * @param expected tipo descrito por el modelo esperado
     * @param actual tipo correspondiente encontrado en la implementacion
     * @param actualTypes tipos de la implementacion indexados por nombre simple
     * @param collectionOperations operaciones de coleccion exigidas por el modelo esperado
     * @param optionalFields atributos cuya ausencia esta permitida
     * @param optionalMethods metodos cuya ausencia esta permitida
    * @param unspecifiedFieldNames nombres de atributo que el enunciado no fija
     * @param findings lista donde se agregan las diferencias detectadas
     */
    private static void compareType(TypeInfo expected, TypeInfo actual, Map<String, TypeInfo> actualTypes,
                                    List<CollectionOperationRequirement> collectionOperations,
                                    Set<String> optionalFields, Set<String> optionalMethods,
                                    Set<String> unspecifiedFieldNames,
                                    ValidationOptions options,
                                    List<Finding> findings) {
        String path = expected.name();
        if (options.validatesType(expected.kind()) && expected.kind() != actual.kind()) {
            add(findings, Severity.ERROR, "TYPE_KIND", path,
                    "Tipo esperado " + expected.kind() + ", implementado " + actual.kind() + ".");
        }
        if (options.validatesType(expected.kind())) {
            compareVisibility(expected.visibility(), actual.visibility(), path, "TYPE_VISIBILITY", findings);
        }
        List<Modifier> expectedTypeModifiers = expected.modifiers();
        if (!options.abstractness() && expected.kind() == com.crud2gui.model.TypeKind.CLASS) {
            expectedTypeModifiers = expectedTypeModifiers.stream()
                    .filter(modifier -> modifier != Modifier.ABSTRACT).toList();
        }
        if (options.validatesType(expected.kind())) {
            compareModifiers(expectedTypeModifiers, actual.modifiers(), path, "TYPE_MODIFIER", findings);
        }

        if (options.relationships() && expected.superclass() != null
                && !typeName(expected.superclass()).equals(typeName(actual.superclass()))) {
            add(findings, Severity.ERROR, "SUPERCLASS", path,
                    "Superclase esperada " + typeName(expected.superclass()) + ", implementada "
                            + typeName(actual.superclass()) + ".");
        }
        if (options.relationships()) {
            Set<String> actualInterfaces = actual.interfaces().stream().map(ModelValidator::typeName)
                .collect(java.util.stream.Collectors.toSet());
            for (TypeRef expectedInterface : expected.interfaces()) {
            if (!actualInterfaces.contains(typeName(expectedInterface))) {
                add(findings, Severity.ERROR, "MISSING_INTERFACE", path,
                    "Falta la interfaz " + typeName(expectedInterface) + ".");
            }
            }
        }

        Map<String, String> fieldAliases = Map.of();
        if (options.attributes()) {
        Map<String, FieldInfo> actualFields = new LinkedHashMap<>();
        actual.fields().forEach(field -> actualFields.put(field.name(), field));
        fieldAliases = resolveUnspecifiedFieldNames(expected, actual, actualFields,
                unspecifiedFieldNames);
        Set<String> expectedFieldNames = new HashSet<>();
        for (FieldInfo expectedField : expected.fields()) {
            expectedFieldNames.add(expectedField.name());
            String actualFieldName = fieldAliases.getOrDefault(expectedField.name(), expectedField.name());
            FieldInfo actualField = actualFields.get(actualFieldName);
            String fieldPath = path + "." + actualFieldName;
            if (unspecifiedFieldNames.contains(fieldKey(expected.name(), expectedField.name()))) {
                String message = actualField == null
                    ? "El enunciado no fija el nombre y no se encontro un unico atributo compatible."
                    : "El enunciado no fija el identificador; se acepta el campo " + actualFieldName
                        + " por su tipo y semantica esperados.";
                add(findings, Severity.INFO, "FIELD_NAME_UNSPECIFIED", fieldPath, message);
            }
            if (actualField == null) {
                if (!optionalFields.contains(fieldKey(expected.name(), expectedField.name()))) {
                    add(findings, Severity.ERROR, "MISSING_FIELD", fieldPath,
                            "Falta el atributo requerido.");
                }
                continue;
            }
            if (!typeName(expectedField.type()).equals(typeName(actualField.type()))) {
                add(findings, Severity.ERROR, "FIELD_TYPE", fieldPath,
                        "Tipo esperado " + typeName(expectedField.type()) + ", implementado "
                                + typeName(actualField.type()) + ".");
            }
            compareVisibility(expectedField.visibility(), actualField.visibility(), fieldPath,
                    "FIELD_VISIBILITY", findings);
            compareModifiers(expectedField.modifiers(), actualField.modifiers(), fieldPath,
                    "FIELD_MODIFIER", findings);
        }
        for (FieldInfo actualField : actual.fields()) {
            if (!expectedFieldNames.contains(actualField.name())
                    && !fieldAliases.containsValue(actualField.name())) {
                add(findings, Severity.ERROR, "ADDITIONAL_FIELD", path + "." + actualField.name(),
                    "Atributo no contemplado en el modelo esperado.");
                } else if (options.methods() && actualField.visibility() == Visibility.PRIVATE
                    && !hasGetter(actual, actualField)) {
                add(findings, Severity.WARNING, "MISSING_GETTER", path + "." + actualField.name(),
                        "El atributo privado no tiene un getter publico que permita consultarlo.");
            }
        }
            }

            if (options.constructors()) compareConstructors(expected, actual, findings);
            if (options.methods()) {
                compareMethods(expected, actual, actualTypes, collectionOperations, optionalMethods,
                    fieldAliases, findings);
            }
            if (options.enumConstants()) compareEnumConstants(expected, actual, findings);
    }

    private static Map<String, String> resolveUnspecifiedFieldNames(TypeInfo expected, TypeInfo actual,
                                                                     Map<String, FieldInfo> actualFields,
                                                                     Set<String> unspecifiedFieldNames) {
        Set<String> matchedActualFields = expected.fields().stream()
                .map(FieldInfo::name)
                .filter(actualFields::containsKey)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, List<FieldInfo>> candidatesByExpectedField = new LinkedHashMap<>();
        Map<String, Integer> candidateUseCounts = new LinkedHashMap<>();
        for (FieldInfo expectedField : expected.fields()) {
            if (!unspecifiedFieldNames.contains(fieldKey(expected.name(), expectedField.name()))
                    || actualFields.containsKey(expectedField.name())) {
                continue;
            }
            List<FieldInfo> candidates = actual.fields().stream()
                    .filter(field -> !matchedActualFields.contains(field.name()))
                    .filter(field -> typeName(field.type()).equals(typeName(expectedField.type())))
                    .toList();
            candidatesByExpectedField.put(expectedField.name(), candidates);
            for (FieldInfo candidate : candidates) {
                candidateUseCounts.merge(candidate.name(), 1, Integer::sum);
            }
        }

        Map<String, String> aliases = new LinkedHashMap<>();
        candidatesByExpectedField.forEach((expectedName, candidates) -> {
            if (candidates.size() == 1 && candidateUseCounts.get(candidates.get(0).name()) == 1) {
                aliases.put(expectedName, candidates.get(0).name());
            }
        });
        return aliases;
    }

    /**
        * Comprueba que cada constructor requerido exista con la misma firma y visibilidad.
     * @param expected tipo cuyos constructores exige el contrato
     * @param actual tipo implementado que se inspecciona
     * @param findings lista donde se agregan constructores ausentes o con visibilidad distinta
     */
    private static void compareConstructors(TypeInfo expected, TypeInfo actual, List<Finding> findings) {
        List<String> actualSignatures = actual.constructors().stream()
                .map(ModelValidator::constructorSignature).toList();
        for (ConstructorInfo constructor : expected.constructors()) {
            String signature = constructorSignature(constructor);
            String path = expected.name() + "(" + signature + ")";
            int index = actualSignatures.indexOf(signature);
            if (index < 0) {
                add(findings, Severity.ERROR, "MISSING_CONSTRUCTOR", path,
                        "Falta el constructor requerido.");
                continue;
            }
            compareVisibility(constructor.visibility(), actual.constructors().get(index).visibility(),
                    path, "CONSTRUCTOR_VISIBILITY", findings);
        }
    }

    /**
        * Comprueba los metodos requeridos, incluidos los heredados, y clasifica los adicionales.
     * @param expected tipo que declara el contrato de metodos
     * @param actual tipo implementado que se inspecciona
     * @param actualTypes jerarquia de tipos implementados, indexada por nombre simple
     * @param collectionOperations operaciones de coleccion que tambien deben estar disponibles
     * @param optionalMethods metodos que pueden faltar sin generar error
     * @param findings lista donde se agregan diferencias y metodos adicionales
     */
    private static void compareMethods(TypeInfo expected, TypeInfo actual, Map<String, TypeInfo> actualTypes,
                                      List<CollectionOperationRequirement> collectionOperations,
                                      Set<String> optionalMethods, Map<String, String> fieldAliases,
                                      List<Finding> findings) {
        Set<String> expectedSignatures = new LinkedHashSet<>();
        for (MethodInfo method : expected.methods()) {
            String signature = methodSignature(method);
            String actualSignature = methodSignatureForFieldAliases(method, fieldAliases);
            String key = methodKey(expected.name(), signature);
            expectedSignatures.add(signature);
            expectedSignatures.add(actualSignature);
                MethodInfo actualMethod = actual.methods().stream()
                    .filter(candidate -> methodSignature(candidate).equals(actualSignature))
                    .findFirst()
                    .orElseGet(() -> inheritedMethod(actual, actualSignature, actualTypes, new HashSet<>()));
            String path = expected.name() + "." + signature;
            if (actualMethod == null) {
                if (!optionalMethods.contains(key)) {
                    add(findings, Severity.ERROR, "MISSING_METHOD", path,
                            "Falta el metodo requerido.");
                }
                continue;
            }
                if (method.returnType() != null
                    && !typeName(method.returnType()).equals(typeName(actualMethod.returnType()))) {
                add(findings, Severity.ERROR, "METHOD_RETURN_TYPE", path,
                    "Retorno esperado " + typeName(method.returnType()) + ", implementado "
                                + typeName(actualMethod.returnType()) + ".");
            }
            compareVisibility(method.visibility(), actualMethod.visibility(), path,
                    "METHOD_VISIBILITY", findings);
            compareModifiers(method.modifiers(), actualMethod.modifiers(), path, "METHOD_MODIFIER", findings);
            if (!method.hasBody() && actualMethod.hasBody()) {
                add(findings, Severity.ERROR, "METHOD_BODY", path,
                        "El metodo se esperaba abstracto, pero tiene implementacion.");
            } else if (method.hasBody() && !actualMethod.hasBody()) {
                add(findings, Severity.ERROR, "METHOD_BODY", path,
                        "El metodo se esperaba implementado, pero es abstracto.");
            }
        }

        for (MethodInfo actualMethod : actual.methods()) {
            String signature = methodSignature(actualMethod);
                if (expectedSignatures.contains(signature) || isFieldAccessor(actualMethod, actual.fields())
                    || isRequiredCollectionOperationMethod(expected, actualMethod, actualTypes,
                        collectionOperations)) continue;
            Severity severity = actualMethod.visibility() == Visibility.PUBLIC ? Severity.WARNING : Severity.INFO;
                String visibility = visibilityDescription(actualMethod.visibility());
            add(findings, severity, "ADDITIONAL_METHOD", expected.name() + "." + signature,
                    actualMethod.visibility() == Visibility.PUBLIC
                                ? "Metodo " + visibility + " no descrito por el modelo esperado; revisar si el modelo LLM esta incompleto."
                                : "Metodo " + visibility + " auxiliar adicional de la implementacion.");
        }
    }

        private static String visibilityDescription(Visibility visibility) {
            if (visibility == null) return "con visibilidad no especificada";
            return switch (visibility) {
                case PUBLIC -> "publico";
                case PRIVATE -> "privado";
                case PROTECTED -> "protegido";
                case PACKAGE -> "de paquete";
            };
        }

    /**
        * Indica si un metodo implementado satisface una operacion implicita de una coleccion del tipo.
     * @param expectedOwner tipo cuyo modelo declara el atributo de coleccion
     * @param method metodo implementado que se clasifica
     * @param actualTypes tipos implementados, usados para resolver el tipo del elemento y sus claves
     * @param requirements operaciones de coleccion exigidas
     * @return {@code true} si el metodo cumple alguna operacion requerida por una coleccion del tipo
     */
    private static boolean isRequiredCollectionOperationMethod(TypeInfo expectedOwner, MethodInfo method,
                                                               Map<String, TypeInfo> actualTypes,
                                                               List<CollectionOperationRequirement> requirements) {
        for (CollectionOperationRequirement requirement : requirements) {
            if (!typeKey(requirement.owner()).equals(typeKey(expectedOwner.name()))) continue;
            FieldInfo field = expectedOwner.fields().stream()
                    .filter(candidate -> candidate.name().equals(requirement.field())).findFirst().orElse(null);
            if (field != null && satisfiesCollectionOperation(method, requirement.operation(),
                    collectionElement(field.type()), actualTypes)) return true;
        }
        return false;
    }

    /**
        * Busca un getter publico convencional para el atributo indicado.
     * @param type tipo donde se busca el getter
     * @param field atributo que debe devolver
     * @return {@code true} si existe un getter con nombre y tipo compatibles
     */
    private static boolean hasGetter(TypeInfo type, FieldInfo field) {
        String suffix = accessorSuffix(field.name());
        return type.methods().stream().anyMatch(method -> isGetter(method, field, suffix));
    }

    /**
        * Reconoce un getter o setter publico convencional asociado a alguno de los atributos.
     * @param method metodo que se quiere clasificar
     * @param fields atributos declarados por el tipo
     * @return {@code true} si el metodo coincide con un accesor publico no estatico
     */
    private static boolean isFieldAccessor(MethodInfo method, List<FieldInfo> fields) {
        if (method.visibility() != Visibility.PUBLIC || method.modifiers().contains(Modifier.STATIC)) return false;
        return fields.stream().anyMatch(field -> {
            String suffix = accessorSuffix(field.name());
            return isGetter(method, field, suffix) || isSetter(method, field, suffix);
        });
    }

    /**
        * Comprueba si un metodo es el getter convencional del atributo.
     * @param method metodo que se compara
     * @param field atributo cuyo valor debe devolver
     * @param suffix sufijo del nombre del atributo con inicial mayuscula
     * @return {@code true} si nombre, visibilidad, parametros y retorno corresponden a un getter
     */
    private static boolean isGetter(MethodInfo method, FieldInfo field, String suffix) {
        if (method.visibility() != Visibility.PUBLIC || method.modifiers().contains(Modifier.STATIC)
                || !method.parameters().isEmpty() || method.returnType() == null
                || !typeName(method.returnType()).equals(typeName(field.type()))) {
            return false;
        }
        return method.name().equals("get" + suffix)
                || ("boolean".equals(typeName(field.type())) && method.name().equals("is" + suffix));
    }

    /**
        * Comprueba si un metodo es el setter convencional del atributo.
     * @param method metodo que se compara
     * @param field atributo que debe modificar
     * @param suffix sufijo del nombre del atributo con inicial mayuscula
     * @return {@code true} si nombre, visibilidad, parametro y retorno corresponden a un setter
     */
    private static boolean isSetter(MethodInfo method, FieldInfo field, String suffix) {
        return method.visibility() == Visibility.PUBLIC && !method.modifiers().contains(Modifier.STATIC)
                && method.name().equals("set" + suffix)
                && method.parameters().size() == 1
                && "void".equals(typeName(method.returnType()))
                && typeName(method.parameters().get(0).type()).equals(typeName(field.type()));
    }

    /**
        * Convierte el nombre de un atributo a la forma usada por getX, isX y setX.
     * @param fieldName nombre original del atributo
     * @return nombre con el primer caracter en mayuscula
     */
    private static String accessorSuffix(String fieldName) {
        if (fieldName.isEmpty()) return fieldName;
        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
        * Busca recursivamente una firma en las superclases e interfaces accesibles del tipo.
     * @param type tipo cuyos ancestros se recorren
     * @param signature firma buscada
     * @param actualTypes tipos implementados indexados por nombre simple
     * @param visited ids ya recorridos, para evitar ciclos en jerarquias malformed
     * @return metodo heredado compatible, o {@code null} si no se encuentra
     */
    private static MethodInfo inheritedMethod(TypeInfo type, String signature, Map<String, TypeInfo> actualTypes,
                                              Set<String> visited) {
        if (!visited.add(type.id())) return null;

        List<TypeRef> ancestors = new ArrayList<>();
        if (type.superclass() != null) ancestors.add(type.superclass());
        ancestors.addAll(type.interfaces());

        for (TypeRef ancestorRef : ancestors) {
            String ancestorName = ancestorRef.qualifiedName() == null
                    ? ancestorRef.name() : ancestorRef.qualifiedName();
            TypeInfo ancestor = actualTypes.get(typeKey(ancestorName));
            if (ancestor == null) continue;

            MethodInfo declared = ancestor.methods().stream()
                    .filter(candidate -> methodSignature(candidate).equals(signature))
                    .findFirst().orElse(null);
            if (declared != null && isInheritedBy(declared, ancestor, type)) return declared;

            MethodInfo inherited = inheritedMethod(ancestor, signature, actualTypes, visited);
            if (inherited != null) return inherited;
        }
        return null;
    }

    /**
        * Determina si la visibilidad permite heredar un metodo en el tipo hijo.
     * @param method metodo declarado por el ancestro
     * @param declaringType tipo que declara el metodo
     * @param childType tipo descendiente que lo heredaria
     * @return {@code true} si no es privado y su visibilidad permite el acceso desde el hijo
     */
    private static boolean isInheritedBy(MethodInfo method, TypeInfo declaringType, TypeInfo childType) {
        if (method.visibility() == Visibility.PRIVATE) return false;
        return method.visibility() != Visibility.PACKAGE
                || Objects.equals(declaringType.packageName(), childType.packageName());
    }

    /**
        * Comprueba que la implementacion contenga las constantes requeridas de un enum.
     * @param expected enum descrito por el modelo esperado
     * @param actual enum encontrado en la implementacion
     * @param findings lista donde se agregan constantes ausentes
     */
    private static void compareEnumConstants(TypeInfo expected, TypeInfo actual, List<Finding> findings) {
        Set<String> actualNames = actual.enumConstants().stream().map(EnumConstantInfo::name)
                .collect(java.util.stream.Collectors.toSet());
        for (EnumConstantInfo constant : expected.enumConstants()) {
            if (!actualNames.contains(constant.name())) {
                add(findings, Severity.ERROR, "MISSING_ENUM_CONSTANT",
                        expected.name() + "." + constant.name(), "Falta el valor del enumerado.");
            }
        }
    }

    /**
        * Compara relaciones estructurales, multiplicidades y tipos de relacion.
     * @param expectedRelations relaciones declaradas por el modelo esperado
     * @param actualRelations relaciones extraidas de la implementacion
     * @param entryPoints tipos de entrada excluidos de las relaciones de dominio
     * @param findings lista donde se agregan relaciones ausentes, diferentes o adicionales
    * @param relationshipDecisions lista donde se agregan decisiones semanticas pendientes
     */
    private static void compareRelations(List<Relation> expectedRelations, List<Relation> actualRelations,
                                        Set<String> entryPoints, Set<String> actualTypeKeys,
                                        Set<String> optionalTypes, List<Finding> findings,
                                        List<RelationshipDecision> relationshipDecisions) {
        List<Relation> expected = expectedRelations.stream()
                .filter(relation -> relation.kind() != RelationKind.DEPENDENCY).toList();
        List<Relation> actual = actualRelations.stream()
                .filter(relation -> relation.kind() != RelationKind.DEPENDENCY)
                .filter(relation -> !entryPoints.contains(typeKey(relation.from())))
                .filter(relation -> !entryPoints.contains(typeKey(relation.to())))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        for (Relation expectedRelation : expected) {
            if (hasMissingOptionalEndpoint(expectedRelation, actualTypeKeys, optionalTypes)) continue;
            int matchIndex = -1;
            for (int i = 0; i < actual.size(); i++) {
                if (sameRelationEndpoint(expectedRelation, actual.get(i))) {
                    matchIndex = i;
                    break;
                }
            }
            String path = typeKey(expectedRelation.from()) + " -> " + typeKey(expectedRelation.to())
                    + (expectedRelation.via() == null ? ""
                        : " a través de atributo " + expectedRelation.via());
            if (matchIndex < 0) {
                add(findings, Severity.ERROR, "MISSING_RELATION", path,
                        "No se encontro la relacion esperada en el modelo extraido.");
                continue;
            }
            Relation actualRelation = actual.remove(matchIndex);
            if (expectedRelation.multiplicity() != null
                    && !expectedRelation.multiplicity().equals(actualRelation.multiplicity())) {
                add(findings, Severity.ERROR, "RELATION_MULTIPLICITY", path,
                        "Multiplicidad esperada " + expectedRelation.multiplicity() + ", extraida "
                                + actualRelation.multiplicity() + ".");
            }
            if (expectedRelation.kind() == actualRelation.kind()) continue;
            if (ownershipKind(expectedRelation.kind()) && ownershipKind(actualRelation.kind())) {
                List<RelationKind> options = List.of(expectedRelation.kind(), actualRelation.kind());
                relationshipDecisions.add(new RelationshipDecision(typeKey(expectedRelation.from()),
                        typeKey(expectedRelation.to()), expectedRelation.via(), expectedRelation.kind(),
                    actualRelation.kind(), "PENDING_CONFIRMATION", options));
                String possibleKinds = options.stream().map(Enum::name)
                    .collect(java.util.stream.Collectors.joining(" o "));
                add(findings, Severity.WARNING, "RELATION_KIND_UNCERTAIN", path,
                    "No se puede determinar el tipo de relacion a partir del codigo. Opciones posibles: "
                        + possibleKinds + ". Confirma el tipo antes de generar el CRUD.");
            } else {
                add(findings, Severity.ERROR, "RELATION_KIND", path,
                        "Relacion esperada " + expectedRelation.kind() + ", extraida "
                                + actualRelation.kind() + ".");
            }
        }

        for (Relation extra : actual) {
            add(findings, Severity.INFO, "ADDITIONAL_RELATION",
                    typeKey(extra.from()) + " -> " + typeKey(extra.to()),
                    "Relacion adicional en la implementacion.");
        }
    }

    private static boolean hasMissingOptionalEndpoint(Relation relation, Set<String> actualTypeKeys,
                                                       Set<String> optionalTypes) {
        String from = typeKey(relation.from());
        String to = typeKey(relation.to());
        return (optionalTypes.contains(from) && !actualTypeKeys.contains(from))
                || (optionalTypes.contains(to) && !actualTypeKeys.contains(to));
    }

    /**
        * Indexa tipos por nombre simple y registra colisiones ambiguas.
     * @param types tipos que se van a indexar
     * @param side etiqueta del modelo, usada en el mensaje de ambiguedad
     * @param findings lista donde se agregan colisiones
     * @return tipos indexados por su nombre simple
     */
    private static Map<String, TypeInfo> indexTypes(List<TypeInfo> types, String side, List<Finding> findings) {
        Map<String, TypeInfo> indexed = new LinkedHashMap<>();
        for (TypeInfo type : types) {
            String key = typeKey(type.name());
            if (indexed.putIfAbsent(key, type) != null) {
                add(findings, Severity.ERROR, "AMBIGUOUS_TYPE", type.name(),
                        "Hay mas de un tipo con el mismo nombre simple en el modelo " + side + ".");
            }
        }
        return indexed;
    }

    /**
        * Reune las clases de entrada declaradas en el analisis o detectadas por tener main.
     * @param model modelo del que se extraen los puntos de entrada
     * @return ids simples de las clases que actuan como lanzadores
     */
    private static Set<String> entryPoints(ProjectModel model) {
        Set<String> entryPoints = new HashSet<>();
        if (model.analysis() != null && model.analysis().entryPoints() != null) {
            model.analysis().entryPoints().forEach(id -> entryPoints.add(typeKey(id)));
        }
        model.types().stream().filter(TypeInfo::hasMainMethod)
                .forEach(type -> entryPoints.add(typeKey(type.id())));
        return entryPoints;
    }

    /**
        * Extrae de los avisos las clases declaradas opcionales.
     * @param source metadatos de origen del modelo esperado
     * @return nombres simples de clases que pueden faltar
     */
    private static Set<String> optionalTypes(SourceInfo source) {
        Set<String> result = new HashSet<>();
        for (String warning : warnings(source)) {
            if (!warning.startsWith("OPCIONAL:")) continue;
            String item = warning.substring("OPCIONAL:".length()).strip();
            if (!item.contains("(") && !item.contains(".")) result.add(typeKey(item));
        }
        return result;
    }

    /**
        * Extrae firmas de metodos opcionales desde los avisos del modelo esperado.
     * @param source metadatos de origen del modelo esperado
     * @return claves de metodos que pueden faltar
     */
    private static Set<String> optionalMethods(SourceInfo source) {
        Set<String> result = new HashSet<>();
        for (String warning : warnings(source)) {
            if (!warning.startsWith("OPCIONAL:")) continue;
            Matcher matcher = OPTIONAL_METHOD.matcher(warning.strip());
            if (!matcher.matches()) continue;
            String parameters = matcher.group(3).strip();
            List<String> parameterTypes = parameters.isEmpty() ? List.of()
                    : splitParameters(parameters).stream().map(ModelValidator::typeText).toList();
            result.add(methodKey(matcher.group(1), matcher.group(2) + "(" + String.join(",", parameterTypes) + ")"));
        }
        return result;
    }

    /**
        * Extrae atributos declarados opcionales desde los avisos del modelo esperado.
     * @param source metadatos de origen del modelo esperado
     * @return claves de atributos que pueden faltar
     */
    private static Set<String> optionalFields(SourceInfo source) {
        Set<String> result = new HashSet<>();
        for (String warning : warnings(source)) {
            Matcher matcher = OPTIONAL_FIELD.matcher(warning.strip());
            if (!matcher.matches()) continue;
            result.add(fieldKey(matcher.group(1), matcher.group(2)));
        }
        return result;
    }

    private static Set<String> unspecifiedFieldNames(SourceInfo source) {
        Set<String> result = new HashSet<>();
        for (String warning : warnings(source)) {
            Matcher matcher = UNSPECIFIED_FIELD_NAME.matcher(warning.strip());
            if (matcher.matches()) result.add(fieldKey(matcher.group(1), matcher.group(2)));
        }
        return result;
    }

    /**
        * Combina operaciones explicitas con las inferidas de atributos privados de coleccion.
     * @param source metadatos que pueden declarar operaciones adicionales
     * @param expectedTypes tipos esperados, usados para detectar colecciones privadas
     * @return requisitos de alta/baja deduplicados por tipo, atributo y operacion
     */
    private static List<CollectionOperationRequirement> collectionOperations(SourceInfo source,
                                                                              Map<String, TypeInfo> expectedTypes) {
        Set<CollectionOperationRequirement> result = new LinkedHashSet<>();
        for (String warning : warnings(source)) {
            Matcher matcher = COLLECTION_OPERATIONS.matcher(warning.strip());
            if (!matcher.matches()) continue;
            for (String operation : matcher.group(3).split(",")) {
                String normalized = operation.strip();
                if (normalized.equals("add") || normalized.equals("remove")) {
                    result.add(new CollectionOperationRequirement(matcher.group(1), matcher.group(2), normalized));
                }
            }
        }

        for (TypeInfo type : expectedTypes.values()) {
            for (FieldInfo field : type.fields()) {
                if (field.visibility() != Visibility.PRIVATE || !isMutableCollection(field.type())) continue;
                result.add(new CollectionOperationRequirement(type.name(), field.name(), "add"));
                result.add(new CollectionOperationRequirement(type.name(), field.name(), "remove"));
            }
        }
        return List.copyOf(result);
    }

    /**
        * Verifica que existan metodos compatibles para las operaciones requeridas sobre colecciones.
     * @param requirements operaciones de alta/baja exigidas
     * @param expectedTypes tipos del modelo esperado
     * @param actualTypes tipos extraidos de la implementacion
     * @param findings lista donde se agregan operaciones ausentes
     */
    private static void validateCollectionOperations(List<CollectionOperationRequirement> requirements,
                                                     Map<String, TypeInfo> expectedTypes,
                                                     Map<String, TypeInfo> actualTypes,
                                                     List<Finding> findings) {
        for (CollectionOperationRequirement requirement : requirements) {
            String ownerKey = typeKey(requirement.owner());
            TypeInfo expectedOwner = expectedTypes.get(ownerKey);
            TypeInfo actualOwner = actualTypes.get(ownerKey);
            if (expectedOwner == null || actualOwner == null) continue;

            FieldInfo expectedField = expectedOwner.fields().stream()
                    .filter(field -> field.name().equals(requirement.field())).findFirst().orElse(null);
            FieldInfo actualField = actualOwner.fields().stream()
                    .filter(field -> field.name().equals(requirement.field())).findFirst().orElse(null);
                if (expectedField == null || actualField == null) continue;

            TypeRef elementType = collectionElement(expectedField.type());
            boolean present = actualOwner.methods().stream()
                    .anyMatch(method -> satisfiesCollectionOperation(method, requirement.operation(),
                            elementType, actualTypes));
            if (!present) {
                String path = requirement.owner() + "." + requirement.field() + "." + requirement.operation();
                add(findings, Severity.ERROR, "MISSING_COLLECTION_OPERATION", path,
                        "Falta un metodo para " + (requirement.operation().equals("add")
                                ? "anadir" : "eliminar") + " elementos de la lista.");
            }
        }
    }

    /**
        * Indica si el tipo corresponde a una coleccion mutable reconocida por el validador.
     * @param type referencia al tipo del atributo
     * @return {@code true} si el nombre representa una lista, conjunto u otra coleccion mutable admitida
     */
    private static boolean isMutableCollection(TypeRef type) {
        return MUTABLE_COLLECTION_TYPES.contains(typeKey(type.name()));
    }

    /**
        * Comprueba si la firma del metodo cumple una operacion de alta o baja de elementos.
     * @param method metodo implementado que se evalua
     * @param operation operacion requerida: {@code add} o {@code remove}
     * @param elementType tipo de los elementos almacenados
     * @param actualTypes tipos implementados, usados para resolver campos identificadores
     * @return {@code true} si el nombre y el parametro del metodo son compatibles con la operacion
     */
    private static boolean satisfiesCollectionOperation(MethodInfo method, String operation, TypeRef elementType,
                                                        Map<String, TypeInfo> actualTypes) {
        if (method.parameters().size() != 1) return false;
        String methodName = method.name().toLowerCase(java.util.Locale.ROOT);
        TypeRef parameterType = method.parameters().get(0).type();
        if (operation.equals("add")) {
            return startsWithAny(methodName, "add", "anadir", "agregar", "insert")
                    && typeName(parameterType).equals(typeName(elementType));
        }
        return startsWithAny(methodName, "remove", "delete", "eliminar", "borrar", "quitar")
                && acceptsElementOrKey(parameterType, elementType, actualTypes);
    }

    /**
        * Comprueba si un parametro representa el elemento o una de sus claves identificadoras.
     * @param parameterType tipo del parametro del metodo de baja
     * @param elementType tipo almacenado por la coleccion
     * @param actualTypes tipos implementados, para resolver atributos como id o email
     * @return {@code true} si el parametro coincide con el elemento o con un campo identificador
     */
    private static boolean acceptsElementOrKey(TypeRef parameterType, TypeRef elementType,
                                                Map<String, TypeInfo> actualTypes) {
        if (typeName(parameterType).equals(typeName(elementType))) return true;
        String elementId = elementType.qualifiedName() == null ? elementType.name() : elementType.qualifiedName();
        TypeInfo element = actualTypes.get(typeKey(elementId));
        if (element == null) return false;
        return element.fields().stream()
                .filter(field -> isIdentityField(field.name()))
                .anyMatch(field -> typeName(field.type()).equals(typeName(parameterType)));
    }

    /**
        * Clasifica nombres convencionales de campos que identifican un elemento.
     * @param fieldName nombre del campo candidato
     * @return {@code true} para id, email, uuid, key y equivalentes
     */
    private static boolean isIdentityField(String fieldName) {
        String normalized = fieldName.toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("id") || normalized.equals("email") || normalized.equals("correo")
                || normalized.equals("uuid") || normalized.equals("key") || normalized.equals("clave");
    }

    /**
        * Comprueba si una cadena empieza por alguno de los prefijos indicados.
     * @param value cadena que se inspecciona
     * @param prefixes prefijos admitidos
     * @return {@code true} si coincide al menos un prefijo
     */
    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
        * Obtiene el primer argumento generico de una coleccion o devuelve su propio tipo si es raw.
     * @param collectionType referencia a la coleccion
     * @return tipo de elemento de la coleccion
     */
    private static TypeRef collectionElement(TypeRef collectionType) {
        return collectionType.arguments().isEmpty() ? collectionType : collectionType.arguments().get(0);
    }

    /**
        * Devuelve los avisos de origen, normalizando metadatos ausentes a una lista vacia.
     * @param source metadatos de origen que pueden contener avisos
     * @return avisos del modelo o una lista vacia
     */
    private static List<String> warnings(SourceInfo source) {
        return source == null || source.warnings() == null ? List.of() : source.warnings();
    }

    /**
        * Divide tipos de parametros por comas sin separar argumentos dentro de genericos.
     * @param parameters texto de parametros de una firma
     * @return lista de tipos de parametro recortados
     */
    private static List<String> splitParameters(String parameters) {
        List<String> result = new ArrayList<>();
        int genericDepth = 0;
        int start = 0;
        for (int i = 0; i < parameters.length(); i++) {
            char character = parameters.charAt(i);
            if (character == '<') genericDepth++;
            else if (character == '>') genericDepth--;
            else if (character == ',' && genericDepth == 0) {
                result.add(parameters.substring(start, i).strip());
                start = i + 1;
            }
        }
        result.add(parameters.substring(start).strip());
        return result;
    }

    /**
        * Normaliza una referencia de tipo escrita en un aviso opcional.
     * @param text texto original del tipo
     * @return tipo sin espacios y con nombres cualificados reducidos a su nombre simple
     */
    private static String typeText(String text) {
        return text.replaceAll("\\s+", "").replaceAll("(?:[\\w$]+\\.)+([\\w$]+)", "$1");
    }

    /**
        * Construye la firma de un constructor usando los tipos de sus parametros.
     * @param constructor constructor cuya firma se normaliza
     * @return tipos de parametros separados por comas
     */
    private static String constructorSignature(ConstructorInfo constructor) {
        return constructor.parameters().stream().map(ParameterInfo::type).map(ModelValidator::typeName)
                .collect(java.util.stream.Collectors.joining(","));
    }

    /**
        * Construye la firma de un metodo usando su nombre y tipos de parametros.
     * @param method metodo cuya firma se normaliza
     * @return nombre y tipos de parametros en formato {@code nombre(Tipo,Tipo)}
     */
    private static String methodSignature(MethodInfo method) {
        return method.name() + "(" + method.parameters().stream().map(ParameterInfo::type)
                .map(ModelValidator::typeName).collect(java.util.stream.Collectors.joining(",")) + ")";
    }

    private static String methodSignatureForFieldAliases(MethodInfo method, Map<String, String> fieldAliases) {
        String methodName = method.name();
        for (Map.Entry<String, String> alias : fieldAliases.entrySet()) {
            String expectedSuffix = Character.toUpperCase(alias.getKey().charAt(0)) + alias.getKey().substring(1);
            String actualSuffix = Character.toUpperCase(alias.getValue().charAt(0)) + alias.getValue().substring(1);
            if (methodName.equals("get" + expectedSuffix)) methodName = "get" + actualSuffix;
            else if (methodName.equals("set" + expectedSuffix)) methodName = "set" + actualSuffix;
        }
        return methodName + "(" + method.parameters().stream().map(ParameterInfo::type)
                .map(ModelValidator::typeName).collect(java.util.stream.Collectors.joining(",")) + ")";
    }

    /**
        * Genera una clave normalizada para identificar un metodo de una clase.
     * @param owner nombre de la clase propietaria
     * @param signature firma normalizada del metodo
     * @return clave formada por clase y firma
     */
    private static String methodKey(String owner, String signature) {
        return typeKey(owner) + "." + signature;
    }

    /**
        * Genera una clave normalizada para identificar un atributo de una clase.
     * @param owner nombre de la clase propietaria
     * @param name nombre del atributo
     * @return clave formada por clase y atributo
     */
    private static String fieldKey(String owner, String name) {
        return typeKey(owner) + "." + name;
    }

    /**
        * Normaliza una referencia de tipo, conservando genericos y dimensiones de array.
     * @param type referencia de tipo que se normaliza
     * @return nombre simple con argumentos genericos y dimensiones, o marcador si es {@code null}
     */
    private static String typeName(TypeRef type) {
        if (type == null) return "<no especificado>";
        StringBuilder result = new StringBuilder(typeKey(type.name()));
        if (!type.arguments().isEmpty()) {
            result.append('<').append(type.arguments().stream().map(ModelValidator::typeName)
                    .collect(java.util.stream.Collectors.joining(","))).append('>');
        }
        result.append("[]".repeat(type.arrayDimensions()));
        return result.toString();
    }

    /**
        * Reduce un identificador cualificado o anidado al nombre simple de su tipo.
     * @param id identificador cualificado del tipo
     * @return nombre simple, o {@code <null>} si el identificador es nulo
     */
    private static String typeKey(String id) {
        if (id == null) return "<null>";
        int separator = Math.max(id.lastIndexOf('.'), id.lastIndexOf('$'));
        return id.substring(separator + 1);
    }

    /**
        * Compara los extremos y el atributo asociado de dos relaciones.
     * @param expected relacion del modelo esperado
     * @param actual relacion extraida de la implementacion
     * @return {@code true} si origen, destino y atributo coinciden
     */
    private static boolean sameRelationEndpoint(Relation expected, Relation actual) {
        return typeKey(expected.from()).equals(typeKey(actual.from()))
                && typeKey(expected.to()).equals(typeKey(actual.to()))
                && Objects.equals(expected.via(), actual.via());
    }

    /**
        * Indica si el tipo de relacion pertenece a las semanticas de asociacion/propiedad.
     * @param kind tipo de relacion que se clasifica
    * @return {@code true} para asociacion, composicion o agregacion
     */
    private static boolean ownershipKind(RelationKind kind) {
        return kind == RelationKind.COMPOSITION || kind == RelationKind.AGGREGATION
            || kind == RelationKind.ASSOCIATION;
    }

    /**
        * Registra una diferencia de visibilidad cuando el modelo esperado la especifica.
     * @param expected visibilidad exigida, o {@code null} si no se especifico
     * @param actual visibilidad encontrada en la implementacion
     * @param path ubicacion del tipo o miembro comparado
     * @param code codigo del hallazgo que se generara
     * @param findings lista donde se agrega la diferencia, si corresponde
     */
    private static void compareVisibility(Visibility expected, Visibility actual, String path, String code,
                                         List<Finding> findings) {
        if (expected != null && expected != actual) {
            add(findings, Severity.ERROR, code, path,
                    "Visibilidad esperada " + expected + ", implementada " + actual + ".");
        }
    }

    /**
        * Comprueba que la implementacion contenga todos los modificadores requeridos.
     * @param expected modificadores exigidos
     * @param actual modificadores presentes en la implementacion
     * @param path ubicacion del tipo o miembro comparado
     * @param code codigo del hallazgo que se generara
     * @param findings lista donde se agregan los modificadores ausentes
     */
    private static void compareModifiers(List<Modifier> expected, List<Modifier> actual, String path,
                                         String code, List<Finding> findings) {
        for (Modifier modifier : expected) {
            if (!actual.contains(modifier)) {
                add(findings, Severity.ERROR, code, path, "Falta el modificador " + modifier + ".");
            }
        }
    }

    /**
        * Agrega un hallazgo con su nivel, codigo, ubicacion y mensaje.
     * @param findings lista de destino
     * @param severity severidad del hallazgo
     * @param code codigo estable de clasificacion
     * @param path tipo o miembro afectado
     * @param message explicacion legible de la diferencia
     */
    private static void add(List<Finding> findings, Severity severity, String code, String path, String message) {
        findings.add(new Finding(severity, code, path, message));
    }

    /** Severidad de una diferencia encontrada. */
    public enum Severity { ERROR, WARNING, INFO }

    /** Diferencia o detalle detectado durante la validacion.
     * @param severity nivel de importancia del hallazgo
     * @param code codigo estable de clasificacion
     * @param path tipo o miembro afectado
     * @param message explicacion legible del hallazgo
     */
    public record Finding(Severity severity, String code, String path, String message) { }

    /** Resultado de validar estructura y decisiones semanticas de un modelo.
     * @param findings lista de diferencias detectadas, normalizada a una lista inmutable
     * @param relationshipDecisions relaciones pendientes de confirmacion, normalizadas a lista inmutable
     */
    public record RelationshipDecision(String from, String to, String via, RelationKind expectedKind,
                                       RelationKind inferredKind, String status, List<RelationKind> options) {
        public RelationshipDecision {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }

    public record Result(List<Finding> findings, List<RelationshipDecision> relationshipDecisions) {
        /** Normaliza la lista de hallazgos y la hace inmutable. */
        public Result {
            findings = findings == null ? List.of() : List.copyOf(findings);
            relationshipDecisions = relationshipDecisions == null
                    ? List.of() : List.copyOf(relationshipDecisions);
        }

        public Result(List<Finding> findings) {
            this(findings, List.of());
        }

        /** @return {@code true} si no hay incumplimientos bloqueantes */
        public boolean isValid() {
            return findings.stream().noneMatch(finding -> finding.severity() == Severity.ERROR);
        }

        /** @return diferencias que invalidan el modelo */
        public List<Finding> errors() {
            return findings.stream().filter(finding -> finding.severity() == Severity.ERROR).toList();
        }

        /** @return avisos y detalles no bloqueantes */
        public List<Finding> notes() {
            return findings.stream().filter(finding -> finding.severity() != Severity.ERROR).toList();
        }
    }

    private record CollectionOperationRequirement(String owner, String field, String operation) { }
}
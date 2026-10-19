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
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actual, "actual");

        List<Finding> findings = new ArrayList<>();
        Set<String> optionalTypes = optionalTypes(expected.source());
        Set<String> optionalMethods = optionalMethods(expected.source());
        Map<String, TypeInfo> expectedTypes = indexTypes(expected.types(), "expected", findings);
        Set<String> entryPoints = entryPoints(actual);
        List<TypeInfo> actualDomainTypes = actual.types().stream()
                .filter(type -> !type.hasMainMethod())
                .filter(type -> !entryPoints.contains(typeKey(type.id())))
                .toList();
        Map<String, TypeInfo> actualTypes = indexTypes(actualDomainTypes, "actual", findings);

        for (Map.Entry<String, TypeInfo> entry : expectedTypes.entrySet()) {
            String key = entry.getKey();
            TypeInfo expectedType = entry.getValue();
            TypeInfo actualType = actualTypes.get(key);
            if (actualType == null) {
                if (!optionalTypes.contains(key)) {
                    add(findings, Severity.ERROR, "MISSING_TYPE", expectedType.name(),
                            "No se encontro el tipo requerido en la implementacion.");
                }
                continue;
            }
            compareType(expectedType, actualType, optionalMethods, findings);
        }

        for (Map.Entry<String, TypeInfo> entry : actualTypes.entrySet()) {
            if (!expectedTypes.containsKey(entry.getKey())) {
                add(findings, Severity.INFO, "ADDITIONAL_TYPE", entry.getValue().name(),
                        "Tipo adicional de la implementacion; no invalida el contrato esperado.");
            }
        }

        compareRelations(expected.relations(), actual.relations(), entryPoints, findings);
        return new Result(findings);
    }

    private static void compareType(TypeInfo expected, TypeInfo actual, Set<String> optionalMethods,
                                    List<Finding> findings) {
        String path = expected.name();
        if (expected.kind() != actual.kind()) {
            add(findings, Severity.ERROR, "TYPE_KIND", path,
                    "Tipo esperado " + expected.kind() + ", implementado " + actual.kind() + ".");
        }
        compareVisibility(expected.visibility(), actual.visibility(), path, "TYPE_VISIBILITY", findings);
        compareModifiers(expected.modifiers(), actual.modifiers(), path, "TYPE_MODIFIER", findings);

        if (expected.superclass() != null
                && !typeName(expected.superclass()).equals(typeName(actual.superclass()))) {
            add(findings, Severity.ERROR, "SUPERCLASS", path,
                    "Superclase esperada " + typeName(expected.superclass()) + ", implementada "
                            + typeName(actual.superclass()) + ".");
        }
        Set<String> actualInterfaces = actual.interfaces().stream().map(ModelValidator::typeName)
                .collect(java.util.stream.Collectors.toSet());
        for (TypeRef expectedInterface : expected.interfaces()) {
            if (!actualInterfaces.contains(typeName(expectedInterface))) {
                add(findings, Severity.ERROR, "MISSING_INTERFACE", path,
                        "Falta la interfaz " + typeName(expectedInterface) + ".");
            }
        }

        Map<String, FieldInfo> actualFields = new LinkedHashMap<>();
        actual.fields().forEach(field -> actualFields.put(field.name(), field));
        Set<String> expectedFieldNames = new HashSet<>();
        for (FieldInfo expectedField : expected.fields()) {
            expectedFieldNames.add(expectedField.name());
            FieldInfo actualField = actualFields.get(expectedField.name());
            String fieldPath = path + "." + expectedField.name();
            if (actualField == null) {
                add(findings, Severity.ERROR, "MISSING_FIELD", fieldPath, "Falta el atributo requerido.");
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
            if (!expectedFieldNames.contains(actualField.name())) {
                add(findings, Severity.INFO, "ADDITIONAL_FIELD", path + "." + actualField.name(),
                        "Atributo adicional de la implementacion.");
            }
        }

        compareConstructors(expected, actual, findings);
        compareMethods(expected, actual, optionalMethods, findings);
        compareEnumConstants(expected, actual, findings);
    }

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

    private static void compareMethods(TypeInfo expected, TypeInfo actual, Set<String> optionalMethods,
                                      List<Finding> findings) {
        Set<String> expectedSignatures = new LinkedHashSet<>();
        for (MethodInfo method : expected.methods()) {
            String signature = methodSignature(method);
            String key = methodKey(expected.name(), signature);
            expectedSignatures.add(signature);
            MethodInfo actualMethod = actual.methods().stream()
                    .filter(candidate -> methodSignature(candidate).equals(signature))
                    .findFirst().orElse(null);
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
            if (expectedSignatures.contains(signature)) continue;
            Severity severity = actualMethod.visibility() == Visibility.PUBLIC ? Severity.WARNING : Severity.INFO;
            add(findings, severity, "ADDITIONAL_METHOD", expected.name() + "." + signature,
                    actualMethod.visibility() == Visibility.PUBLIC
                            ? "Metodo publico no descrito por el modelo esperado; revisar si el modelo LLM esta incompleto."
                            : "Metodo auxiliar adicional de la implementacion.");
        }
    }

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

    private static void compareRelations(List<Relation> expectedRelations, List<Relation> actualRelations,
                                        Set<String> entryPoints, List<Finding> findings) {
        List<Relation> expected = expectedRelations.stream()
                .filter(relation -> relation.kind() != RelationKind.DEPENDENCY).toList();
        List<Relation> actual = actualRelations.stream()
                .filter(relation -> relation.kind() != RelationKind.DEPENDENCY)
                .filter(relation -> !entryPoints.contains(typeKey(relation.from())))
                .filter(relation -> !entryPoints.contains(typeKey(relation.to())))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        for (Relation expectedRelation : expected) {
            int matchIndex = -1;
            for (int i = 0; i < actual.size(); i++) {
                if (sameRelationEndpoint(expectedRelation, actual.get(i))) {
                    matchIndex = i;
                    break;
                }
            }
            String path = typeKey(expectedRelation.from()) + " -> " + typeKey(expectedRelation.to())
                    + (expectedRelation.via() == null ? "" : " via " + expectedRelation.via());
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
                add(findings, Severity.WARNING, "RELATION_OWNERSHIP_UNCERTAIN", path,
                        "El enunciado y el codigo distinguen composicion/agregacion; Java no permite confirmar "
                                + "por si solo la propiedad del ciclo de vida.");
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

    private static Set<String> entryPoints(ProjectModel model) {
        Set<String> entryPoints = new HashSet<>();
        if (model.analysis() != null && model.analysis().entryPoints() != null) {
            model.analysis().entryPoints().forEach(id -> entryPoints.add(typeKey(id)));
        }
        model.types().stream().filter(TypeInfo::hasMainMethod)
                .forEach(type -> entryPoints.add(typeKey(type.id())));
        return entryPoints;
    }

    private static Set<String> optionalTypes(SourceInfo source) {
        Set<String> result = new HashSet<>();
        for (String warning : warnings(source)) {
            if (!warning.startsWith("OPCIONAL:")) continue;
            String item = warning.substring("OPCIONAL:".length()).strip();
            if (!item.contains("(") && !item.contains(".")) result.add(typeKey(item));
        }
        return result;
    }

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

    private static List<String> warnings(SourceInfo source) {
        return source == null || source.warnings() == null ? List.of() : source.warnings();
    }

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

    private static String typeText(String text) {
        return text.replaceAll("\\s+", "").replaceAll("(?:[\\w$]+\\.)+([\\w$]+)", "$1");
    }

    private static String constructorSignature(ConstructorInfo constructor) {
        return constructor.parameters().stream().map(ParameterInfo::type).map(ModelValidator::typeName)
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String methodSignature(MethodInfo method) {
        return method.name() + "(" + method.parameters().stream().map(ParameterInfo::type)
                .map(ModelValidator::typeName).collect(java.util.stream.Collectors.joining(",")) + ")";
    }

    private static String methodKey(String owner, String signature) {
        return typeKey(owner) + "." + signature;
    }

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

    private static String typeKey(String id) {
        if (id == null) return "<null>";
        int separator = Math.max(id.lastIndexOf('.'), id.lastIndexOf('$'));
        return id.substring(separator + 1);
    }

    private static boolean sameRelationEndpoint(Relation expected, Relation actual) {
        return typeKey(expected.from()).equals(typeKey(actual.from()))
                && typeKey(expected.to()).equals(typeKey(actual.to()))
                && Objects.equals(expected.via(), actual.via());
    }

    private static boolean ownershipKind(RelationKind kind) {
        return kind == RelationKind.COMPOSITION || kind == RelationKind.AGGREGATION;
    }

    private static void compareVisibility(Visibility expected, Visibility actual, String path, String code,
                                         List<Finding> findings) {
        if (expected != null && expected != actual) {
            add(findings, Severity.ERROR, code, path,
                    "Visibilidad esperada " + expected + ", implementada " + actual + ".");
        }
    }

    private static void compareModifiers(List<Modifier> expected, List<Modifier> actual, String path,
                                         String code, List<Finding> findings) {
        for (Modifier modifier : expected) {
            if (!actual.contains(modifier)) {
                add(findings, Severity.ERROR, code, path, "Falta el modificador " + modifier + ".");
            }
        }
    }

    private static void add(List<Finding> findings, Severity severity, String code, String path, String message) {
        findings.add(new Finding(severity, code, path, message));
    }

    /** Severidad de una diferencia encontrada. */
    public enum Severity { ERROR, WARNING, INFO }

    /** Diferencia o detalle detectado durante la validacion. */
    public record Finding(Severity severity, String code, String path, String message) { }

    /** Resultado de validar un modelo esperado frente a una implementacion. */
    public record Result(List<Finding> findings) {
        public Result {
            findings = findings == null ? List.of() : List.copyOf(findings);
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
}
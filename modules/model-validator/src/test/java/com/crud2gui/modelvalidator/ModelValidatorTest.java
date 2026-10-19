package com.crud2gui.modelvalidator;

import com.crud2gui.model.Analysis;
import com.crud2gui.model.Confidence;
import com.crud2gui.model.ConstructorInfo;
import com.crud2gui.model.FieldInfo;
import com.crud2gui.model.MethodInfo;
import com.crud2gui.model.ParameterInfo;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.model.Relation;
import com.crud2gui.model.RelationKind;
import com.crud2gui.model.SourceInfo;
import com.crud2gui.model.TypeInfo;
import com.crud2gui.model.TypeKind;
import com.crud2gui.model.TypeRef;
import com.crud2gui.model.Visibility;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelValidatorTest {

    @Test
    void acceptsRequiredContractAndReportsNonBlockingDifferences() {
        ProjectModel expected = model(List.of(
                type("Owner", "", TypeKind.CLASS, List.of(
                        field("projects", listOf("Project"), Visibility.PRIVATE)),
                        List.of(constructor(Visibility.PUBLIC, "String")),
                        List.of(method("addProject", "void", Visibility.PUBLIC, "Project"),
                                method("findProject", "Project", Visibility.PUBLIC, "String")),
                        false),
                type("Project", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false)),
                List.of(new Relation(RelationKind.COMPOSITION, "Owner", "Project", "projects",
                        "0..*", Confidence.HIGH, null)),
                List.of("OPCIONAL: Owner.findProject(String)"), null);

        ProjectModel actual = model(List.of(
                type("demo.Owner", "demo", TypeKind.CLASS, List.of(
                        field("projects", listOf("demo.Project"), Visibility.PRIVATE)),
                        List.of(constructor(Visibility.PUBLIC, "String")),
                        List.of(method("addProject", "void", Visibility.PUBLIC, "demo.Project"),
                                method("getProjects", "List", Visibility.PUBLIC)), false),
                type("demo.Project", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), false),
                type("demo.Launcher", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), true)),
                List.of(
                        new Relation(RelationKind.AGGREGATION, "demo.Owner", "demo.Project", "projects",
                                "0..*", Confidence.HIGH, null),
                        new Relation(RelationKind.DEPENDENCY, "demo.Launcher", "demo.Owner", "main(owner)",
                                null, Confidence.HIGH, null)),
                List.of(), new Analysis(null, List.of(), List.of(), List.of("demo.Launcher"), false));

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("RELATION_OWNERSHIP_UNCERTAIN")));
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("ADDITIONAL_METHOD")));
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_METHOD")
                && f.path().contains("findProject")));
        assertFalse(result.findings().stream().anyMatch(f -> f.path().contains("Launcher")));
    }

    @Test
    void rejectsMissingRequiredFieldsAndMethods() {
        ProjectModel expected = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(field("email", typeRef("String", false), Visibility.PRIVATE)), List.of(),
                List.of(method("close", "void", Visibility.PUBLIC)), false)), List.of(), List.of(), null);
        ProjectModel actual = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(), List.of(), List.of(), false)), List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertFalse(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_FIELD")));
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_METHOD")));
    }

    private static ProjectModel model(List<TypeInfo> types, List<Relation> relations,
                                      List<String> warnings, Analysis analysis) {
        return new ProjectModel("1.0", new SourceInfo(null, null, 0, warnings), types, relations, analysis, null);
    }

    private static TypeInfo type(String id, String packageName, TypeKind kind, List<FieldInfo> fields,
                                 List<ConstructorInfo> constructors, List<MethodInfo> methods,
                                 boolean hasMainMethod) {
        String name = id.substring(Math.max(id.lastIndexOf('.'), id.lastIndexOf('$')) + 1);
        return new TypeInfo(id, name, packageName, kind, Visibility.PUBLIC, List.of(), null, null,
                List.of(), List.of(), List.of(), null, null, hasMainMethod, fields, constructors,
                methods, List.of());
    }

    private static FieldInfo field(String name, TypeRef type, Visibility visibility) {
        return new FieldInfo(name, type, visibility, List.of(), false, List.of(), null, null);
    }

    private static ConstructorInfo constructor(Visibility visibility, String... parameterTypes) {
        List<ParameterInfo> parameters = java.util.stream.IntStream.range(0, parameterTypes.length)
                .mapToObj(index -> new ParameterInfo("arg" + index,
                        typeRef(parameterTypes[index], false), false)).toList();
        return new ConstructorInfo(visibility, parameters, List.of(), List.of(), null, null);
    }

    private static MethodInfo method(String name, String returnType, Visibility visibility,
                                     String... parameterTypes) {
        List<ParameterInfo> parameters = java.util.stream.IntStream.range(0, parameterTypes.length)
                .mapToObj(index -> new ParameterInfo("arg" + index,
                        typeRef(parameterTypes[index], !isBuiltIn(parameterTypes[index])), false)).toList();
        return new MethodInfo(name, typeRef(returnType, !isBuiltIn(returnType)), visibility, List.of(),
                parameters, List.of(), List.of(), true, null, null);
    }

    private static TypeRef listOf(String element) {
        return new TypeRef("List", null, false, List.of(typeRef(element, true)), 0);
    }

    private static TypeRef typeRef(String name, boolean projectType) {
        String simpleName = name.substring(Math.max(name.lastIndexOf('.'), name.lastIndexOf('$')) + 1);
        return new TypeRef(simpleName, projectType ? name : null, projectType, List.of(), 0);
    }

    private static boolean isBuiltIn(String type) {
        return List.of("void", "boolean", "byte", "short", "int", "long", "float", "double", "char",
                "String", "List").contains(type);
    }
}

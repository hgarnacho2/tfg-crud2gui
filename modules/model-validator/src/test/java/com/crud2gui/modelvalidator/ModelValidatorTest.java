package com.crud2gui.modelvalidator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crud2gui.model.Analysis;
import com.crud2gui.model.Confidence;
import com.crud2gui.model.ConstructorInfo;
import com.crud2gui.model.FieldInfo;
import com.crud2gui.model.MethodInfo;
import com.crud2gui.model.ParameterInfo;
import com.crud2gui.model.ManagerCandidate;
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
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelValidatorTest {

        @TempDir
        Path tempDir;

    @Test
        void acceptsRequiredContractAndReportsNonBlockingDifferences() throws Exception {
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
                                method("removeProject", "boolean", Visibility.PUBLIC, "demo.Project"),
                                method("getProjects", "List", Visibility.PUBLIC)), false),
                type("demo.Project", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), false),
                type("demo.Launcher", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), true)),
                List.of(
                        new Relation(RelationKind.AGGREGATION, "demo.Owner", "demo.Project", "projects",
                                "0..*", Confidence.HIGH, null),
                        new Relation(RelationKind.DEPENDENCY, "demo.Launcher", "demo.Owner", "main(owner)",
                                null, Confidence.HIGH, null)),
                List.of(), new Analysis(new ManagerCandidate("demo.Owner", 5.0, Confidence.HIGH,
                        List.of("coordina el sistema")), List.of(), List.of(), List.of("demo.Launcher"), false));

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("RELATION_KIND_UNCERTAIN")));
        assertTrue(result.relationshipDecisions().size() == 1);
        ModelValidator.RelationshipDecision decision = result.relationshipDecisions().get(0);
        assertTrue(decision.from().equals("Owner"));
        assertTrue(decision.to().equals("Project"));
        assertTrue(decision.via().equals("projects"));
        assertTrue(decision.expectedKind() == RelationKind.COMPOSITION);
        assertTrue(decision.inferredKind() == RelationKind.AGGREGATION);
        assertTrue(decision.status().equals("PENDING_CONFIRMATION"));
        assertTrue(decision.options().equals(List.of(RelationKind.COMPOSITION, RelationKind.AGGREGATION)));
        ModelValidator.Finding uncertainRelation = result.findings().stream()
                .filter(f -> f.code().equals("RELATION_KIND_UNCERTAIN"))
                .findFirst().orElseThrow();
        assertTrue(uncertainRelation.message().contains("tipo de relacion"));
        assertTrue(uncertainRelation.message().contains("COMPOSITION o AGGREGATION"));
        assertTrue(uncertainRelation.message().contains("Confirma el tipo"));
        assertTrue(result.findings().stream().anyMatch(f -> f.path()
                .equals("Owner -> Project a través de atributo projects")));
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("ADDITIONAL_METHOD")));
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_METHOD")
                && f.path().contains("findProject")));
        assertTrue(result.findings().stream().anyMatch(f -> f.severity() == ModelValidator.Severity.INFO
                && f.code().equals("LAUNCHER_EXCLUDED") && f.path().equals("Launcher")));

        Path reportFile = tempDir.resolve("output/pending-relationship.json");
        ModelComparisonMain.writeReport(reportFile, Path.of("expected.json"), Path.of("actual.json"),
                expected, actual, result);
        JsonNode report = new ObjectMapper().readTree(reportFile.toFile());
        assertTrue(report.get("valid").asBoolean());
        assertFalse(report.get("readyForCrud").asBoolean());
        assertTrue(report.get("relationshipDecisions").size() == 1);
        assertTrue(report.get("crudConfirmations").size() == 1);
        assertTrue(report.get("crudConfirmations").get(0).get("type").asText().equals("RELATIONSHIP_KIND"));
        assertTrue(report.get("crudConfirmations").get(0).get("stage").asText().equals("CRUD_GENERATION"));
        assertTrue(report.get("crudConfirmations").get(0).get("requiredForCrud").asBoolean());
    }

    @Test
    void keepsNonOwnershipRelationKindMismatchAsStructuralError() {
        List<TypeInfo> types = List.of(
                type("Child", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false),
                type("Parent", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false));
        ProjectModel expected = model(types, List.of(new Relation(RelationKind.INHERITANCE,
                "Child", "Parent", null, null, Confidence.HIGH, null)), List.of(), null);
        ProjectModel actual = model(types, List.of(new Relation(RelationKind.ASSOCIATION,
                "Child", "Parent", null, null, Confidence.HIGH, null)), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertFalse(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("RELATION_KIND")));
        assertTrue(result.relationshipDecisions().isEmpty());
    }

    @Test
    void excludesExpectedLauncherWithoutReportingMissingClass() {
        ProjectModel expected = model(List.of(
                type("Launcher", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false)),
                List.of(), List.of(), null);
        ProjectModel actual = model(List.of(
                type("demo.Launcher", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), true)),
                List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_CLASS")));
        assertTrue(result.findings().stream().anyMatch(f -> f.severity() == ModelValidator.Severity.INFO
                && f.code().equals("LAUNCHER_EXCLUDED") && f.path().equals("Launcher")));
    }

    @Test
    void validatesOptionalInheritanceWhenPresentAndAllowsItToBeAbsent() {
        List<TypeInfo> expectedTypes = List.of(
                type("AlmacenLenya", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false),
                subtype("AlmacenLenyaV2", "", "AlmacenLenya", List.of()));
        Relation inheritance = new Relation(RelationKind.INHERITANCE, "AlmacenLenyaV2", "AlmacenLenya",
                null, null, Confidence.HIGH, null);
        ProjectModel expected = model(expectedTypes, List.of(inheritance),
                List.of("OPCIONAL: AlmacenLenyaV2"), null);

        ProjectModel withOptionalType = model(expectedTypes, List.of(inheritance), List.of(), null);
        ModelValidator.Result presentResult = ModelValidator.validate(expected, withOptionalType);
        assertTrue(presentResult.isValid());
        assertFalse(presentResult.findings().stream().anyMatch(f -> f.code().equals("ADDITIONAL_TYPE")
                || f.code().equals("ADDITIONAL_RELATION")));

        ProjectModel withoutOptionalType = model(List.of(expectedTypes.get(0)), List.of(), List.of(), null);
        ModelValidator.Result absentResult = ModelValidator.validate(expected, withoutOptionalType);
        assertTrue(absentResult.isValid());
        assertFalse(absentResult.findings().stream().anyMatch(f -> f.code().equals("MISSING_CLASS")
                || f.code().equals("MISSING_RELATION")));
    }

    @Test
    void acceptsDifferentNameForUnspecifiedFieldAndReportsInfo() {
        ProjectModel expected = model(List.of(type("Warehouse", "", TypeKind.CLASS,
                List.of(field("availableQuantity", typeRef("int", false), Visibility.PRIVATE)), List.of(),
                List.of(method("getAvailableQuantity", "int", Visibility.PUBLIC),
                        method("setAvailableQuantity", "void", Visibility.PUBLIC, "int")), false)),
                List.of(), List.of("NOMBRE_ATRIBUTO_NO_ESPECIFICADO: Warehouse.availableQuantity"), null);
        ProjectModel actual = model(List.of(type("Warehouse", "", TypeKind.CLASS,
                List.of(field("stock", typeRef("int", false), Visibility.PRIVATE)), List.of(),
                List.of(method("getStock", "int", Visibility.PUBLIC),
                        method("setStock", "void", Visibility.PUBLIC, "int")), false)),
                List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.severity() == ModelValidator.Severity.INFO
                && f.code().equals("FIELD_NAME_UNSPECIFIED") && f.path().equals("Warehouse.stock")
                && f.message().contains("se acepta el campo stock")));
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_FIELD")
                || f.code().equals("ADDITIONAL_FIELD") || f.code().equals("MISSING_METHOD")
                || f.code().equals("ADDITIONAL_METHOD")));
    }

    @Test
    void doesNotGuessUnspecifiedFieldNameWhenSeveralFieldsMatch() {
        ProjectModel expected = model(List.of(type("Warehouse", "", TypeKind.CLASS,
                List.of(field("availableQuantity", typeRef("int", false), Visibility.PRIVATE)), List.of(),
                List.of(), false)), List.of(),
                List.of("NOMBRE_ATRIBUTO_NO_ESPECIFICADO: Warehouse.availableQuantity"), null);
        ProjectModel actual = model(List.of(type("Warehouse", "", TypeKind.CLASS,
                List.of(field("stock", typeRef("int", false), Visibility.PRIVATE),
                        field("reserved", typeRef("int", false), Visibility.PRIVATE)), List.of(),
                List.of(), false)), List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertFalse(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("FIELD_NAME_UNSPECIFIED")
                && f.message().contains("no se encontro un unico atributo compatible")));
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_FIELD")));
    }

    @Test
    void allowsDisablingOnlySelectedValidationCategories() throws Exception {
        ValidationOptions options = ValidationOptions.fromJson(new ObjectMapper().readTree(
                "{\"types\":{\"enums\":false},\"attributes\":false}"));
        ProjectModel expected = model(List.of(
                type("Account", "", TypeKind.CLASS,
                        List.of(field("email", typeRef("String", false), Visibility.PRIVATE)),
                        List.of(), List.of(), false),
                type("Status", "", TypeKind.ENUM, List.of(), List.of(), List.of(), false)),
                List.of(), List.of(), null);
        ProjectModel actual = model(List.of(
                type("Account", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false)),
                List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual, options);

        assertTrue(options.classes());
        assertFalse(options.enums());
        assertFalse(options.attributes());
        assertTrue(options.methods());
        assertTrue(result.isValid());
    }

    @Test
    void canDisableAbstractnessRelationshipsAndMemberChecks() throws Exception {
        ValidationOptions options = ValidationOptions.fromJson(new ObjectMapper().readTree(
                "{\"types\":{\"abstractness\":false},\"relationships\":false,"
                        + "\"attributes\":false,\"constructors\":false,\"methods\":false,"
                        + "\"enumConstants\":false,\"collectionOperations\":false}"));
        TypeInfo expectedType = new TypeInfo("Account", "Account", "", TypeKind.CLASS, Visibility.PUBLIC,
                List.of(com.crud2gui.model.Modifier.ABSTRACT), null, null, List.of(), List.of(), List.of(),
                null, null, false, List.of(field("email", typeRef("String", false), Visibility.PRIVATE)),
                List.of(constructor(Visibility.PUBLIC, "String")),
                List.of(method("close", "void", Visibility.PUBLIC)), List.of());
        ProjectModel expected = model(List.of(expectedType), List.of(), List.of(), null);
        ProjectModel actual = model(List.of(type("Account", "", TypeKind.CLASS, List.of(), List.of(),
                List.of(), false)), List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual, options);

        assertTrue(result.isValid());
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

        @Test
        void identifiesMissingDeclarationKindWhenKnown() {
                for (TypeKind kind : TypeKind.values()) {
                        ProjectModel expected = model(List.of(
                                        type("MissingDeclaration", "", kind, List.of(), List.of(), List.of(), false)),
                                        List.of(), List.of(), null);
                        ProjectModel actual = model(List.of(), List.of(), List.of(), null);

                        ModelValidator.Result result = ModelValidator.validate(expected, actual);

                        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_" + kind.name())
                                        && f.message().contains(kind.name())));
                }

                                ProjectModel expectedWithoutKind = model(List.of(
                                        type("UnknownDeclaration", "", null, List.of(), List.of(), List.of(), false)),
                                        List.of(), List.of(), null);
                                ModelValidator.Result resultWithoutKind = ModelValidator.validate(expectedWithoutKind,
                                        model(List.of(), List.of(), List.of(), null));

                                assertTrue(resultWithoutKind.findings().stream().anyMatch(f -> f.code().equals("MISSING_TYPE")));
        }

    @Test
    void rejectsAdditionalTypesAndFields() {
        ProjectModel expected = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(field("email", typeRef("String", false), Visibility.PRIVATE)), List.of(),
                List.of(), false)), List.of(), List.of(), null);
        ProjectModel actual = model(List.of(
                type("Account", "", TypeKind.CLASS,
                        List.of(field("email", typeRef("String", false), Visibility.PRIVATE),
                                field("notes", typeRef("String", false), Visibility.PRIVATE)),
                        List.of(), List.of(), false),
                type("AuditRecord", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false)),
                List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertFalse(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("ADDITIONAL_FIELD")));
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("ADDITIONAL_TYPE")));
    }

    @Test
    void allowsDeclaredOptionalTypesAndFieldsToBeAbsent() {
        ProjectModel expected = model(List.of(
                type("Account", "", TypeKind.CLASS,
                        List.of(field("email", typeRef("String", false), Visibility.PRIVATE),
                                field("nickname", typeRef("String", false), Visibility.PRIVATE)),
                        List.of(), List.of(), false),
                type("AuditRecord", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false)),
                List.of(), List.of("OPCIONAL: Account.nickname", "OPCIONAL: AuditRecord"), null);
        ProjectModel actual = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(field("email", typeRef("String", false), Visibility.PRIVATE)),
                List.of(), List.of(), false)), List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
    }

    @Test
    void acceptsRequiredMethodInheritedFromSuperclass() {
        TypeInfo expectedProduct = type("Producto", "", TypeKind.CLASS, List.of(), List.of(),
                List.of(method("print", "void", Visibility.PUBLIC)), false);
        TypeInfo expectedElectronic = subtype("ProductoElectronica", "", "Producto",
                List.of(method("print", "void", Visibility.PUBLIC)));
        ProjectModel expected = model(List.of(expectedProduct, expectedElectronic), List.of(), List.of(), null);

        TypeInfo actualProduct = type("demo.Producto", "demo", TypeKind.CLASS, List.of(), List.of(),
                List.of(method("print", "void", Visibility.PUBLIC)), false);
        TypeInfo actualElectronic = subtype("demo.ProductoElectronica", "demo", "demo.Producto", List.of());
        ProjectModel actual = model(List.of(actualProduct, actualElectronic), List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_METHOD")
                && f.path().equals("ProductoElectronica.print()")));
    }

    @Test
    void doesNotAcceptPrivateInheritedMethodAsRequiredMethod() {
        TypeInfo expectedBase = type("Base", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false);
        TypeInfo expectedChild = subtype("Child", "", "Base",
                List.of(method("print", "void", Visibility.PUBLIC)));
        ProjectModel expected = model(List.of(expectedBase, expectedChild), List.of(), List.of(), null);

        TypeInfo actualBase = type("demo.Base", "demo", TypeKind.CLASS, List.of(), List.of(),
                List.of(method("print", "void", Visibility.PRIVATE)), false);
        TypeInfo actualChild = subtype("demo.Child", "demo", "demo.Base", List.of());
        ProjectModel actual = model(List.of(actualBase, actualChild), List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertFalse(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_METHOD")
                && f.path().equals("Child.print()")));
    }

    @Test
    void doesNotWarnForAccessorsMatchingExpectedFields() {
        ProjectModel expected = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(field("name", typeRef("String", false), Visibility.PRIVATE)), List.of(),
                List.of(), false)), List.of(), List.of(), null);
        ProjectModel actual = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(field("name", typeRef("String", false), Visibility.PRIVATE)), List.of(),
                List.of(method("getName", "String", Visibility.PUBLIC),
                        method("setName", "void", Visibility.PUBLIC, "String")), false)),
                List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("ADDITIONAL_METHOD")));
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_GETTER")));
    }

    @Test
    void warnsWhenExpectedPrivateFieldHasNoGetter() {
        ProjectModel expected = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(field("name", typeRef("String", false), Visibility.PRIVATE)), List.of(),
                List.of(), false)), List.of(), List.of(), null);
        ProjectModel actual = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(field("name", typeRef("String", false), Visibility.PRIVATE)), List.of(),
                List.of(), false)), List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.severity() == ModelValidator.Severity.WARNING
                && f.code().equals("MISSING_GETTER") && f.path().equals("Account.name")));
    }

    @Test
    void additionalMethodMessagesIncludeVisibility() {
        ProjectModel expected = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(), List.of(), List.of(), false)), List.of(), List.of(), null);
        ProjectModel actual = model(List.of(type("Account", "", TypeKind.CLASS,
                List.of(), List.of(), List.of(
                        method("publicHelper", "void", Visibility.PUBLIC),
                        method("privateHelper", "void", Visibility.PRIVATE)), false)),
                List.of(), List.of(), null);

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.findings().stream().anyMatch(f -> f.path().equals("Account.publicHelper()")
                && f.message().contains("publico")));
        assertTrue(result.findings().stream().anyMatch(f -> f.path().equals("Account.privateHelper()")
                && f.message().contains("privado")));
    }

    @Test
    void reportsConfirmedExpectedManagerSeparatelyFromStructuralResult() {
        TypeInfo expectedType = type("Gestor", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false);
        TypeInfo actualType = type("demo.Gestor", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), false);
        ProjectModel expected = new ProjectModel("1.0", "Gestor", new SourceInfo(null, null, 0, List.of()),
                List.of(expectedType), List.of(), null, null);
        Analysis analysis = new Analysis(new ManagerCandidate("demo.Gestor", 8.0, Confidence.HIGH,
                List.of("alcance")), List.of(), List.of(), List.of(), true);
        ProjectModel actual = new ProjectModel("1.0", null, new SourceInfo(null, null, 0, List.of()),
                List.of(actualType), List.of(), analysis, null);

        ModelValidator.Result structuralResult = ModelValidator.validate(expected, actual);
        var manager = ModelComparisonMain.managerSelection(expected, actual);

        assertTrue(structuralResult.isValid());
        assertTrue(manager.get("status").asText().equals("MATCH"));
        assertTrue(manager.get("manualOverride").asBoolean());
        assertFalse(manager.get("requiresConfirmation").asBoolean());
    }

    @Test
    void reportsManagerMismatchAsConfirmationWithoutInvalidatingStructure() {
        TypeInfo expectedManager = type("Gestor", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false);
        TypeInfo expectedAlternative = type("Alternativa", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false);
        ProjectModel expected = new ProjectModel("1.0", "Gestor", new SourceInfo(null, null, 0, List.of()),
                List.of(expectedManager, expectedAlternative), List.of(), null, null);
        Analysis analysis = new Analysis(new ManagerCandidate("demo.Alternativa", 4.0, Confidence.LOW,
                List.of("colecciones")), List.of(), List.of(), List.of(), false);
        ProjectModel actual = new ProjectModel("1.0", null, new SourceInfo(null, null, 0, List.of()),
                List.of(type("demo.Gestor", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), false),
                        type("demo.Alternativa", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), false)),
                List.of(), analysis, null);

        ModelValidator.Result structuralResult = ModelValidator.validate(expected, actual);
        var manager = ModelComparisonMain.managerSelection(expected, actual);

        assertTrue(structuralResult.isValid());
        assertTrue(manager.get("status").asText().equals("MISMATCH"));
        assertTrue(manager.get("requiresConfirmation").asBoolean());
    }

    @Test
        void reportsAmbiguousExpectedManagerAsConfirmationWithoutStructuralFailure() throws Exception {
        List<TypeInfo> expectedTypes = List.of(
                type("Platform", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false),
                type("Catalog", "", TypeKind.CLASS, List.of(), List.of(), List.of(), false));
        String ambiguity = "AMBIGUO: Gestora: Platform, Catalog; ambas coordinan el sistema.";
        ProjectModel expected = model(expectedTypes, List.of(), List.of(ambiguity), null);

        Analysis analysis = new Analysis(
                new ManagerCandidate("demo.Platform", 5.0, Confidence.HIGH, List.of("gestiona catalogo")),
                List.of(new ManagerCandidate("demo.Catalog", 4.0, Confidence.HIGH, List.of("gestiona elementos"))),
                List.of(), List.of(), false);
        ProjectModel actual = model(List.of(
                type("demo.Platform", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), false),
                type("demo.Catalog", "demo", TypeKind.CLASS, List.of(), List.of(), List.of(), false)),
                List.of(), List.of(), analysis);

        ModelValidator.Result structuralResult = ModelValidator.validate(expected, actual);
        var manager = ModelComparisonMain.managerSelection(expected, actual);

        assertTrue(structuralResult.isValid());
        assertTrue(manager.get("status").asText().equals("AMBIGUOUS_EXPECTATION"));
        assertTrue(manager.get("requiresConfirmation").asBoolean());
        assertTrue(manager.get("expectedManagerAmbiguities").get(0).asText().contains("Platform, Catalog"));

        Path reportFile = tempDir.resolve("output/manager-confirmation.json");
        ModelComparisonMain.writeReport(reportFile, Path.of("expected.json"), Path.of("actual.json"),
                expected, actual, structuralResult);
        JsonNode report = new ObjectMapper().readTree(reportFile.toFile());
        assertFalse(report.get("readyForCrud").asBoolean());
        assertTrue(report.get("crudConfirmations").size() == 1);
        assertTrue(report.get("crudConfirmations").get(0).get("type").asText().equals("MANAGER_SELECTION"));
        assertTrue(report.get("crudConfirmations").get(0).get("stage").asText()
                .equals("BEFORE_CRUD_GENERATION"));
    }

    @Test
    void acceptsAddAndRemoveMethodsForDeclaredCollectionOperations() {
        ProjectModel expected = collectionOperationsModel(List.of(
                method("addProducto", "void", Visibility.PUBLIC, "demo.Producto"),
                method("removeProducto", "boolean", Visibility.PUBLIC, "int")));
        ProjectModel actual = collectionOperationsModel(List.of(
                method("addProducto", "void", Visibility.PUBLIC, "demo.Producto"),
                method("removeProducto", "boolean", Visibility.PUBLIC, "int")));

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertTrue(result.isValid());
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_COLLECTION_OPERATION")));
        assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("ADDITIONAL_METHOD")
                && (f.path().contains("addProducto") || f.path().contains("removeProducto"))));
    }

    @Test
    void rejectsMissingMethodForDeclaredCollectionOperation() {
        ProjectModel expected = collectionOperationsModel(List.of(
                method("addProducto", "void", Visibility.PUBLIC, "demo.Producto"),
                method("removeProducto", "boolean", Visibility.PUBLIC, "int")));
        ProjectModel actual = collectionOperationsModel(List.of(
                method("addProducto", "void", Visibility.PUBLIC, "demo.Producto")));

        ModelValidator.Result result = ModelValidator.validate(expected, actual);

        assertFalse(result.isValid());
        assertTrue(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_COLLECTION_OPERATION")
                && f.path().equals("Usuario.productos.remove")));
    }

        @Test
        void infersOperationsForPrivateCollectionWithoutWarningMarker() {
                List<MethodInfo> operations = List.of(
                                method("addProducto", "void", Visibility.PUBLIC, "demo.Producto"),
                                method("removeProducto", "boolean", Visibility.PUBLIC, "int"));
                ProjectModel expected = collectionOperationsModel(operations, false);
                ProjectModel actual = collectionOperationsModel(operations, false);

                ModelValidator.Result result = ModelValidator.validate(expected, actual);

                assertTrue(result.isValid());
                assertFalse(result.findings().stream().anyMatch(f -> f.code().equals("MISSING_COLLECTION_OPERATION")));
        }

        @Test
                void createsTraceableDefaultValidationReportName() {
                        Path reportFile = ModelComparisonMain.defaultReportPath(Path.of("enunciado_practica_23.json"));

                        assertTrue(reportFile.getParent().equals(Path.of("output")));
                        assertTrue(reportFile.getFileName().toString()
                                .matches("validation_enunciado_practica_23_\\d{8}_\\d{6}(?:_\\d+)?\\.json"));
                }

                @Test
                void writesValidationReportWithSummaryAndFindings() throws Exception {
                ModelValidator.Result result = new ModelValidator.Result(List.of(
                                new ModelValidator.Finding(ModelValidator.Severity.ERROR, "MISSING_TYPE", "Account",
                                                "No se encontro el tipo requerido en la implementacion."),
                                new ModelValidator.Finding(ModelValidator.Severity.WARNING, "RELATION_KIND_UNCERTAIN",
                                                "Account -> Order", "La propiedad no se puede confirmar.")));
                Path reportFile = tempDir.resolve("output/validation_report_test.json");

                ModelComparisonMain.writeReport(reportFile, Path.of("expected.json"), Path.of("actual.json"), result);

                JsonNode report = new ObjectMapper().readTree(reportFile.toFile());
                assertFalse(report.get("valid").asBoolean());
                assertFalse(report.has("result"));
                assertTrue(report.get("summary").get("errors").asInt() == 1);
                assertTrue(report.get("summary").get("warnings").asInt() == 1);
                assertTrue(report.get("findings").isArray());
                assertTrue(report.get("relationshipDecisions").isArray());
                assertTrue(report.get("validationOptions").get("attributes").asBoolean());
                assertFalse(report.get("readyForCrud").asBoolean());
                assertTrue(report.get("findings").get(0).get("code").asText().equals("MISSING_TYPE"));
                assertFalse(report.get("managerSelection").has("matchesExpected"));
        }

    private static ProjectModel model(List<TypeInfo> types, List<Relation> relations,
                                      List<String> warnings, Analysis analysis) {
        return new ProjectModel("1.0", new SourceInfo(null, null, 0, warnings), types, relations, analysis, null);
    }

    private static ProjectModel collectionOperationsModel(List<MethodInfo> userMethods) {
                return collectionOperationsModel(userMethods, true);
        }

        private static ProjectModel collectionOperationsModel(List<MethodInfo> userMethods, boolean markOperations) {
        return model(List.of(
                type("demo.Usuario", "demo", TypeKind.CLASS,
                        List.of(field("productos", listOf("demo.Producto"), Visibility.PRIVATE)),
                        List.of(), userMethods, false),
                type("demo.Producto", "demo", TypeKind.CLASS,
                        List.of(field("id", typeRef("int", false), Visibility.PRIVATE)),
                        List.of(), List.of(), false)), List.of(),
                markOperations ? List.of("OPERACIONES_LISTA: Usuario.productos(add,remove)") : List.of(), null);
    }

    private static TypeInfo type(String id, String packageName, TypeKind kind, List<FieldInfo> fields,
                                 List<ConstructorInfo> constructors, List<MethodInfo> methods,
                                 boolean hasMainMethod) {
        String name = id.substring(Math.max(id.lastIndexOf('.'), id.lastIndexOf('$')) + 1);
        return new TypeInfo(id, name, packageName, kind, Visibility.PUBLIC, List.of(), null, null,
                List.of(), List.of(), List.of(), null, null, hasMainMethod, fields, constructors,
                methods, List.of());
    }

        private static TypeInfo subtype(String id, String packageName, String superclass,
                                                                        List<MethodInfo> methods) {
                String name = id.substring(Math.max(id.lastIndexOf('.'), id.lastIndexOf('$')) + 1);
                return new TypeInfo(id, name, packageName, TypeKind.CLASS, Visibility.PUBLIC, List.of(), null,
                                typeRef(superclass, true), List.of(), List.of(), List.of(), null, null, false,
                                List.of(), List.of(), methods, List.of());
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

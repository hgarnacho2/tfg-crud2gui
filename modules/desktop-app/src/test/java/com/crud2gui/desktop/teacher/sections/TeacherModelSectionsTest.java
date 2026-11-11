package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.authenticator.entraid.AuthenticatedUser;
import com.crud2gui.desktop.teacher.TeacherDashboardView;
import com.crud2gui.desktop.teacher.TeacherWorkflowContext;
import com.crud2gui.desktop.teacher.sections.TeacherContextSection;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherModelSectionsTest {

    @TempDir
    Path directory;

    @BeforeAll
    static void startJavaFx() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> started.complete(null));
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> started.complete(null));
        }
        started.get(10, TimeUnit.SECONDS);
        Platform.setImplicitExit(false);
    }

    @Test
        void selectsBothModelsRunsValidationAndDisplaysReport() throws Exception {
                String expectedJson = """
                                {
                                    "schemaVersion": "1.0",
                                    "expectedManagerId": "Owner",
                                    "source": { "filesParsed": 1, "warnings": [] },
                                    "types": [{
                                        "id": "demo.Owner", "name": "Owner", "packageName": "demo",
                                        "kind": "CLASS", "visibility": "PUBLIC", "modifiers": [],
                                        "hasMainMethod": false, "fields": [], "constructors": [],
                                        "methods": [], "enumConstants": [], "interfaces": []
                                    }],
                                    "relations": []
                                }
                                """;
                String actualJson = """
                                {
                                    "schemaVersion": "1.0",
                                    "expectedManagerId": "Owner",
                                    "source": { "filesParsed": 1, "warnings": [] },
                                    "types": [{
                                        "id": "demo.Owner", "name": "Owner", "packageName": "demo",
                                        "kind": "CLASS", "visibility": "PUBLIC", "modifiers": [],
                                        "hasMainMethod": false, "fields": [], "constructors": [],
                                        "methods": [], "enumConstants": [], "interfaces": []
                                    }],
                                    "relations": [],
                                    "analysis": {
                                        "managerClass": { "id": "demo.Owner", "score": 1.0,
                                            "confidence": "HIGH", "reasons": ["Clase principal del dominio"] },
                                        "alternatives": [], "subManagers": [], "entryPoints": [], "manualOverride": false
                                    }
                                }
                                """;
                Path source = Files.writeString(directory.resolve("source.json"), actualJson);
                Path inferred = Files.writeString(directory.resolve("inferred.json"), expectedJson);
                Path reportDirectory = directory.resolve("reports");
                Path initialDirectory = directory.resolve("initial-reports");
                AtomicReference<File> folderSelection = new AtomicReference<>();
                CompletableFuture<Void> validationFinished = new CompletableFuture<>();
                AtomicReference<ModelValidationSection> sectionReference = new AtomicReference<>();
        AtomicReference<File> selection = new AtomicReference<>(source.toFile());
        onFx(() -> {
                        ModelValidationSection section = new ModelValidationSection(owner -> selection.get(),
                                        () -> initialDirectory, (owner, current) -> folderSelection.get());
                    sectionReference.set(section);
            attach(section);
                assertEquals("Selecciona los modelos JSON de código y enunciado para preparar su validación.",
                    ((Label) section.lookup(".section-subtitle")).getText());
            Button first = button(section, "model-validation-step-java");
            Button second = button(section, "model-validation-step-statement");
            Button result = button(section, "model-validation-step-result");
                assertEquals("Modelo extra\u00eddo del c\u00f3digo",
                    ((Label) first.lookup(".statement-step-description")).getText());
                assertEquals("RESULTADO DE LA VALIDACI\u00d3N",
                    ((Label) result.lookup(".statement-step-title")).getText());
                assertEquals("JSON extra\u00eddo de los fuentes Java.",
                    ((Label) section.lookup("#model-validation-java").lookup(".panel-subtitle")).getText());
                assertEquals("Ning\u00fan archivo seleccionado.",
                    ((Label) section.lookup("#model-validation-java-filename")).getText());
            assertTrue(second.isDisabled());
            assertTrue(result.isDisabled());
            assertFalse(button(section, "model-validation-run").getParent().getParent().isVisible());
            button(section, "model-validation-java-select").fire();
            assertFalse(second.isDisabled());
            assertTrue(first.getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
                ModelJsonSelection sourceSelection =
                    (ModelJsonSelection) section.lookup("#model-validation-java");
                assertTrue(sourceSelection.lookup("#model-validation-java-preview").isVisible());
                assertFalse(((javafx.scene.control.TextArea) sourceSelection.lookup(".code")).isEditable());
                assertEquals("Modelo cargado correctamente. Puedes continuar con el paso 2: Modelo inferido enunciado.",
                    ((Label) sourceSelection.lookup("#model-validation-java-status")).getText());
                Label filename = (Label) sourceSelection.lookup("#model-validation-java-filename");
                assertTrue(filename.getGraphic().getStyleClass().contains("statement-neutral-badge"));
            second.fire();
            assertFalse(section.lookup("#model-validation-java").getParent().isVisible());
            assertTrue(section.lookup("#model-validation-statement").getParent().isVisible());
            selection.set(inferred.toFile());
            button(section, "model-validation-statement-select").fire();
            assertEquals(source, ((ModelJsonSelection) section.lookup("#model-validation-java")).getSelectedFile());
            assertEquals(inferred,
                    ((ModelJsonSelection) section.lookup("#model-validation-statement")).getSelectedFile());
            assertTrue(second.getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
                ModelJsonSelection inferredSelection =
                    (ModelJsonSelection) section.lookup("#model-validation-statement");
                assertTrue(inferredSelection.lookup("#model-validation-statement-preview").isVisible());
                assertFalse(((javafx.scene.control.TextArea) inferredSelection.lookup(".code")).isEditable());
                assertEquals("Modelo inferido cargado correctamente. Puedes continuar con el paso 3: Resultado de la validación.",
                    ((Label) inferredSelection.lookup("#model-validation-statement-status")).getText());
            assertFalse(result.isDisabled());
            assertFalse(Files.exists(initialDirectory));
            assertFalse(Files.exists(reportDirectory));
            assertNull(section.lookup("#model-validation-result-view"));
            result.fire();
            assertNotNull(section.lookup("#model-validation-options"));
            assertFalse(section.lookup("#model-validation-result-title").getParent().isManaged());
            var methodsOption = (javafx.scene.control.CheckBox) section.lookup("#validation-option-methods");
            methodsOption.setSelected(true);
            assertTrue(button(section, "model-validation-run").getParent().getParent().isVisible());
            assertEquals("Validar modelos", button(section, "model-validation-run").getText());
            button(section, "model-validation-change-output-folder").fire();
            assertEquals(initialDirectory.toString(),
                    ((Label) section.lookup("#model-validation-output-path")).getText());
            folderSelection.set(reportDirectory.toFile());
            button(section, "model-validation-change-output-folder").fire();
            assertEquals(reportDirectory.toString(),
                    ((Label) section.lookup("#model-validation-output-path")).getText());
                Button openOutputFolder = button(section, "model-validation-open-output-folder");
                assertEquals("Abrir carpeta de salida", openOutputFolder.getText());
                assertTrue(openOutputFolder.isVisible());
            result.disabledProperty().addListener((observable, wasDisabled, isDisabled) -> {
                if (!isDisabled) {
                    validationFinished.complete(null);
                }
            });
            button(section, "model-validation-run").fire();
            assertTrue(result.isDisabled());
            assertTrue(section.lookup("#model-validation-options").isDisabled());
            return null;
        });
        validationFinished.get(20, TimeUnit.SECONDS);
        onFx(() -> {
            ModelValidationSection section = sectionReference.get();
            Button result = button(section, "model-validation-step-result");
            assertFalse(result.isDisabled());
            assertTrue(result.getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
            var reportView = section.lookup("#model-validation-result-view");
            assertNotNull(reportView);
                section.getScene().getRoot().applyCss();
                var resultPanel = (javafx.scene.layout.VBox) section.lookup(".model-validation-result-panel");
                assertEquals(14, resultPanel.getSpacing());
                assertEquals(14, ((ModelJsonSelection) section.lookup("#model-validation-java")).getSpacing());
                assertEquals(14, ((ModelJsonSelection) section.lookup("#model-validation-statement")).getSpacing());
                Label savedStatus = (Label) reportView.lookup("#model-validation-report-path");
                assertTrue(savedStatus.getStyleClass().contains("status-message"));
                assertTrue(savedStatus.isWrapText());
                assertEquals(15, savedStatus.getFont().getSize());
                assertEquals(javafx.scene.paint.Color.web("#e4f3ec"),
                    savedStatus.getBackground().getFills().get(0).getFill());
                assertEquals(16, ((Label) reportView.lookup(".model-validation-block-title")).getFont().getSize());
                var accordion = (javafx.scene.control.TitledPane) reportView.lookup("#model-validation-findings");
                assertTrue(accordion.getText().startsWith("Informe de la validación ("));
                assertEquals(16, accordion.getFont().getSize());
                assertEquals(6, accordion.getBorder().getStrokes().get(0).getRadii().getTopLeftHorizontalRadius());
                assertTrue(accordion.lookup(".title .text") instanceof javafx.scene.text.Text);
                assertTrue(((javafx.scene.text.Text) accordion.lookup(".title .text")).getFont().getStyle()
                    .contains("Bold"));
                assertEquals(16, ((javafx.scene.text.Text) accordion.lookup(".title .text")).getFont().getSize());
                var confirmations = (javafx.scene.control.TitledPane)
                    reportView.lookup("#model-validation-confirmations");
                assertEquals(16, ((javafx.scene.text.Text) confirmations.lookup(".title .text"))
                    .getFont().getSize());
                assertEquals("No hay confirmaciones pendientes.",
                    ((Label) confirmations.lookup(".text-muted")).getText());
                var relationships = (javafx.scene.control.TitledPane)
                    reportView.lookup("#model-validation-relationships");
                var relationshipsTitle = (javafx.scene.text.Text) relationships.lookup(".title .text");
                assertTrue(relationships.getText().startsWith("Avisos de relaciones entre clases ("));
                assertTrue(relationships.isExpanded());
                assertEquals(16, relationshipsTitle.getFont().getSize());
            assertEquals("Estructura válida",
                    ((Label) reportView.lookup("#model-validation-outcome-title")).getText());
            assertEquals("Listo para CRUD",
                    ((Label) reportView.lookup("#model-validation-readiness")).getText());
            assertTrue(((Label) reportView.lookup("#model-validation-report-path")).getText()
                    .contains(reportDirectory.toString()));
                assertFalse(button(section, "model-validation-run").isDisabled());
                assertTrue(((Label) section.lookup("#model-validation-output-name")).getText().endsWith(".json"));
                assertFalse(Files.exists(initialDirectory));
            return null;
        });
        try (var reports = Files.list(reportDirectory)) {
            Path report = reports.findFirst().orElseThrow();
            assertTrue(Files.readString(report).contains("\"readyForCrud\" : true"));
            var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(report.toFile());
            assertTrue(json.path("validationOptions").path("methods").asBoolean());
        }
        Path newDestination = directory.resolve("new-reports");
        CompletableFuture<Void> repeatedValidation = new CompletableFuture<>();
        onFx(() -> {
            ModelValidationSection section = sectionReference.get();
            Button result = button(section, "model-validation-step-result");
            result.disabledProperty().addListener((observable, previous, disabled) -> {
                if (!disabled) {
                    repeatedValidation.complete(null);
                }
            });
            folderSelection.set(newDestination.toFile());
            ((javafx.scene.control.CheckBox) section.lookup("#validation-option-methods")).setSelected(false);
            button(section, "model-validation-change-output-folder").fire();
            button(section, "model-validation-run").fire();
            assertTrue(button(section, "model-validation-change-output-folder").isDisabled());
            return null;
        });
        repeatedValidation.get(20, TimeUnit.SECONDS);
        try (var reports = Files.list(newDestination)) {
            Path report = reports.findFirst().orElseThrow();
            var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(report.toFile());
            assertFalse(json.path("validationOptions").path("methods").asBoolean());
        }
        try (var reports = Files.list(reportDirectory)) {
            assertEquals(1, reports.count());
        }
    }

        @Test
        void showsTheSourceOfEachRelationshipKind() throws Exception {
                var report = new com.fasterxml.jackson.databind.ObjectMapper().readTree("""
                                {
                                    "valid": true,
                                    "readyForCrud": false,
                                    "summary": {"errors": 0, "warnings": 1, "info": 0},
                                    "managerSelection": {},
                                    "relationshipDecisions": [{
                                        "from": "Usuario",
                                        "to": "Producto",
                                        "via": "productos",
                                        "expectedKind": "COMPOSITION",
                                        "inferredKind": "ASSOCIATION",
                                        "status": "PENDING_CONFIRMATION"
                                    }],
                                    "crudConfirmations": [],
                                    "findings": []
                                }
                                """);

                onFx(() -> {
                        ModelValidationResultView view = new ModelValidationResultView(report, directory.resolve("report.json"));
                        attach(view);

                        var relationships = (javafx.scene.control.TitledPane) view.lookup("#model-validation-relationships");
                        assertEquals("ATRIBUTO", ((Label) relationships.lookup(
                            ".model-validation-relation-attribute-badge")).getText());
                        assertEquals("productos", ((Label) relationships.lookup(".model-validation-detail"))
                            .getText());
                        assertEquals("Se confirmará al generar el CRUD",
                            ((Label) relationships.lookup(".model-validation-pending-badge")).getText());
                    var kinds = relationships.lookup(".model-validation-relation-kinds");
                    assertEquals("ENUNCIADO", ((Label) kinds.lookup(".model-validation-relation-origin-enunciado"))
                        .getText());
                    assertEquals("Composición", ((Label) kinds.lookup(".model-validation-relation-kind-statement"))
                        .getText());
                    assertEquals("CÓDIGO", ((Label) kinds.lookup(".model-validation-relation-origin-code"))
                        .getText());
                    assertEquals("Asociación", ((Label) kinds.lookup(".model-validation-relation-kind-source"))
                        .getText());
                        return null;
                });
        }

        @Test
        void changingSourceModelKeepsSelectedExpectedModelAndEnablesResultStep() throws Exception {
        Path source = Files.writeString(directory.resolve("source.json"), "{}");
        Path replacement = Files.writeString(directory.resolve("source-replacement.json"), "{}");
        Path inferred = Files.writeString(directory.resolve("inferred.json"), "{}");
        AtomicReference<File> selection = new AtomicReference<>(source.toFile());

        onFx(() -> {
            ModelValidationSection section = new ModelValidationSection(owner -> selection.get(),
                () -> directory, (owner, current) -> null);
            attach(section);

            button(section, "model-validation-java-select").fire();
            button(section, "model-validation-step-statement").fire();
            selection.set(inferred.toFile());
            button(section, "model-validation-statement-select").fire();
            assertFalse(button(section, "model-validation-step-result").isDisabled());

            button(section, "model-validation-step-java").fire();
            selection.set(replacement.toFile());
            button(section, "model-validation-java-select").fire();

            assertEquals(replacement, ((ModelJsonSelection) section.lookup("#model-validation-java"))
                .getSelectedFile());
            assertEquals(inferred, ((ModelJsonSelection) section.lookup("#model-validation-statement"))
                .getSelectedFile());
            assertFalse(button(section, "model-validation-step-result").isDisabled());
            assertFalse(button(section, "model-validation-run").isDisabled());
            return null;
        });
        }

        @Test
        void configuresEveryValidatorOptionVisually() throws Exception {
        onFx(() -> {
            var initial = com.crud2gui.modelvalidator.ValidationOptions.defaults();
            var editor = new com.crud2gui.desktop.ui.ValidationOptionsView(initial);
            attach(editor);
            assertEquals(initial, editor.getOptions());
            var controls = editor.lookupAll(".check-box");
            assertEquals(12, controls.size());
            controls.forEach(control -> ((javafx.scene.control.CheckBox) control).setSelected(false));
            assertEquals(new com.crud2gui.modelvalidator.ValidationOptions(false, false, false, false,
                    false, false, false, false, false, false, false, false), editor.getOptions());
            assertEquals(com.crud2gui.modelvalidator.ValidationOptions.defaults(), initial);
            return null;
        });
    }

    @Test
    void crudSelectionSurvivesCancellationAndInvalidFiles() throws Exception {
        Path model = Files.writeString(directory.resolve("validated.JSON"), "{}");
        Path invalid = Files.writeString(directory.resolve("model.txt"), "{}");
        AtomicReference<File> selection = new AtomicReference<>();
        onFx(() -> {
            CrudSection section = new CrudSection(owner -> selection.get());
            attach(section);
            ModelJsonSelection selector = (ModelJsonSelection) section.lookup("#crud-model");
            Button select = button(section, "crud-model-select");
                assertEquals("JSON validado extra\u00eddo de los fuentes Java.",
                    ((Label) selector.lookup(".panel-subtitle")).getText());
                assertEquals("Ning\u00fan archivo seleccionado.",
                    ((Label) section.lookup("#crud-model-filename")).getText());
            select.fire();
            assertNull(selector.getSelectedFile());
            selection.set(invalid.toFile());
            select.fire();
            assertNull(selector.getSelectedFile());
            Label status = (Label) section.lookup("#crud-model-status");
            assertTrue(status.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            selection.set(model.toFile());
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            assertEquals("validated.JSON", ((Label) section.lookup("#crud-model-filename")).getText());
            assertEquals("", status.getText());
            assertFalse(status.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            assertTrue(button(section, "crud-step-model").getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("completed")));
            selection.set(null);
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            selection.set(directory.resolve("missing.json").toFile());
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            selection.set(directory.toFile());
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            return null;
        });
    }

    @Test
    void teacherMenuOpensAndCachesBothSections() throws Exception {
        onFx(() -> {
            AuthenticatedUser user = new AuthenticatedUser("teacher", "teacher", "Docente", null,
                    Set.of("docente"), null);
            TeacherDashboardView dashboard = new TeacherDashboardView(user, () -> { });
            attach(dashboard);
            Button context = navigation(dashboard, "Contexto");
            Button validation = navigation(dashboard, "Validar modelos");
            Button crud = navigation(dashboard, "CRUD");
            ScrollPane content = (ScrollPane) dashboard.getCenter();
            Node contextPage = content.getContent();
            assertTrue(contextPage instanceof TeacherContextSection);
            context.fire();
            assertSame(contextPage, content.getContent());
            validation.fire();
            Node validationPage = content.getContent();
            assertTrue(validationPage instanceof ModelValidationSection);
            crud.fire();
            Node crudPage = content.getContent();
            assertTrue(crudPage instanceof CrudSection);
            assertNotNull(crudPage.lookup("#crud-model-select"));
            validation.fire();
            assertSame(validationPage, content.getContent());
            crud.fire();
            assertSame(crudPage, content.getContent());
            return null;
        });
    }

        @Test
        void rendersWorkflowFilesInSeparateContextBranches() throws Exception {
        Path pdf = Files.writeString(directory.resolve("enunciado.pdf"), "pdf");
        Path extracted = Files.writeString(directory.resolve("enunciado.txt"), "texto");
        Path filtered = Files.writeString(directory.resolve("enunciado-filtrado.txt"), "filtrado");
        Path prompt = Files.writeString(directory.resolve("prompt-v6.txt"), "instrucciones");
        Path inferred = Files.writeString(directory.resolve("modelo-enunciado.json"), "{}");
        Path refined = Files.writeString(directory.resolve("modelo-enunciado-validated.json"), "{}");
        Path codeFolder = Files.createDirectory(directory.resolve("src"));
        Path codeModel = Files.writeString(directory.resolve("modelo-codigo.json"), "{}");
        Path report = Files.writeString(directory.resolve("validacion.json"), "{}");
        TeacherWorkflowContext workflow = new TeacherWorkflowContext(directory.resolve("workflow.json"));
        workflow.selectStatementPdf(pdf);
        workflow.recordExtractedText(extracted);
        workflow.recordFilteredText(filtered);
        workflow.selectPromptFile(prompt);
        workflow.recordGeneratedStatementModel(inferred);
        workflow.recordConfirmedStatementModel(refined);
        workflow.selectCodeFolder(codeFolder);
        workflow.recordCodeModel(codeModel);
        workflow.recordValidationReport(report);

        onFx(() -> {
            TeacherContextSection section = new TeacherContextSection(workflow);
            StackPane root = new StackPane(section);
            new Scene(root, 1200, 900);
            root.resize(1200, 900);
            root.applyCss();
            root.layout();
            assertEquals("enunciado.pdf",
                ((Label) section.lookup("#teacher-context-stage-statement-pdf-file")).getText());
            assertEquals("enunciado.txt",
                ((Label) section.lookup("#teacher-context-stage-statement-extracted-file")).getText());
            assertEquals("enunciado-filtrado.txt",
                ((Label) section.lookup("#teacher-context-stage-statement-filtered-file")).getText());
            assertEquals("modelo-enunciado.json",
                ((Label) section.lookup("#teacher-context-stage-statement-model-file")).getText());
            assertEquals("modelo-enunciado-validated.json",
                ((Label) section.lookup("#teacher-context-stage-statement-refined-model-file")).getText());
            VBox refinedModelStage = (VBox) section.lookup("#teacher-context-stage-statement-refined-model");
            assertEquals("Modelo revisado del enunciado",
                    ((Label) refinedModelStage.lookup(".workflow-context-stage-title")).getText());
                VBox inferredModelStage = (VBox) section.lookup("#teacher-context-stage-statement-model");
                assertEquals("PROMPT",
                    ((Label) inferredModelStage.lookup("#teacher-context-model-prompt-badge")).getText());
                assertEquals("prompt-v6.txt",
                    ((Label) inferredModelStage.lookup("#teacher-context-model-prompt-file")).getText());
            assertEquals("src",
                ((Label) section.lookup("#teacher-context-stage-code-folder-file")).getText());
            assertEquals(((VBox) section.lookup("#teacher-context-stage-statement-pdf")).getWidth(),
                    ((VBox) section.lookup("#teacher-context-stage-code-folder")).getWidth(), 1);
            assertEquals("modelo-codigo.json",
                ((Label) section.lookup("#teacher-context-stage-code-model-file")).getText());
            assertEquals("validacion.json",
                ((Label) section.lookup("#teacher-context-stage-validation-report-file")).getText());
                assertEquals("Entradas para validar",
                    ((Label) section.lookup(".workflow-context-validation-inputs-title")).getText());
            for (String stage : java.util.List.of("statement-pdf", "statement-extracted", "statement-filtered",
                    "statement-model", "statement-refined-model", "code-folder", "code-model",
                    "validation-report")) {
                Button reveal = (Button) section.lookup("#teacher-context-stage-" + stage + "-reveal");
                assertNotNull(reveal);
                assertTrue(reveal.isVisible());
                assertFalse(reveal.isDisabled());
            }
            assertFalse(((Button) section.lookup("#teacher-context-stage-crud-reveal")).isManaged());
            assertFalse(((Button) section.lookup("#teacher-context-validation-input-code-reveal")).isDisabled());
            assertFalse(((Button) section.lookup("#teacher-context-validation-input-statement-reveal")).isDisabled());
            Label codeBadge = (Label) section.lookup("#teacher-context-validation-input-code-badge");
            Label statementBadge = (Label) section.lookup("#teacher-context-validation-input-statement-badge");
            assertEquals("CODIGO", codeBadge.getText());
            assertEquals("ENUNCIADO", statementBadge.getText());
            assertTrue(codeBadge.getStyleClass().contains("workflow-context-input-badge"));
            assertTrue(statementBadge.getStyleClass().contains("workflow-context-input-badge"));
            assertEquals("modelo-codigo.json",
                ((Label) section.lookup("#teacher-context-validation-input-code-file")).getText());
            assertEquals("modelo-enunciado-validated.json",
                ((Label) section.lookup("#teacher-context-validation-input-statement-file")).getText());
            assertTrue(((Label) section.lookup("#teacher-context-stage-crud-file")).getText()
                .contains("más adelante"));
            assertNull(section.lookup(".teacher-context-convergence"));
                assertTrue(section.lookup("#teacher-context-merge-code").getBoundsInLocal().getHeight()
                    > section.lookup("#teacher-context-merge-statement").getBoundsInLocal().getHeight());
                assertNotNull(section.lookup("#teacher-context-merge-junction"));
                assertNotNull(section.lookup("#teacher-context-validation-arrow"));
                assertNull(section.lookup(".teacher-context-arrow-head"));
            return null;
        });
        }

    @Test
    void showsPromptBadgeAndEmptyStateWhenNoPromptIsSelected() throws Exception {
        TeacherWorkflowContext workflow = new TeacherWorkflowContext(directory.resolve("empty-workflow.json"));
        TeacherContextSection section = onFx(() -> {
            TeacherContextSection created = new TeacherContextSection(workflow);
            StackPane root = new StackPane(created);
            Scene scene = new Scene(root, 1200, 900);
            scene.getStylesheets().add(TeacherModelSectionsTest.class.getResource("/styles/app.css").toExternalForm());
            root.applyCss();
            root.layout();
            return created;
        });

        onFx(() -> {
            VBox inferredModelStage = (VBox) section.lookup("#teacher-context-stage-statement-model");
            Label stageTitle = (Label) inferredModelStage.lookup(".workflow-context-stage-title");
            assertEquals(Color.web("#75837d"), stageTitle.getTextFill());
            Label stageType = (Label) inferredModelStage.lookup(".workflow-context-stage-type");
            assertEquals(Color.web("#75837d"), stageType.getTextFill());
            assertEquals(Color.web("#e8eeeb"), stageType.getBackground().getFills().get(0).getFill());
                Label promptBadge = (Label) inferredModelStage.lookup("#teacher-context-model-prompt-badge");
                assertEquals("PROMPT", promptBadge.getText());
                assertEquals(Color.web("#75837d"), promptBadge.getTextFill());
            assertEquals("Sin prompt seleccionado",
                    ((Label) inferredModelStage.lookup("#teacher-context-model-prompt-file")).getText());
            Button revealPrompt = (Button) inferredModelStage.lookup("#teacher-context-model-prompt-reveal");
            assertFalse(revealPrompt.isVisible());
            assertFalse(revealPrompt.isManaged());
                VBox validationStage = (VBox) section.lookup("#teacher-context-stage-validation-report");
                assertEquals(Color.web("#75837d"), ((Label) validationStage.lookup(
                    "#teacher-context-validation-input-code-badge")).getTextFill());
                assertEquals(Color.web("#75837d"), ((Label) validationStage.lookup(
                    "#teacher-context-validation-input-statement-badge")).getTextFill());
            return null;
        });
    }

    @Test
    void validationRestoresLatestModelsFromWorkflowContext() throws Exception {
        Path statement = Files.writeString(directory.resolve("filtered.txt"), "statement");
        Path javaModel = Files.writeString(directory.resolve("java-model.json"), "{}");
        Path inferredModel = Files.writeString(directory.resolve("inferred-model-validated.json"), "{}");
        Path validationReport = Files.writeString(directory.resolve("validation-report.json"), "{}");
        TeacherWorkflowContext workflow = new TeacherWorkflowContext(directory.resolve("workflow.json"));
        workflow.recordFilteredText(statement);
        workflow.recordCodeModel(javaModel);
        workflow.recordConfirmedStatementModel(inferredModel);
        workflow.recordValidationReport(validationReport);

        onFx(() -> {
            ModelFromStatementSection inference = new ModelFromStatementSection(workflow);
            attach(inference);
            assertEquals(statement.toString(), ((Label) inference.lookup("#model-statement-source")).getText());
            assertEquals(inferredModel, workflow.getSuggestedInferredModel());

            ModelValidationSection section = new ModelValidationSection(workflow);
            attach(section);

            ModelJsonSelection source = (ModelJsonSelection) section.lookup("#model-validation-java");
            ModelJsonSelection inferred = (ModelJsonSelection) section.lookup("#model-validation-statement");
            assertEquals(javaModel, source.getSelectedFile());
            assertEquals(inferredModel, inferred.getSelectedFile());
            assertEquals(validationReport, workflow.getValidationReport());
            assertFalse(button(section, "model-validation-run").isDisabled());
            return null;
        });
    }

    @Test
    void expandsModelJsonPreviewToFillAvailableVerticalSpace() throws Exception {
        Path model = Files.writeString(directory.resolve("model.json"), "{\"root\":{\"value\":\"model\"}}");

        onFx(() -> {
            ModelValidationSection section = new ModelValidationSection(owner -> model.toFile());
            StackPane root = new StackPane(section);
            new Scene(root, 1200, 800);
            root.resize(1200, 800);
            root.applyCss();
            root.layout();
            button(section, "model-validation-java-select").fire();
            root.layout();

            StackPane panelHost = (StackPane) section.getChildren().get(2);
            VBox panel = (VBox) panelHost.getChildren().get(0);
            ModelJsonSelection selection = (ModelJsonSelection) section.lookup("#model-validation-java");
            VBox preview = (VBox) selection.lookup("#model-validation-java-preview");
            var editor = selection.lookup(".json-editor");
            assertTrue(panelHost.getHeight() > 500);
            assertEquals(panelHost.getHeight(), panel.getHeight(), 1);
            assertTrue(preview.getHeight() > 300);
            assertTrue(editor.getBoundsInParent().getHeight() > 300);
            return null;
        });
    }

    private static void attach(Node section) {
        StackPane root = new StackPane(section);
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(TeacherModelSectionsTest.class.getResource("/styles/app.css").toExternalForm());
        root.applyCss();
        root.layout();
    }

    private static Button button(Node root, String id) {
        return (Button) root.lookup("#" + id);
    }

    private static Button navigation(Node root, String text) {
        return root.lookupAll(".sidebar-navigation-button").stream()
                .map(Button.class::cast).filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
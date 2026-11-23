package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.ClassDiagramView;
import com.crud2gui.desktop.teacher.TeacherWorkflowContext;
import com.crud2gui.statementanalyzer.client.ModelClientProvider;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.model.ModelResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.css.PseudoClass;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.Duration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelFromStatementSectionTest {

        private static final String MODEL_JSON = """
                        {
                            "schemaVersion": "1.0",
                            "expectedManagerId": "PlataformaCompraVenta",
                            "source": {"root": null, "generatedAt": null, "filesParsed": 0, "warnings": []},
                            "types": [
                                {"id": "PlataformaCompraVenta", "name": "PlataformaCompraVenta", "packageName": "",
                                 "kind": "CLASS", "visibility": null, "modifiers": [], "enclosingType": null,
                                 "superclass": null, "interfaces": [], "typeParameters": [], "annotations": [],
                                 "javadoc": null, "location": null, "hasMainMethod": false, "fields": [],
                                 "constructors": [], "methods": [], "enumConstants": [], "optional": false},
                                {"id": "Usuario", "name": "Usuario", "packageName": "", "kind": "CLASS",
                                 "visibility": null, "modifiers": [], "enclosingType": null, "superclass": null,
                                 "interfaces": [], "typeParameters": [], "annotations": [], "javadoc": null,
                                 "location": null, "hasMainMethod": false, "optional": true,
                                 "fields": [{"name": "email", "type": {"name": "String", "qualifiedName": null,
                                 "projectType": false, "arguments": [], "arrayDimensions": 0},
                                 "visibility": "PRIVATE", "modifiers": [], "initializedInline": false,
                                 "annotations": [], "javadoc": null, "location": null, "optional": true}],
                                 "constructors": [], "methods": [{"name": "getEmail",
                                 "returnType": {"name": "String", "qualifiedName": null, "projectType": false,
                                 "arguments": [], "arrayDimensions": 0}, "visibility": "PUBLIC", "modifiers": [],
                                 "parameters": [], "thrown": [], "annotations": [], "hasBody": true,
                                 "javadoc": null, "location": null, "optional": true}], "enumConstants": []},
                                {"id": "Producto", "name": "Producto", "packageName": "", "kind": "CLASS",
                                 "visibility": null, "modifiers": [], "enclosingType": null, "superclass": null,
                                 "interfaces": [], "typeParameters": [], "annotations": [], "javadoc": null,
                                 "location": null, "hasMainMethod": false, "fields": [], "constructors": [],
                                 "methods": [], "enumConstants": [], "optional": false}
                            ],
                            "relations": [
                                {"kind": "COMPOSITION", "from": "PlataformaCompraVenta", "to": "Usuario",
                                 "via": "usuarios", "multiplicity": "0..*", "confidence": "HIGH", "evidence": null},
                                {"kind": "COMPOSITION", "from": "Usuario", "to": "Producto",
                                 "via": "productos", "multiplicity": "0..*", "confidence": "HIGH", "evidence": null}
                            ],
                            "analysis": null,
                            "layout": null
                        }
                        """;

    private static final class FakeProvider implements ModelClientProvider {
        private final AtomicReference<String> prompt = new AtomicReference<>();
        private final AtomicReference<String> statement = new AtomicReference<>();
        private final AtomicReference<String> answer = new AtomicReference<>(MODEL_JSON);
        private final String providerId;
        private final List<ModelInfo> models;

        private FakeProvider() {
            this("fake", List.of(new ModelInfo("fake-model", "Modelo simulado")));
        }

        private FakeProvider(String providerId, List<ModelInfo> models) {
            this.providerId = providerId;
            this.models = models;
        }

        @Override
        public String id() {
            return providerId;
        }

        @Override
        public String displayName() {
            return "Cliente simulado";
        }

        @Override
        public ModelClient createClient() {
            return new ModelClient() {
                @Override
                public String provider() {
                    return providerId;
                }

                @Override
                public List<ModelInfo> listModels() {
                    return models;
                }

                @Override
                public ModelResponse generate(String model, String selectedPrompt, String selectedStatement,
                                              GenerationOptions options) {
                    prompt.set(selectedPrompt);
                    statement.set(selectedStatement);
                    return new ModelResponse("fake", model, answer.get(), "STOP", 120, 35,
                            Duration.ofMillis(250));
                }
            };
        }
    }

    @Test
    void replacesPromptSchemaMarkerWithoutDroppingSurroundingInstructions() throws Exception {
        String prompt = "Reglas de inferencia.\n{{PROJECT_MODEL_JSON_SCHEMA}}\nFin de las reglas.";

        String expanded = ModelFromStatementSection.includeProjectModelSchema(prompt);

        assertTrue(expanded.startsWith("Reglas de inferencia.\n{"));
        assertTrue(expanded.endsWith("\nFin de las reglas."));
        assertTrue(expanded.contains("\"title\": \"CRUD2GUI ProjectModel 1.0\""));
        assertFalse(expanded.contains("{{PROJECT_MODEL_JSON_SCHEMA}}"));
    }

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
    void enablesPromptStepAfterSelectingTextWithEmptyWorkflow() throws Exception {
        Path statement = Files.writeString(directory.resolve("enunciado-filtrado.txt"),
                "Texto filtrado del enunciado.\n".repeat(1500));
        Path stateFile = directory.resolve("empty-workflow.json");
        TeacherWorkflowContext workflow = new TeacherWorkflowContext(stateFile);

        onFx(() -> {
            ModelFromStatementSection section = new ModelFromStatementSection(
                    owner -> statement.toFile(), owner -> null, (owner, current) -> null,
                    List.of(), directory.resolve("output"), workflow);
            StackPane root = new StackPane(section);
            new Scene(root, 1200, 900);
            root.applyCss();
            root.layout();

            assertTrue(section.lookup("#statement-step-2").isDisabled());
            ((Button) section.lookup("#model-statement-select-text")).fire();
            assertEquals(statement.toAbsolutePath().normalize(), workflow.getSelectedStatementText());
            assertFalse(section.lookup("#statement-step-2").isDisabled());
            ((Button) section.lookup("#statement-step-2")).fire();
            assertTrue(section.lookup("#model-statement-prompt-card").isVisible());
            return null;
        });
        assertEquals(statement.toAbsolutePath().normalize(),
                new TeacherWorkflowContext(stateFile).getSelectedStatementText());
    }

    @Test
    void enablesNextStepAfterLoadingFilteredStatementText() throws Exception {
        Path textFile = directory.resolve("enunciado-filtrado.txt");
        Files.writeString(textFile, "Texto filtrado del enunciado.");
        Path promptFile = directory.resolve("prompt-modelo.txt");
        Files.writeString(promptFile, "Instrucciones para inferir el modelo.");

        onFx(() -> {
            ModelFromStatementSection section =
                new ModelFromStatementSection(owner -> textFile.toFile(), owner -> promptFile.toFile());
            TextArea text = (TextArea) section.lookup("#model-statement-text");
            Label source = (Label) section.lookup("#model-statement-source");
            Label selectedTextName = (Label) section.lookup("#model-statement-text-card .statement-file-name");
            Label selectedTextBadge = (Label) selectedTextName.getGraphic();
            assertTrue(selectedTextBadge.getStyleClass().contains("statement-neutral-badge"));
            Label status = (Label) section.lookup("#model-statement-status");
            TextArea prompt = (TextArea) section.lookup("#model-statement-prompt-text");
            Label promptSource = (Label) section.lookup("#model-statement-prompt-source");
            Label promptName = (Label) section.lookup("#model-statement-prompt-card .statement-file-name");
            Label promptBadge = (Label) promptName.getGraphic();
            Label promptStatus = (Label) section.lookup("#model-statement-prompt-status");
            ComboBox<?> providers = (ComboBox<?>) section.lookup("#model-statement-provider");
            ComboBox<?> models = (ComboBox<?>) section.lookup("#model-statement-model");
            Button refreshModels = (Button) section.lookup("#model-statement-refresh-models");
            Button openOutputFolder = (Button) section.lookup("#model-statement-open-output-folder");
            StackPane panelHost = (StackPane) section.getChildren().get(2);
            assertEquals(6, panelHost.getChildren().size());
            String[] statusIds = {"model-statement-status", "model-statement-prompt-status",
                    "model-statement-llm-status", "model-statement-generation-status",
                    "model-statement-refine-status", "model-statement-diagram-status"};
            for (int index = 0; index < statusIds.length; index++) {
                VBox content = (VBox) ((VBox) panelHost.getChildren().get(index)).getChildren().get(0);
                assertEquals(section.lookup("#" + statusIds[index]),
                        content.getChildren().get(content.getChildren().size() - 1));
            }
            assertEquals(Region.USE_PREF_SIZE, section.getMinHeight());
            assertEquals(Priority.ALWAYS, VBox.getVgrow(panelHost));
            assertEquals(Priority.ALWAYS, VBox.getVgrow(text));
            assertFalse(text.isEditable());
            assertFalse(text.isVisible());
            assertFalse(text.isManaged());
            StackPane root = new StackPane(section);
            new Scene(root, 1200, 1000);
            root.resize(1200, 1000);
            root.applyCss();
            root.layout();
            javafx.scene.layout.HBox flow = (javafx.scene.layout.HBox) section.getChildren().get(1);
            root.resize(2200, 1000);
            root.layout();
            assertEquals(flow.prefWidth(-1), flow.getWidth(), 1);
            assertTrue(flow.getWidth() < section.getWidth() - section.getInsets().getLeft()
                    - section.getInsets().getRight());
            VBox statementPanel = (VBox) panelHost.getChildren().get(0);
            assertEquals(statementPanel.prefHeight(statementPanel.getWidth()), statementPanel.getHeight(), 1);
            assertEquals(0, statementPanel.getLayoutY(), 1);
            assertTrue(statementPanel.getHeight() < panelHost.getHeight() / 2);
                assertEquals(List.of("gemini", "groq", "ollama"), providers.getItems().stream()
                    .map(ModelClientProvider.class::cast).map(ModelClientProvider::id).toList());
                assertTrue(models.isDisabled());
                assertTrue(providers.getStyleClass().contains("model-llm-selector"));
                assertTrue(models.getStyleClass().contains("model-llm-selector"));
                    assertEquals("Actualizar modelos", refreshModels.getText());
                    assertEquals("Abrir carpeta de salida", openOutputFolder.getText());
                    assertTrue(section.lookup("#model-statement-provider-title").getStyleClass()
                        .contains("model-llm-field-title"));
                    assertTrue(section.lookup("#model-statement-model-title").getStyleClass()
                        .contains("model-llm-field-title"));
            for (int i = 0; i < 6; i++) {
                assertEquals(i != 0, ((Button) section.lookup("#statement-step-" + (i + 1))).isDisabled());
            }
            ((Button) section.lookup("#statement-step-2")).fire();
            assertTrue(panelHost.getChildren().get(0).isVisible());

            ((Button) section.lookup("#model-statement-select-text")).fire();
            assertEquals("Texto filtrado del enunciado.", text.getText());
            assertTrue(text.isVisible());
            assertTrue(text.isManaged());
            root.layout();
            assertEquals(panelHost.getHeight(), statementPanel.getHeight(), 1);
                assertEquals(textFile.getFileName().toString(), selectedTextName.getText());
                assertEquals("TXT", selectedTextBadge.getText());
            assertEquals(textFile.toAbsolutePath().normalize().getParent().toString(), source.getText());
                assertTrue(section.lookup("#model-statement-text-card").getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("selected")));
            assertTrue(status.getText().contains("cargado correctamente"));
            assertTrue(status.getText().contains("paso 4: Obtener modelo para cargar un JSON local."));
            assertFalse(((Button) section.lookup("#statement-step-2")).isDisabled());
            assertTrue(((Button) section.lookup("#statement-step-1")).getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("completed")));

            ((Button) section.lookup("#statement-step-2")).fire();
            assertFalse(panelHost.getChildren().get(0).isVisible());
            assertTrue(panelHost.getChildren().get(1).isVisible());
            assertTrue(((Button) section.lookup("#statement-step-2"))
                    .getPseudoClassStates().contains(PseudoClass.getPseudoClass("selected")));
            for (int i = 2; i < 6; i++) {
                assertEquals(i >= 4, section.lookup("#statement-step-" + (i + 1)).isDisabled());
                assertFalse(section.lookup("#statement-step-" + (i + 1)).getPseudoClassStates()
                        .contains(PseudoClass.getPseudoClass("completed")));
            }

            assertFalse(prompt.isEditable());
            assertEquals(Priority.ALWAYS, VBox.getVgrow(prompt));
            assertFalse(prompt.isVisible());
            assertFalse(prompt.isManaged());
            root.layout();
            VBox promptPanel = (VBox) panelHost.getChildren().get(1);
            assertEquals(promptPanel.prefHeight(promptPanel.getWidth()), promptPanel.getHeight(), 1);
            assertEquals(0, promptPanel.getLayoutY(), 1);
            assertTrue(promptPanel.getHeight() < panelHost.getHeight() / 2);
            ((Button) section.lookup("#model-statement-select-prompt")).fire();
            assertEquals("Instrucciones para inferir el modelo.", prompt.getText());
            assertTrue(prompt.isVisible());
            assertTrue(prompt.isManaged());
            root.layout();
            assertEquals(panelHost.getHeight(), promptPanel.getHeight(), 1);
            double promptHeight = prompt.getHeight();
            root.resize(2200, 1200);
            root.layout();
            assertTrue(prompt.getHeight() > promptHeight + 150);
            root.resize(2200, 800);
            root.layout();
            assertTrue(prompt.getHeight() < promptHeight - 150);
            assertTrue(promptStatus.isVisible());
            assertTrue(promptStatus.isManaged());
                assertEquals(promptFile.getFileName().toString(), promptName.getText());
                assertEquals("TXT", promptBadge.getText());
            assertEquals(promptFile.toAbsolutePath().normalize().getParent().toString(), promptSource.getText());
                assertTrue(section.lookup("#model-statement-prompt-card").getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("selected")));
            assertTrue(promptStatus.getText().contains("cargado correctamente"));
            assertTrue(promptStatus.getText().contains("Puedes continuar con el paso 3: Seleccionar LLM."));
            assertFalse(((Button) section.lookup("#statement-step-3")).isDisabled());
            assertFalse(((Button) section.lookup("#statement-step-4")).isDisabled());
            assertTrue(((Button) section.lookup("#statement-step-2")).getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("completed")));
            return null;
        });
    }

    @Test
    void restoresPromptFromPersistedWorkflowContext() throws Exception {
        Path promptFile = directory.resolve("persisted-prompt.txt");
        String prompt = "Instrucciones persistidas para inferir el modelo.";
        Files.writeString(promptFile, prompt);
        Path stateFile = directory.resolve("workflow.json");
        TeacherWorkflowContext saved = new TeacherWorkflowContext(stateFile);
        saved.selectPromptFile(promptFile);
        TeacherWorkflowContext restored = new TeacherWorkflowContext(stateFile);

        onFx(() -> {
            ModelFromStatementSection section = new ModelFromStatementSection(restored);
            StackPane root = new StackPane(section);
            new Scene(root, 1200, 900);
            root.applyCss();
            root.layout();

            TextArea promptText = (TextArea) section.lookup("#model-statement-prompt-text");
            Label promptPath = (Label) section.lookup("#model-statement-prompt-source");
            Label promptName = (Label) section.lookup("#model-statement-prompt-card .statement-file-name");
            assertEquals(prompt, promptText.getText());
            assertTrue(promptText.isVisible());
            assertEquals(promptFile.toAbsolutePath().normalize().getParent().toString(), promptPath.getText());
            assertEquals(promptFile.getFileName().toString(), promptName.getText());
            return null;
        });
    }

    @Test
    void restoresLlmProviderAndAvailableModelAfterModelListLoads() throws Exception {
        FakeProvider provider = new FakeProvider();
        Path stateFile = directory.resolve("llm-workflow.json");
        TeacherWorkflowContext saved = new TeacherWorkflowContext(stateFile);
        saved.selectStatementText(Files.writeString(directory.resolve("statement.txt"), "Enunciado."));
        saved.selectModelProvider(provider.id());
        saved.selectModel("fake-model");
        TeacherWorkflowContext restored = new TeacherWorkflowContext(stateFile);

        ModelFromStatementSection section = onFx(() -> {
            ModelFromStatementSection created = new ModelFromStatementSection(owner -> null, owner -> null,
                    (owner, current) -> null, List.of(provider), directory.resolve("output"), restored);
            StackPane root = new StackPane(created);
            new Scene(root, 1200, 900);
            root.applyCss();
            root.layout();
            return created;
        });

        Label status = onFx(() -> (Label) section.lookup("#model-statement-llm-status"));
        awaitStatus(status, "Puedes continuar con el paso 4: Obtener modelo.");
        onFx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<ModelClientProvider> providers =
                    (ComboBox<ModelClientProvider>) section.lookup("#model-statement-provider");
            @SuppressWarnings("unchecked")
            ComboBox<ModelInfo> models = (ComboBox<ModelInfo>) section.lookup("#model-statement-model");
            assertEquals(provider, providers.getValue());
            assertEquals("fake-model", models.getValue().id());
            assertFalse(((Button) section.lookup("#statement-step-4")).isDisabled());
            return null;
        });
    }

        @Test
        void restoresConfirmedModelAndReviewStepsWhenReturningToSection() throws Exception {
        Path statement = Files.writeString(directory.resolve("texto-enunciado-filtrado.txt"), "Enunciado.");
        Path prompt = Files.writeString(directory.resolve("prompt.txt"), "Instrucciones.");
        Path generated = Files.writeString(directory.resolve("modelo-generado.json"), MODEL_JSON);
        Path reviewed = Files.writeString(directory.resolve("modelo-generado-revisado.json"), MODEL_JSON);
        TeacherWorkflowContext workflow = new TeacherWorkflowContext(directory.resolve("confirmed-workflow.json"));
        workflow.selectStatementText(statement);
        workflow.selectPromptFile(prompt);
        workflow.selectModelProvider("fake");
        workflow.selectModel("fake-model");
        workflow.recordGeneratedStatementModel(generated);
        workflow.recordConfirmedStatementModel(reviewed);
        FakeProvider provider = new FakeProvider();

        ModelFromStatementSection section = onFx(() -> {
            ModelFromStatementSection created = new ModelFromStatementSection(owner -> null, owner -> null,
                (owner, current) -> null, List.of(provider), directory.resolve("output"), workflow);
            StackPane root = new StackPane(created);
            new Scene(root, 1200, 900);
            root.applyCss();
            root.layout();
            return created;
        });

        Label reviewStatus = onFx(() -> (Label) section.lookup("#model-statement-refine-status"));
        awaitStatus(reviewStatus, "Modelo revisado restaurado");
        onFx(() -> {
            assertEquals(reviewed.getFileName().toString(),
                ((Label) section.lookup("#model-statement-output-filename")).getText());
            assertTrue(((StackPane) section.lookup("#model-statement-generated-json")).isVisible());
            assertTrue(section.generatedModelViewer() != null);
            assertFalse(section.generatedModelViewer().isEditable());
            assertTrue(section.lookup("#statement-step-4").getPseudoClassStates()
                .contains(PseudoClass.getPseudoClass("completed")));
            assertTrue(section.lookup("#statement-step-5").getPseudoClassStates()
                .contains(PseudoClass.getPseudoClass("completed")));
            assertFalse(section.lookup("#statement-step-6").isDisabled());
            assertTrue(section.lookup("#statement-step-6").getPseudoClassStates()
                .contains(PseudoClass.getPseudoClass("completed")));
            ((Button) section.lookup("#model-statement-confirm-model")).fire();
            assertTrue(Files.isRegularFile(reviewed));
            assertEquals(MODEL_JSON, Files.readString(reviewed));
            assertTrue(Files.isRegularFile(directory.resolve("modelo-generado-revisado_1.json")));
            return null;
        });
        }

    @Test
    void selectsConfiguredProviderDefaultWhenAvailable() throws Exception {
        String property = "STATEMENT_ANALYZER_GEMINI_MODEL";
        String previousValue = System.getProperty(property);
        System.setProperty(property, "gemini-default-test");
        try {
            FakeProvider provider = new FakeProvider("gemini",
                    List.of(new ModelInfo("gemini-default-test", "Gemini predeterminado")));
            ModelFromStatementSection section = onFx(() -> {
                ModelFromStatementSection created = new ModelFromStatementSection(owner -> null, owner -> null,
                        List.of(provider), directory.resolve("output"));
                StackPane root = new StackPane(created);
                new Scene(root, 1200, 900);
                root.applyCss();
                root.layout();
                return created;
            });
            onFx(() -> {
                @SuppressWarnings("unchecked")
                ComboBox<ModelClientProvider> providers = (ComboBox<ModelClientProvider>)
                        section.lookup("#model-statement-provider");
                providers.setValue(provider);
                providers.getOnAction().handle(new ActionEvent());
                return null;
            });

            Label status = onFx(() -> (Label) section.lookup("#model-statement-llm-status"));
            awaitStatus(status, "Modelo de lenguaje seleccionado.");
            onFx(() -> {
                @SuppressWarnings("unchecked")
                ComboBox<ModelInfo> models = (ComboBox<ModelInfo>) section.lookup("#model-statement-model");
                assertEquals("gemini-default-test", models.getValue().id());
                return null;
            });
        } finally {
            if (previousValue == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, previousValue);
            }
        }
    }

    @Test
    void keepsStatementViewerHiddenWhenSelectionIsCancelledOrReadingFails() throws Exception {
        AtomicReference<File> selected = new AtomicReference<>();
        onFx(() -> {
            ModelFromStatementSection section = new ModelFromStatementSection(owner -> selected.get());
            TextArea text = (TextArea) section.lookup("#model-statement-text");
            Button select = (Button) section.lookup("#model-statement-select-text");
            select.fire();
            assertFalse(text.isVisible());
            assertFalse(text.isManaged());
            selected.set(directory.resolve("inexistente.txt").toFile());
            select.fire();
            assertFalse(text.isVisible());
            assertFalse(text.isManaged());
            assertTrue(((Label) section.lookup("#model-statement-status")).getText()
                    .contains("No se pudo leer"));
            return null;
        });
    }

    @Test
    void keepsPromptViewerHiddenWhenSelectionIsCancelledOrReadingFails() throws Exception {
        AtomicReference<File> selected = new AtomicReference<>();
        Path promptFile = directory.resolve("prompt.txt");
        Files.writeString(promptFile, "Instrucciones del modelo.");
        onFx(() -> {
            ModelFromStatementSection section =
                    new ModelFromStatementSection(owner -> null, owner -> selected.get());
            TextArea prompt = (TextArea) section.lookup("#model-statement-prompt-text");
            Button select = (Button) section.lookup("#model-statement-select-prompt");
            Label status = (Label) section.lookup("#model-statement-prompt-status");
            select.fire();
            assertFalse(prompt.isVisible());
            assertFalse(prompt.isManaged());
            selected.set(directory.resolve("inexistente.txt").toFile());
            select.fire();
            assertFalse(prompt.isVisible());
            assertFalse(prompt.isManaged());
            assertTrue(status.getText().contains("No se pudo leer"));
            selected.set(promptFile.toFile());
            select.fire();
            assertTrue(prompt.isVisible());
            assertTrue(prompt.isManaged());
            assertTrue(status.getText().contains("Puedes continuar con el paso 3: Seleccionar LLM."));
            selected.set(null);
            select.fire();
            assertEquals("Instrucciones del modelo.", prompt.getText());
            selected.set(directory.resolve("inexistente.txt").toFile());
            select.fire();
            assertEquals("Instrucciones del modelo.", prompt.getText());
            assertTrue(prompt.isVisible());
            assertTrue(prompt.isManaged());
            assertTrue(status.getText().contains("No se pudo leer"));
            return null;
        });
    }

    @Test
    void generatesAndPersistsJsonBeforeEnablingDiagramStep() throws Exception {
        Path textFile = directory.resolve("texto-enunciado_practica_23-filtrado.txt");
        Path promptFile = directory.resolve("prompt-modelo.txt");
        Path initialOutputDirectory = directory.resolve("output-inicial");
        Path selectedOutputDirectory = directory.resolve("output-seleccionado");
        Path reviewedOutputDirectory = directory.resolve("output-confirmado");
        Files.createDirectories(reviewedOutputDirectory);
        AtomicReference<Path> selectedOutputFolder = new AtomicReference<>(selectedOutputDirectory);
        Files.writeString(textFile, "Texto del enunciado para el modelo.");
        Files.writeString(promptFile, "Instrucciones para inferir clases.");
        FakeProvider provider = new FakeProvider("fake",
            List.of(new ModelInfo("gemini-3.5-flash-lite", "Gemini 3.5 Flash Lite")));

        ModelFromStatementSection section = onFx(() -> new ModelFromStatementSection(
            owner -> textFile.toFile(), owner -> promptFile.toFile(),
            (owner, current) -> selectedOutputFolder.get().toFile(), List.of(provider), initialOutputDirectory));
        onFx(() -> {
            ((Button) section.lookup("#model-statement-select-text")).fire();
            ((Button) section.lookup("#model-statement-select-prompt")).fire();
            ComboBox<?> providers = (ComboBox<?>) section.lookup("#model-statement-provider");
            providers.getSelectionModel().select(0);
            providers.getOnAction().handle(new ActionEvent());
            return null;
        });

        Label modelStatus = onFx(() -> (Label) section.lookup("#model-statement-llm-status"));
        awaitStatus(modelStatus, "1 modelos");
        onFx(() -> {
            ((ComboBox<?>) section.lookup("#model-statement-model")).getSelectionModel().select(0);
            assertTrue(((Label) section.lookup("#model-statement-llm-status")).getText()
                    .contains("Puedes continuar con el paso 4: Obtener modelo."));
            ((Button) section.lookup("#statement-step-4")).fire();
            assertEquals("Cliente simulado",
                    ((Label) section.lookup("#model-statement-request-provider")).getText());
                assertEquals("Gemini 3.5 Flash Lite",
                    ((Label) section.lookup("#model-statement-request-model")).getText());
                ((Button) section.lookup("#model-statement-change-output-folder")).fire();
                assertEquals(selectedOutputDirectory.toAbsolutePath().normalize().toString(),
                    ((Label) section.lookup("#model-statement-output-folder")).getText());
                assertEquals("modelo-enunciado_practica_23-filtrado-gemini-3.5-flash-lite.json",
                    ((Label) section.lookup("#model-statement-output-filename")).getText());
            ((Button) section.lookup("#model-statement-generate-model")).fire();
            return null;
        });

        Label generationStatus = onFx(() -> (Label) section.lookup("#model-statement-generation-status"));
        awaitStatus(generationStatus, "Análisis completado");
        onFx(() -> {
            StackPane result = (StackPane) section.lookup("#model-statement-generated-json");
            Label outputName = (Label) section.lookup("#model-statement-output-filename");
            Label outputDirectory = (Label) section.lookup("#model-statement-output-folder");
            assertTrue(result.isVisible());
            assertTrue(section.generatedModelViewer() != null);
            assertFalse(section.generatedModelViewer().isEditable());
            assertEquals("model-statement-generated-model-viewer",
                    section.generatedModelViewer().getView().getId());
            assertEquals(section.generatedModelViewer().getView(), result.getChildren().get(0));
            assertTrue(generationStatus.getText().contains("Puedes continuar con el paso 5: Revisar modelo."));
            assertEquals("", ((Label) section.lookup("#model-statement-diagram-status")).getText());
            assertEquals("modelo-enunciado_practica_23-filtrado-gemini-3.5-flash-lite.json", outputName.getText());
            Path outputPath = Path.of(outputDirectory.getText()).resolve(outputName.getText());
            assertEquals(selectedOutputDirectory.toAbsolutePath().normalize(), outputPath.getParent());
            assertEquals(outputPath.getFileName().toString(), outputName.getText());
            assertTrue(Files.isRegularFile(outputPath));
            Label fullPath = (Label) section.lookup("#model-statement-generation-output-path");
            assertFalse(fullPath.isVisible());
            assertFalse(fullPath.isManaged());
            assertFalse(((Button) section.lookup("#statement-step-5")).isDisabled());
            assertTrue(((Button) section.lookup("#statement-step-6")).isDisabled());
            assertTrue(provider.prompt.get().startsWith("Instrucciones para inferir clases."));
            assertTrue(provider.prompt.get().contains("\"title\": \"CRUD2GUI ProjectModel 1.0\""));
            assertFalse(provider.prompt.get().contains("{{PROJECT_MODEL_JSON_SCHEMA}}"));
            assertEquals("Texto del enunciado para el modelo.", provider.statement.get());
                ((Button) section.lookup("#statement-step-5")).fire();
            selectedOutputFolder.set(reviewedOutputDirectory);
            ((Button) section.lookup("#model-statement-change-review-output-folder")).fire();
            assertEquals(reviewedOutputDirectory.toAbsolutePath().normalize().toString(),
                ((Label) section.lookup("#model-statement-review-output-folder")).getText());
            assertEquals("modelo-enunciado_practica_23-filtrado-gemini-3.5-flash-lite-revisado.json",
                ((Label) section.lookup("#model-statement-review-output-filename")).getText());
            assertTrue(section.lookup("#model-statement-open-review-output-folder") instanceof Button);
                assertEquals(1, ((StackPane) section.lookup("#model-statement-editor-host"))
                        .getChildren().size());
                ((Button) section.lookup("#model-statement-confirm-model")).fire();
            Path validated = reviewedOutputDirectory.resolve(outputPath.getFileName().toString()
                .replace(".json", "-revisado.json"));
                assertTrue(Files.isRegularFile(validated));
                assertTrue(Files.readString(validated).contains("\"expectedManagerId\" : \"PlataformaCompraVenta\""));
                assertTrue(((Label) section.lookup("#model-statement-refine-status")).getText()
                        .contains("Puedes continuar con el paso 6: Diagrama de clases."));
                assertEquals("Diagrama de clases generado correctamente. Flujo completado.",
                        ((Label) section.lookup("#model-statement-diagram-status")).getText());
                assertFalse(((Button) section.lookup("#statement-step-6")).isDisabled());
                ((Button) section.lookup("#statement-step-6")).fire();
                StackPane panelHost = (StackPane) section.getChildren().get(2);
                VBox diagramPanel = (VBox) panelHost.getChildren().get(5);
                VBox diagramContent = (VBox) diagramPanel.getChildren().get(0);
                ScrollPane diagramScroll = (ScrollPane) diagramContent.getChildren().get(1);
                ClassDiagramView diagram = (ClassDiagramView) diagramScroll.getContent();
                assertEquals(5, diagramContent.lookupAll(".uml-relation-legend-item").size());
                assertEquals(3, diagram.lookupAll(".uml-class-card").size());
                assertEquals(1, diagram.lookupAll(".uml-manager-card").size());
                assertTrue(diagram.lookupAll(".uml-package-name").isEmpty());
                assertEquals("PlataformaCompraVenta", diagram.getModel().expectedManagerId());
                assertEquals(2, diagram.getModel().relations().size());
                assertTrue(diagram.lookupAll(".uml-stereotype").stream()
                        .map(node -> ((Label) node).getText())
                        .anyMatch(text -> text.equals("<<opcional>> <<class>>")));
                assertTrue(diagram.lookupAll(".uml-member-line").stream()
                        .map(node -> ((Label) node).getText())
                        .anyMatch(text -> text.contains("email : String «opcional»")));
                assertTrue(diagram.lookupAll(".uml-member-line").stream()
                        .map(node -> ((Label) node).getText())
                        .anyMatch(text -> text.contains("getEmail() : String «opcional»")));
                    diagram.applyCss();
                    diagram.resize(diagram.getPrefWidth(), diagram.getPrefHeight());
                    diagram.layout();
                        List<Node> classCards = diagram.lookupAll(".uml-class-card").stream().toList();
                    var managerCard = diagram.getChildren().stream()
                        .filter(node -> node.getStyleClass().contains("uml-manager-card"))
                        .findFirst().orElseThrow();
                            assertTrue(managerCard.getLayoutX() + managerCard.getBoundsInParent().getWidth() / 2
                                < diagram.getWidth() / 2);
                            assertTrue(classCards.get(0).getLayoutX() < classCards.get(1).getLayoutX());
                            assertTrue(classCards.get(1).getLayoutX() < classCards.get(2).getLayoutX());
                    assertTrue(diagram.lookupAll(".uml-member-line").size() >= 2);
                        for (int first = 0; first < classCards.size(); first++) {
                        assertTrue(classCards.get(first).getOnMouseDragged() != null);
                        for (int second = first + 1; second < classCards.size(); second++) {
                            assertFalse(classCards.get(first).getBoundsInParent()
                                .intersects(classCards.get(second).getBoundsInParent()));
                        }
                        }
                ScrollPane pageScroll = new ScrollPane(section);
                pageScroll.setFitToWidth(true);
                pageScroll.setFitToHeight(true);
                StackPane root = new StackPane(pageScroll);
                new Scene(root, 1200, 650);
                root.resize(1200, 650);
                root.applyCss();
                root.layout();
                assertEquals(ScrollPane.ScrollBarPolicy.NEVER, diagramScroll.getVbarPolicy());
                assertTrue(diagramScroll.getViewportBounds().getHeight() >= diagram.getPrefHeight() - 1);
                assertTrue(section.getHeight() > pageScroll.getViewportBounds().getHeight());
                assertTrue(classCards.stream().allMatch(card -> card.getBoundsInParent().getMaxY()
                        <= diagramScroll.getViewportBounds().getHeight()));
                double initialScroll = pageScroll.getVvalue();
                diagram.fireEvent(new javafx.scene.input.ScrollEvent(
                        javafx.scene.input.ScrollEvent.SCROLL, 20, 20, 20, 20,
                        false, false, false, false, false, false,
                        0, -120, 0, -120,
                        javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                        javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
                assertTrue(pageScroll.getVvalue() > initialScroll, "La rueda debe desplazar la pagina.");
                pageScroll.setVvalue(1);
                root.layout();
                assertEquals(1, pageScroll.getVvalue());
            return null;
        });
    }

    @Test
    void showsInvalidResponsesWithoutEnablingDiagramAndAllowsRetry() throws Exception {
        Path textFile = directory.resolve("enunciado.txt");
        Path promptFile = directory.resolve("prompt.txt");
        Path outputDirectory = directory.resolve("output");
        Files.writeString(textFile, "Texto del enunciado.");
        Files.writeString(promptFile, "Instrucciones del modelo.");
        FakeProvider provider = new FakeProvider();
        ModelFromStatementSection section = onFx(() -> new ModelFromStatementSection(
                owner -> textFile.toFile(), owner -> promptFile.toFile(),
                (owner, current) -> null, List.of(provider), outputDirectory));
        onFx(() -> {
            ((Button) section.lookup("#model-statement-select-text")).fire();
            ((Button) section.lookup("#model-statement-select-prompt")).fire();
            @SuppressWarnings("unchecked")
            ComboBox<ModelClientProvider> providers = (ComboBox<ModelClientProvider>)
                    section.lookup("#model-statement-provider");
            providers.setValue(provider);
            providers.getOnAction().handle(new ActionEvent());
            return null;
        });
        Label llmStatus = onFx(() -> (Label) section.lookup("#model-statement-llm-status"));
        awaitStatus(llmStatus, "1 modelos");
        onFx(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<ModelInfo> models = (ComboBox<ModelInfo>) section.lookup("#model-statement-model");
            models.getSelectionModel().selectFirst();
            ((Button) section.lookup("#statement-step-4")).fire();
            ((Button) section.lookup("#model-statement-generate-model")).fire();
            return null;
        });
        Label status = onFx(() -> (Label) section.lookup("#model-statement-generation-status"));
        awaitStatus(status, "Análisis completado");
        String firstOutputName = onFx(() ->
            ((Label) section.lookup("#model-statement-output-filename")).getText());
        for (String invalid : List.of("{\"types\":[{\"id\":\"A\"]}", "[1,2]", "{\"schemaVersion\":\"99.0\"}",
            "{\"schemaVersion\":\"1.0\",\"types\":[],\"relations\":[]}")) {
            provider.answer.set(invalid);
            onFx(() -> {
                ((Button) section.lookup("#model-statement-generate-model")).fire();
                assertTrue(section.lookup("#statement-step-5").isDisabled());
                return null;
            });
            awaitStatus(status, "No se pudo obtener un JSON válido:");
            onFx(() -> {
                StackPane result = (StackPane) section.lookup("#model-statement-generated-json");
                TextArea viewer = (TextArea) result.getChildren().get(0);
                assertEquals(invalid, viewer.getText());
                assertTrue(viewer.isVisible());
                assertTrue(viewer.isManaged());
                assertFalse(viewer.isEditable());
                assertTrue(section.generatedModelViewer() == null);
                assertTrue(status.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
                assertTrue(section.lookup("#statement-step-5").isDisabled());
                assertFalse(section.lookup("#statement-step-4").getPseudoClassStates()
                        .contains(PseudoClass.getPseudoClass("completed")));
                assertEquals("", ((Label) section.lookup(".model-generation-output-path")).getText());
                ScrollPane diagramScroll = (ScrollPane) section.lookup("#model-statement-class-diagram-scroll");
                assertEquals(null, ((ClassDiagramView) diagramScroll.getContent()).getModel());
                return null;
            });
        }
        try (var files = Files.walk(outputDirectory)) {
            assertEquals(1, files.filter(Files::isRegularFile).count());
        }
        provider.answer.set(MODEL_JSON);
        onFx(() -> {
            ((Button) section.lookup("#model-statement-generate-model")).fire();
            return null;
        });
        awaitStatus(status, "Análisis completado");
        onFx(() -> {
            assertFalse(section.lookup("#statement-step-5").isDisabled());
            assertFalse(status.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            assertTrue(section.generatedModelViewer() != null);
            assertFalse(section.generatedModelViewer().isEditable());
            assertEquals(section.generatedModelViewer().getView(),
                    ((StackPane) section.lookup("#model-statement-generated-json")).getChildren().get(0));
                String expectedName = firstOutputName.replaceFirst("\\.json$", "_1.json");
                assertEquals(expectedName, ((Label) section.lookup("#model-statement-output-filename")).getText());
                Path folder = Path.of(((Label) section.lookup("#model-statement-output-folder")).getText());
                assertTrue(Files.isRegularFile(folder.resolve(expectedName)));
            return null;
        });
    }


    @Test
    void enablesLlmGenerationOnlyWhileProviderAndModelAreSelected() throws Exception {
        Path textFile = Files.writeString(directory.resolve("enunciado.txt"), "Texto del enunciado.");
        Path promptFile = Files.writeString(directory.resolve("prompt.txt"), "Instrucciones del modelo.");
        FakeProvider provider = new FakeProvider();
        PseudoClass missing = PseudoClass.getPseudoClass("missing");
        ModelFromStatementSection section = onFx(() -> {
            ModelFromStatementSection created = new ModelFromStatementSection(
                    owner -> textFile.toFile(), owner -> promptFile.toFile(),
                    List.of(provider), directory.resolve("output"));
            Scene scene = new Scene(created, 1200, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            created.applyCss();
            created.layout();
            assertTrue(created.lookup("#model-statement-generate-model").isDisabled());
            for (String field : List.of("provider", "model", "prompt", "statement")) {
                Label badge = (Label) created.lookup("#model-statement-request-" + field);
                assertEquals("Sin seleccionar", badge.getText());
                assertTrue(badge.getPseudoClassStates().contains(missing));
                assertEquals(Color.web("#9a3412"), badge.getTextFill());
                assertTrue(badge.getPadding().getTop() > 0);
            }
            ((Button) created.lookup("#model-statement-select-text")).fire();
            ((Button) created.lookup("#model-statement-select-prompt")).fire();
            ((Button) created.lookup("#statement-step-4")).fire();
            assertTrue(created.lookup("#model-statement-generate-model").isDisabled());
            assertFalse(created.lookup("#model-statement-use-local-model").isDisabled());
            for (String field : List.of("prompt", "statement")) {
                Label badge = (Label) created.lookup("#model-statement-request-" + field);
                badge.applyCss();
                assertFalse(badge.getPseudoClassStates().contains(missing));
                assertEquals(Color.web("#135c48"), badge.getTextFill());
                VBox lines = (VBox) badge.getGraphic();
                Label filename = (Label) lines.getChildren().get(0);
                Label folder = (Label) lines.getChildren().get(1);
                Path selectedFile = field.equals("prompt") ? promptFile : textFile;
                assertEquals(selectedFile.getFileName().toString(), filename.getText());
                assertEquals(selectedFile.getParent().toString(), folder.getText());
                assertEquals(Color.web("#66736e"), folder.getTextFill());
                assertEquals(Font.font(folder.getFont().getFamily(), FontWeight.NORMAL,
                        folder.getFont().getSize()), folder.getFont());
                assertEquals(Font.font(filename.getFont().getFamily(), FontWeight.BOLD,
                        filename.getFont().getSize()), filename.getFont());
            }
            ComboBox<?> providers = (ComboBox<?>) created.lookup("#model-statement-provider");
            providers.getSelectionModel().select(0);
            providers.getOnAction().handle(new ActionEvent());
            return created;
        });
        Label status = onFx(() -> (Label) section.lookup("#model-statement-llm-status"));
        awaitStatus(status, "1 modelos");
        onFx(() -> {
            Button generate = (Button) section.lookup("#model-statement-generate-model");
            ComboBox<?> models = (ComboBox<?>) section.lookup("#model-statement-model");
            ComboBox<?> providers = (ComboBox<?>) section.lookup("#model-statement-provider");
            Label modelBadge = (Label) section.lookup("#model-statement-request-model");
            assertTrue(generate.isDisabled());
            models.getSelectionModel().select(0);
            assertFalse(generate.isDisabled());
            assertFalse(modelBadge.getPseudoClassStates().contains(missing));
            models.getSelectionModel().clearSelection();
            assertTrue(generate.isDisabled());
            assertEquals("Sin seleccionar", modelBadge.getText());
            assertTrue(modelBadge.getPseudoClassStates().contains(missing));
            models.getSelectionModel().select(0);
            assertFalse(generate.isDisabled());
            providers.setValue(null);
            providers.getOnAction().handle(new ActionEvent());
            assertTrue(generate.isDisabled());
            assertEquals("Sin seleccionar", modelBadge.getText());
            assertTrue(modelBadge.getPseudoClassStates().contains(missing));
            assertFalse(section.lookup("#statement-step-4").isDisabled());
            assertFalse(section.lookup("#model-statement-use-local-model").isDisabled());
            return null;
        });
    }

    @Test
    void preservesLocalModelAcrossLlmChangesAndWorkflowRestoration() throws Exception {
        Path statement = Files.writeString(directory.resolve("statement.txt"), "Enunciado.");
        Path anotherStatement = Files.writeString(directory.resolve("another-statement.txt"), "Otro enunciado.");
        Path prompt = Files.writeString(directory.resolve("prompt.txt"), "Instrucciones.");
        Path localModel = Files.writeString(directory.resolve("local.json"), MODEL_JSON);
        Path stateFile = directory.resolve("local-workflow.json");
        TeacherWorkflowContext workflow = new TeacherWorkflowContext(stateFile);
        FakeProvider provider = new FakeProvider();
        AtomicReference<Node> originalEditor = new AtomicReference<>();
        ModelFromStatementSection section = onFx(() -> {
            ModelFromStatementSection created = new ModelFromStatementSection(
                    owner -> statement.toFile(), owner -> prompt.toFile(), (owner, current) -> null,
                    List.of(provider), directory.resolve("output"), workflow, owner -> localModel.toFile());
            new Scene(created, 1200, 900);
            ((Button) created.lookup("#model-statement-select-text")).fire();
            ((Button) created.lookup("#statement-step-4")).fire();
            ((Button) created.lookup("#model-statement-use-local-model")).fire();
            assertFalse(created.lookup("#statement-step-5").isDisabled());
            originalEditor.set(((StackPane) created.lookup("#model-statement-editor-host")).getChildren().get(0));
            ComboBox<?> providers = (ComboBox<?>) created.lookup("#model-statement-provider");
            providers.getSelectionModel().select(0);
            providers.getOnAction().handle(new ActionEvent());
            return created;
        });
        awaitStatus(onFx(() -> (Label) section.lookup("#model-statement-llm-status")), "1 modelos");
        onFx(() -> {
            ((ComboBox<?>) section.lookup("#model-statement-model")).getSelectionModel().select(0);
            assertTrue(section.lookup("#model-statement-generate-model").isDisabled());
            ((Button) section.lookup("#model-statement-select-prompt")).fire();
            assertFalse(section.lookup("#model-statement-generate-model").isDisabled());
            assertEquals(originalEditor.get(),
                    ((StackPane) section.lookup("#model-statement-editor-host")).getChildren().get(0));
            ((Button) section.lookup("#model-statement-confirm-model")).fire();
            assertFalse(section.lookup("#statement-step-6").isDisabled());
            assertEquals(localModel, workflow.getGeneratedStatementModel());
            return null;
        });
        TeacherWorkflowContext restored = new TeacherWorkflowContext(stateFile);
        ModelFromStatementSection restoredSection = onFx(() -> {
            ModelFromStatementSection created = new ModelFromStatementSection(
                    owner -> anotherStatement.toFile(), owner -> prompt.toFile(), (owner, current) -> null,
                    List.of(provider), directory.resolve("output"), restored);
            new Scene(created, 1200, 900);
            return created;
        });
        awaitStatus(onFx(() -> (Label) restoredSection.lookup("#model-statement-refine-status")),
                "Modelo revisado restaurado");
        onFx(() -> {
            assertFalse(restoredSection.lookup("#statement-step-6").isDisabled());
            ComboBox<?> providers = (ComboBox<?>) restoredSection.lookup("#model-statement-provider");
            providers.setValue(null);
            providers.getOnAction().handle(new ActionEvent());
            ((Button) restoredSection.lookup("#model-statement-select-prompt")).fire();
            assertEquals(localModel, restored.getGeneratedStatementModel());
            assertFalse(restoredSection.lookup("#statement-step-6").isDisabled());
            ((Button) restoredSection.lookup("#model-statement-select-text")).fire();
            assertEquals(null, restored.getGeneratedStatementModel());
            assertEquals(null, restored.getSuggestedInferredModel());
            assertTrue(restoredSection.lookup("#statement-step-5").isDisabled());
            assertTrue(restoredSection.lookup("#statement-step-6").isDisabled());
            return null;
        });
    }

    @Test
    void loadsLocalModelWithoutLlmAndContinuesThroughConfirmation() throws Exception {
    Path textFile = Files.writeString(directory.resolve("enunciado-local.txt"), "Texto del enunciado.");
    Path promptFile = Files.writeString(directory.resolve("prompt-local.txt"), "Instrucciones del modelo.");
    Path invalidModel = Files.writeString(directory.resolve("invalido.json"), "no es JSON");
    Path localModel = Files.writeString(directory.resolve("modelo-local.json"), MODEL_JSON);
    AtomicReference<File> selectedLocalModel = new AtomicReference<>(invalidModel.toFile());

    onFx(() -> {
        ModelFromStatementSection section = new ModelFromStatementSection(
            owner -> textFile.toFile(), owner -> promptFile.toFile(),
            (owner, current) -> null, List.of(), directory.resolve("output"), null,
            owner -> selectedLocalModel.get());
        StackPane root = new StackPane(section);
        new Scene(root, 1200, 900);
        root.applyCss();
        root.layout();
        ((Button) section.lookup("#model-statement-select-text")).fire();

        Button generationStep = (Button) section.lookup("#statement-step-4");
        Button refineStep = (Button) section.lookup("#statement-step-5");
        Button generateModelButton = (Button) section.lookup("#model-statement-generate-model");
        Button localModelButton = (Button) section.lookup("#model-statement-use-local-model");
        Label generationStatus = (Label) section.lookup("#model-statement-generation-status");
        assertTrue(generateModelButton.getParent() == localModelButton.getParent());
        assertEquals("Obtener modelo mediante LLM",
            generateModelButton.getText());
        assertEquals("Trabajar con modelo local", localModelButton.getText());
        assertFalse(generationStep.isDisabled());
        generationStep.fire();
        assertTrue(generateModelButton.isDisabled());
        assertFalse(localModelButton.isDisabled());

        localModelButton.fire();
        assertTrue(generationStatus.getText().contains("No se pudo cargar el modelo local"));
        assertTrue(refineStep.isDisabled());

        selectedLocalModel.set(localModel.toFile());
        localModelButton.fire();
        assertTrue(generationStatus.getText().contains("Modelo local cargado correctamente"));
        assertEquals(localModel.getFileName().toString(),
            ((Label) section.lookup("#model-statement-output-filename")).getText());
        assertEquals(localModel.toAbsolutePath().normalize().getParent().toString(),
            ((Label) section.lookup("#model-statement-output-folder")).getText());
        assertFalse(refineStep.isDisabled());
        assertTrue(section.generatedModelViewer() != null);
        assertFalse(section.generatedModelViewer().isEditable());

        refineStep.fire();
        ((Button) section.lookup("#model-statement-confirm-model")).fire();
        Path validatedModel = directory.resolve("modelo-local-revisado.json");
        assertTrue(Files.isRegularFile(validatedModel));
        assertFalse(((Button) section.lookup("#statement-step-6")).isDisabled());
        return null;
    });
    }
    private static void awaitStatus(Label status, String expectedText) throws Exception {
        CompletableFuture<Void> completed = new CompletableFuture<>();
        onFx(() -> {
            if (status.getText().contains(expectedText)) {
                completed.complete(null);
            } else {
                status.textProperty().addListener((observable, previous, current) -> {
                    if (current.contains(expectedText)) {
                        completed.complete(null);
                    }
                });
            }
            return null;
        });
        try {
            completed.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException timeout) {
            String actualStatus = onFx(status::getText);
            throw new AssertionError("Esperaba el estado '" + expectedText + "', pero se mostró: '"
                    + actualStatus + "'.", timeout);
        }
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}

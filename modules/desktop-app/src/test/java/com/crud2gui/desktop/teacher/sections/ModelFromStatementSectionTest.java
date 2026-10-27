package com.crud2gui.desktop.teacher.sections;

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
                                 "kind": "CLASS", "fields": [], "constructors": [], "methods": []},
                                {"id": "Usuario", "name": "Usuario", "packageName": "", "kind": "CLASS",
                                 "optional": true,
                                 "fields": [{"name": "email", "type": {"name": "String", "projectType": false,
                                 "arguments": [], "arrayDimensions": 0}, "visibility": "PRIVATE", "modifiers": [],
                                 "initializedInline": false, "optional": true}], "constructors": [],
                                 "methods": [{"name": "getEmail", "returnType": {"name": "String", "projectType": false,
                                 "arguments": [], "arrayDimensions": 0}, "visibility": "PUBLIC", "modifiers": [],
                                 "parameters": [], "hasBody": true, "optional": true}]},
                                {"id": "Producto", "name": "Producto", "packageName": "", "kind": "CLASS",
                                 "fields": [], "constructors": [], "methods": []}
                            ],
                            "relations": [
                                {"kind": "COMPOSITION", "from": "PlataformaCompraVenta", "to": "Usuario",
                                 "via": "usuarios", "multiplicity": "0..*", "confidence": "HIGH"},
                                {"kind": "COMPOSITION", "from": "Usuario", "to": "Producto",
                                 "via": "productos", "multiplicity": "0..*", "confidence": "HIGH"}
                            ],
                            "analysis": null,
                            "layout": null
                        }
                        """;

    private static final class FakeProvider implements ModelClientProvider {
        private final AtomicReference<String> prompt = new AtomicReference<>();
        private final AtomicReference<String> statement = new AtomicReference<>();

        @Override
        public String id() {
            return "fake";
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
                    return "fake";
                }

                @Override
                public List<ModelInfo> listModels() {
                    return List.of(new ModelInfo("fake-model", "Modelo simulado"));
                }

                @Override
                public ModelResponse generate(String model, String selectedPrompt, String selectedStatement,
                                              GenerationOptions options) {
                    prompt.set(selectedPrompt);
                    statement.set(selectedStatement);
                    return new ModelResponse("fake", model, MODEL_JSON, "STOP", 120, 35,
                            Duration.ofMillis(250));
                }
            };
        }
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
            assertEquals(5, panelHost.getChildren().size());
            String[] statusIds = {"model-statement-status", "model-statement-prompt-status",
                    "model-statement-llm-status", "model-statement-generation-status",
                    "model-statement-diagram-status"};
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
            for (int i = 0; i < 5; i++) {
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
            assertEquals(textFile.toAbsolutePath().toString(), source.getText());
                assertTrue(section.lookup("#model-statement-text-card").getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("selected")));
            assertTrue(status.getText().contains("cargado correctamente"));
            assertTrue(status.getText().contains("Puedes continuar con el paso 2: Seleccionar prompt."));
            assertFalse(((Button) section.lookup("#statement-step-2")).isDisabled());
            assertTrue(((Button) section.lookup("#statement-step-1")).getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("completed")));

            ((Button) section.lookup("#statement-step-2")).fire();
            assertFalse(panelHost.getChildren().get(0).isVisible());
            assertTrue(panelHost.getChildren().get(1).isVisible());
            assertTrue(((Button) section.lookup("#statement-step-2"))
                    .getPseudoClassStates().contains(PseudoClass.getPseudoClass("selected")));
            for (int i = 2; i < 5; i++) {
                assertTrue(((Button) section.lookup("#statement-step-" + (i + 1))).isDisabled());
            }

            assertFalse(prompt.isEditable());
            assertEquals(Priority.ALWAYS, VBox.getVgrow(prompt));
            ((Button) section.lookup("#model-statement-select-prompt")).fire();
            assertEquals("Instrucciones para inferir el modelo.", prompt.getText());
                assertEquals(promptFile.getFileName().toString(), promptName.getText());
                assertEquals("TXT", promptBadge.getText());
            assertEquals(promptFile.toAbsolutePath().toString(), promptSource.getText());
                assertTrue(section.lookup("#model-statement-prompt-card").getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("selected")));
            assertTrue(promptStatus.getText().contains("cargado correctamente"));
            assertTrue(promptStatus.getText().contains("Puedes continuar con el paso 3: Seleccionar LLM."));
            assertFalse(((Button) section.lookup("#statement-step-3")).isDisabled());
            assertTrue(((Button) section.lookup("#statement-step-4")).isDisabled());
            assertTrue(((Button) section.lookup("#statement-step-2")).getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("completed")));
            return null;
        });
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
    void generatesAndPersistsJsonBeforeEnablingDiagramStep() throws Exception {
        Path textFile = directory.resolve("enunciado-filtrado.txt");
        Path promptFile = directory.resolve("prompt-modelo.txt");
        Path initialOutputDirectory = directory.resolve("output-inicial");
        Path selectedOutputDirectory = directory.resolve("output-seleccionado");
        Files.writeString(textFile, "Texto del enunciado para el modelo.");
        Files.writeString(promptFile, "Instrucciones para inferir clases.");
        FakeProvider provider = new FakeProvider();

        ModelFromStatementSection section = onFx(() -> new ModelFromStatementSection(
            owner -> textFile.toFile(), owner -> promptFile.toFile(),
            (owner, current) -> selectedOutputDirectory.toFile(), List.of(provider), initialOutputDirectory));
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
            assertEquals("Modelo simulado",
                    ((Label) section.lookup("#model-statement-request-model")).getText());
                ((Button) section.lookup("#model-statement-change-output-folder")).fire();
                assertEquals(selectedOutputDirectory.toAbsolutePath().normalize().toString(),
                    ((Label) section.lookup("#model-statement-output-folder")).getText());
            ((Button) section.lookup("#model-statement-generate-model")).fire();
            return null;
        });

        Label generationStatus = onFx(() -> (Label) section.lookup("#model-statement-generation-status"));
        awaitStatus(generationStatus, "Análisis completado");
        onFx(() -> {
            TextArea result = (TextArea) section.lookup("#model-statement-generated-json");
            Label output = (Label) section.lookup(".model-generation-output-path");
            assertTrue(result.getText().contains("\"name\" : \"Usuario\""));
            assertTrue(generationStatus.getText().contains("Puedes continuar con el paso 5: Diagrama de clases."));
            assertEquals("Diagrama de clases generado correctamente. Flujo completado.",
                    ((Label) section.lookup("#model-statement-diagram-status")).getText());
            Path outputPath = Path.of(output.getText().substring("Resultado: ".length()));
            assertTrue(outputPath.startsWith(selectedOutputDirectory.toAbsolutePath().normalize()));
            assertTrue(Files.isRegularFile(outputPath));
            assertFalse(((Button) section.lookup("#statement-step-5")).isDisabled());
            assertEquals("Instrucciones para inferir clases.", provider.prompt.get());
            assertEquals("Texto del enunciado para el modelo.", provider.statement.get());
                ((Button) section.lookup("#statement-step-5")).fire();
                StackPane panelHost = (StackPane) section.getChildren().get(2);
                VBox diagramPanel = (VBox) panelHost.getChildren().get(4);
                VBox diagramContent = (VBox) diagramPanel.getChildren().get(0);
                ScrollPane diagramScroll = (ScrollPane) diagramContent.getChildren().get(1);
                ClassDiagramView diagram = (ClassDiagramView) diagramScroll.getContent();
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

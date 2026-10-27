package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.model.ModelIO;
import com.crud2gui.model.ManagerCandidate;
import com.crud2gui.model.Confidence;
import com.crud2gui.model.Evidence;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.model.Relation;
import com.crud2gui.model.RelationKind;
import com.crud2gui.model.TypeInfo;
import com.crud2gui.model.TypeKind;
import com.crud2gui.model.Visibility;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TreeCell;
import javafx.scene.input.ScrollEvent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelFromCodeSectionTest {

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
    void selectsFolderExtractsJsonAndShowsClassDiagram() throws Exception {
        Files.writeString(directory.resolve("Producto.java"), """
                package demo;

                public class Producto {
                    private final String nombre;

                    public Producto(String nombre) {
                        this.nombre = nombre;
                    }

                    public String getNombre() {
                        return nombre;
                    }
                }
                """);
        Files.writeString(directory.resolve("Lanzador.java"), """
                package demo;

                public class Lanzador {
                    public static void main(String[] args) {
                    }
                }
                """);
        Files.writeString(directory.resolve("Gestor.java"), """
                package demo;

                public class Gestor {
                    private Producto producto;
                }
                """);

        ModelFromCodeSection section = onFx(() -> new ModelFromCodeSection(owner -> directory.toFile()));
        onFx(() -> {
            ((Button) section.lookup("#model-code-select-folder")).fire();
            return null;
        });
        awaitFolderTree(section);
        onFx(() -> {
            assertFalse(((Button) section.lookup("#model-code-extract-model")).isDisabled());
            ((Button) section.lookup("#statement-step-2")).fire();
            ((Button) section.lookup("#model-code-extract-model")).fire();
            return null;
        });

        awaitStatus(section, "Modelo obtenido correctamente.");
        onFx(() -> {
            Label folderName = (Label) section.lookup(".statement-file-name");
            Label folderBadge = (Label) folderName.getGraphic();
            assertEquals("FOLDER", folderBadge.getText());
            TextArea json = (TextArea) section.lookup("#model-code-generated-json");
            assertTrue(json.getText().contains("\"schemaVersion\" : \"1.0\""));
            assertTrue(json.getText().contains("\"name\" : \"Producto\""));
            assertTrue(json.getText().contains("\"name\" : \"Lanzador\""));
            assertTrue(json.getText().contains("\"name\" : \"Gestor\""));
            assertTrue(json.getText().contains("\"packageName\" : \"demo\""));
            assertEquals("demo", ModelIO.fromJson(json.getText()).types().get(0).packageName());
            Button diagramStep = (Button) section.lookup("#statement-step-3");
            diagramStep.fire();
            assertTrue(diagramStep.getPseudoClassStates().stream()
                    .anyMatch(state -> state.getPseudoClassName().equals("selected")));
            StackPane panelHost = (StackPane) section.getChildren().get(2);
            VBox diagramPanel = (VBox) panelHost.getChildren().get(2);
            VBox diagramContent = (VBox) diagramPanel.getChildren().get(0);
            ComboBox<?> managerSelector = (ComboBox<?>) diagramContent.getChildren().get(2);
            assertTrue(((Label) section.lookup("#model-code-extraction-status")).getText()
                    .contains("Puedes continuar con el paso 3: Diagrama de clases."));
            assertEquals("Diagrama de clases generado correctamente. Flujo completado.",
                    ((Label) section.lookup("#model-code-diagram-status")).getText());
            String[] statusIds = {"model-code-selection-status", "model-code-extraction-status",
                    "model-code-diagram-status"};
            for (int index = 0; index < statusIds.length; index++) {
                VBox content = (VBox) ((VBox) panelHost.getChildren().get(index)).getChildren().get(0);
                assertEquals(section.lookup("#" + statusIds[index]),
                        content.getChildren().get(content.getChildren().size() - 1));
            }
            ClassDiagramView diagram = (ClassDiagramView)
                    ((ScrollPane) diagramContent.getChildren().get(3)).getContent();
            assertEquals(2, diagram.getChildren().stream()
                    .filter(node -> node.getStyleClass().contains("uml-class-card")).count());
            assertEquals(1, diagram.getChildren().stream()
                    .filter(node -> node.getStyleClass().contains("uml-manager-card")).count());
            assertTrue(diagram.getChildren().stream()
                    .filter(node -> node.getStyleClass().contains("uml-class-card"))
                    .map(node -> (VBox) node)
                    .map(card -> (HBox) card.getChildren().get(0))
                    .map(header -> (VBox) header.getChildren().get(0))
                    .map(title -> ((Label) title.getChildren().get(1)).getText())
                    .noneMatch("Lanzador"::equals));
            assertFalse(managerSelector.isVisible());
            assertFalse(((Button) section.lookup("#statement-step-2")).isDisabled());
            return null;
        });
    }

    @Test
    void reportsWhenFolderHasNoJavaFiles() throws Exception {
        ModelFromCodeSection section = onFx(() -> new ModelFromCodeSection(owner -> directory.toFile()));
        onFx(() -> {
            ((Button) section.lookup("#model-code-select-folder")).fire();
            return null;
        });
        awaitFolderTree(section);
        onFx(() -> {
            ((Button) section.lookup("#statement-step-2")).fire();
            ((Button) section.lookup("#model-code-extract-model")).fire();
            return null;
        });

        awaitStatus(section, "No se encontraron ficheros .java");
        onFx(() -> {
            assertTrue(((Button) section.lookup("#statement-step-3")).isDisabled());
            assertFalse(((Button) section.lookup("#model-code-extract-model")).isDisabled());
            return null;
        });
    }

    @Test
    void showsCompactEmptyFolderCardUntilAValidFolderIsSelected() throws Exception {
        ModelFromCodeSection section = onFx(() -> new ModelFromCodeSection(owner -> null));
        onFx(() -> {
            var card = section.lookup("#model-code-selected-folder");
            assertTrue(card.isVisible());
            assertTrue(card.isManaged());
            Label name = (Label) card.lookup(".statement-file-name");
            assertEquals("Ninguna carpeta seleccionada.", name.getText());
            assertTrue(name.getGraphic().getStyleClass().contains("statement-neutral-badge"));
            assertFalse(section.lookup("#model-code-folder-tree").isManaged());
            assertFalse(section.lookup("#model-code-folder-tree-status").isManaged());
            StackPane root = new StackPane(section);
            new Scene(root, 1200, 1000);
            root.resize(1200, 1000);
            root.applyCss();
            root.layout();
            StackPane host = (StackPane) section.getChildren().get(2);
            VBox panel = (VBox) host.getChildren().get(0);
            assertEquals(panel.prefHeight(panel.getWidth()), panel.getHeight(), 1);
            assertEquals(0, panel.getLayoutY(), 1);
            assertTrue(panel.getHeight() < host.getHeight() / 2);
            javafx.scene.layout.HBox flow = (javafx.scene.layout.HBox) section.getChildren().get(1);
            assertEquals(flow.prefWidth(-1), flow.getWidth(), 1);
            assertTrue(flow.getWidth() < section.getWidth() - section.getInsets().getLeft()
                    - section.getInsets().getRight());
            ((Button) section.lookup("#model-code-select-folder")).fire();
            assertTrue(card.isVisible());
            assertTrue(card.isManaged());
            assertTrue(((Button) section.lookup("#statement-step-2")).isDisabled());
            return null;
        });
    }

    @Test
    void showsScrollableReadOnlyFolderTreeThatFillsAvailableHeight() throws Exception {
        Path source = Files.createDirectories(directory.resolve("src"));
        Path packageFolder = Files.createDirectories(source.resolve("demo"));
        Files.writeString(packageFolder.resolve("Producto.java"), "package demo; class Producto {}");
        Files.writeString(directory.resolve("README.md"), "proyecto");
        for (int index = 0; index < 60; index++) {
            Files.writeString(packageFolder.resolve("Clase" + index + ".java"), "class Clase" + index + " {}");
        }

        ModelFromCodeSection section = onFx(() -> new ModelFromCodeSection(owner -> directory.toFile()));
        onFx(() -> {
            ((Button) section.lookup("#model-code-select-folder")).fire();
            return null;
        });

        awaitLabel(onFx(() -> (Label) section.lookup("#model-code-folder-tree-status")),
                "Contenido de la carpeta cargado.");
        onFx(() -> {
            TreeView<?> tree = (TreeView<?>) section.lookup("#model-code-folder-tree");
            var folderCard = section.lookup("#model-code-selected-folder");
            assertTrue(folderCard.isVisible());
            assertTrue(folderCard.isManaged());
            assertTrue(tree.isVisible());
            Label selectionStatus = (Label) section.lookup("#model-code-selection-status");
            assertEquals("Carpeta de código fuente seleccionada. Puedes continuar con el paso 2: Obtener modelo.",
                    selectionStatus.getText());
            assertTrue(selectionStatus.getStyleClass().contains("status-message"));
            assertFalse(selectionStatus.getPseudoClassStates().stream()
                    .anyMatch(state -> state.getPseudoClassName().equals("error")));
            assertFalse(((Button) section.lookup("#statement-step-2")).isDisabled());
            assertFalse(tree.isMouseTransparent());
            assertFalse(tree.isDisabled());
            assertEquals(null, tree.getSelectionModel());
            assertFalse(tree.isFocusTraversable());
            assertTrue(tree.getRoot().isExpanded());
            assertTrue(tree.getRoot().getValue().toString().contains(directory.getFileName().toString()));

            TreeItem<?> srcItem = tree.getRoot().getChildren().stream()
                    .filter(item -> item.getValue().toString().contains("src"))
                    .findFirst()
                    .orElseThrow();
            assertTrue(srcItem.isExpanded());
            TreeItem<?> demoItem = srcItem.getChildren().get(0);
            assertTrue(demoItem.isExpanded());
            assertTrue(demoItem.getChildren().stream()
                    .anyMatch(item -> item.getValue().toString().contains("Producto.java")));
            assertTrue(tree.getRoot().getChildren().stream()
                    .anyMatch(item -> item.getValue().toString().contains("README.md")));

            StackPane root = new StackPane(section);
            new Scene(root, 1200, 1000);
            root.resize(1200, 1000);
            root.applyCss();
            root.layout();
            double tallTreeHeight = tree.getHeight();
            assertTrue(tallTreeHeight > 300, "El arbol debe crecer mas alla de su altura preferida.");
            root.resize(1200, 750);
            root.layout();
            assertTrue(tree.getHeight() < tallTreeHeight);
            assertEquals(250, tallTreeHeight - tree.getHeight(), 1);
            assertTrue(tree.lookupAll(".tree-cell").stream()
                    .allMatch(cell -> ((TreeCell<?>) cell).isMouseTransparent()));
            List<TreeCell<?>> visibleCells = tree.lookupAll(".tree-cell").stream()
                    .map(node -> (TreeCell<?>) node)
                    .filter(cell -> !cell.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            assertTrue(visibleCells.stream().anyMatch(cell -> cell.getText().endsWith(".java")));
            for (TreeCell<?> cell : visibleCells) {
                assertEquals(cell.getText().endsWith(".java"),
                        cell.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("java-source")));
            }

            ScrollBar vertical = tree.lookupAll(".scroll-bar").stream()
                    .map(node -> (ScrollBar) node)
                    .filter(bar -> bar.getOrientation() == Orientation.VERTICAL)
                    .findFirst().orElseThrow();
            assertTrue(vertical.isVisible());
            double initialValue = vertical.getValue();
            tree.lookup(".virtual-flow").fireEvent(new ScrollEvent(
                    ScrollEvent.SCROLL, 20, 20, 20, 20,
                    false, false, false, false, false, false,
                    0, -120, 0, -120,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
            assertTrue(vertical.getValue() > initialValue, "La rueda debe desplazar el contenido.");
            double scrolledValue = vertical.getValue();
            vertical.increment();
            assertTrue(vertical.getValue() > scrolledValue, "La barra debe permitir desplazarse.");
            assertTrue(tree.getRoot().isExpanded());
            assertTrue(srcItem.isExpanded());
            assertTrue(demoItem.isExpanded());

            StackPane panelHost = (StackPane) section.getChildren().get(2);
            VBox extractionPanel = (VBox) panelHost.getChildren().get(1);
            VBox extractionContent = (VBox) extractionPanel.getChildren().get(0);
            TextArea json = (TextArea) extractionContent.lookup("#model-code-generated-json");
            assertEquals(Priority.ALWAYS, VBox.getVgrow(json));
            assertEquals(0, json.getMinHeight());
            return null;
        });
    }

    @Test
    void laysOutManagerUserProductAndChildrenInColumnsAndFiltersEnumDependencies() throws Exception {
        TypeInfo manager = type("demo.PlataformaCompraVenta", "PlataformaCompraVenta", TypeKind.CLASS);
        TypeInfo user = type("demo.Usuario", "Usuario", TypeKind.CLASS);
        TypeInfo product = type("demo.Producto", "Producto", TypeKind.CLASS);
        TypeInfo electronic = type("demo.ProductoElectronica", "ProductoElectronica", TypeKind.CLASS);
        TypeInfo state = type("demo.T_Estado", "T_Estado", TypeKind.ENUM);
        Relation managerUsers = relation(RelationKind.ASSOCIATION, manager, user);
        Relation userProducts = relation(RelationKind.ASSOCIATION, user, product);
        Relation productState = relation(RelationKind.ASSOCIATION, product, state);
        List<Relation> relations = List.of(
                managerUsers,
                userProducts,
                relation(RelationKind.INHERITANCE, electronic, product),
                productState,
                relation(RelationKind.DEPENDENCY, electronic, state),
                relation(RelationKind.DEPENDENCY, manager, state));
        ProjectModel model = new ProjectModel("1.0", null, null,
                List.of(manager, user, product, electronic, state), relations, null, null);

        assertEquals(List.of(managerUsers, userProducts,
                        relation(RelationKind.INHERITANCE, electronic, product), productState),
                ClassDiagramView.filterDiagramRelations(model,
                        Set.of(manager.id(), user.id(), product.id(), electronic.id(), state.id())));

        onFx(() -> {
            ClassDiagramView diagram = new ClassDiagramView();
            diagram.setModel(model, manager.id());
            diagram.applyCss();
            diagram.resize(diagram.getPrefWidth(), diagram.getPrefHeight());
            diagram.layout();

            Pane managerCard = cardNamed(diagram, "PlataformaCompraVenta");
            Pane userCard = cardNamed(diagram, "Usuario");
            Pane productCard = cardNamed(diagram, "Producto");
            Pane electronicCard = cardNamed(diagram, "ProductoElectronica");
            Pane stateCard = cardNamed(diagram, "T_Estado");
            assertTrue(managerCard.getLayoutX() < userCard.getLayoutX());
            assertTrue(userCard.getLayoutX() < productCard.getLayoutX());
            assertTrue(productCard.getLayoutX() < electronicCard.getLayoutX());
            assertEquals(electronicCard.getLayoutX(), stateCard.getLayoutX());
            assertTrue(electronicCard.getLayoutY() < stateCard.getLayoutY());
            return null;
        });
    }

    private static TypeInfo type(String id, String name, TypeKind kind) {
        return new TypeInfo(id, name, "demo", kind, Visibility.PUBLIC, List.of(),
                null, null, List.of(), List.of(), List.of(), null, null, false,
                List.of(), List.of(), List.of(), List.of());
    }

    private static Relation relation(RelationKind kind, TypeInfo from, TypeInfo to) {
        return new Relation(kind, from.id(), to.id(), null, null, Confidence.HIGH,
                new Evidence(false, false, false, false, null));
    }

    private static Pane cardNamed(ClassDiagramView diagram, String name) {
        return (Pane) diagram.getChildren().stream()
                .filter(node -> node.getStyleClass().contains("uml-class-card"))
                .filter(node -> {
                    var header = (javafx.scene.layout.HBox) ((Pane) node).getChildren().get(0);
                    var title = (javafx.scene.layout.VBox) header.getChildren().get(0);
                    return ((Label) title.getChildren().get(1)).getText().equals(name);
                })
                .findFirst()
                .orElseThrow();
    }

    @Test
    void letsUserChooseBetweenTiedManagerCandidates() throws Exception {
        Files.writeString(directory.resolve("Producto.java"), """
                package demo;

                public class Producto {
                }
                """);
        Files.writeString(directory.resolve("GestorA.java"), """
                package demo;

                public class GestorA {
                    private Producto producto;
                }
                """);
        Files.writeString(directory.resolve("GestorB.java"), """
                package demo;

                public class GestorB {
                    private Producto producto;
                }
                """);

        ModelFromCodeSection section = onFx(() -> new ModelFromCodeSection(owner -> directory.toFile()));
        onFx(() -> {
            ((Button) section.lookup("#model-code-select-folder")).fire();
            return null;
        });
        awaitFolderTree(section);
        onFx(() -> {
            ((Button) section.lookup("#statement-step-2")).fire();
            ((Button) section.lookup("#model-code-extract-model")).fire();
            return null;
        });

        awaitStatus(section, "Modelo obtenido correctamente.");
        onFx(() -> {
            StackPane panelHost = (StackPane) section.getChildren().get(2);
            VBox diagramPanel = (VBox) panelHost.getChildren().get(2);
            VBox diagramContent = (VBox) diagramPanel.getChildren().get(0);
            @SuppressWarnings("unchecked")
            ComboBox<ManagerCandidate> managerSelector =
                    (ComboBox<ManagerCandidate>) diagramContent.getChildren().get(2);
            assertTrue(managerSelector.isVisible());
            assertEquals(2, managerSelector.getItems().size());
            for (ManagerCandidate candidate : managerSelector.getItems()) {
                assertTrue(managerSelector.getConverter().toString(candidate).contains("certeza "));
            }

            ManagerCandidate alternative = managerSelector.getItems().stream()
                    .filter(candidate -> !candidate.equals(managerSelector.getValue()))
                    .findFirst()
                    .orElseThrow();
            managerSelector.setValue(alternative);
            ClassDiagramView diagram = (ClassDiagramView)
                    ((ScrollPane) diagramContent.getChildren().get(3)).getContent();
            VBox managerCard = (VBox) diagram.getChildren().stream()
                    .filter(node -> node.getStyleClass().contains("uml-manager-card"))
                    .findFirst()
                    .orElseThrow();
            HBox header = (HBox) managerCard.getChildren().get(0);
            VBox title = (VBox) header.getChildren().get(0);
            assertEquals(alternative.id().substring(alternative.id().lastIndexOf('.') + 1),
                    ((Label) title.getChildren().get(1)).getText());
            return null;
        });
    }

    private static void awaitStatus(ModelFromCodeSection section, String expectedText) throws Exception {
        awaitLabel(onFx(() -> (Label) section.lookup("#model-code-extraction-status")), expectedText);
    }

    private static void awaitFolderTree(ModelFromCodeSection section) throws Exception {
        awaitLabel(onFx(() -> (Label) section.lookup("#model-code-folder-tree-status")),
                "Contenido de la carpeta cargado.");
    }

    private static void awaitLabel(Label status, String expectedText) throws Exception {
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
            throw new AssertionError("Esperaba el estado '" + expectedText + "', pero se mostró: '"
                    + status.getText() + "'.", timeout);
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

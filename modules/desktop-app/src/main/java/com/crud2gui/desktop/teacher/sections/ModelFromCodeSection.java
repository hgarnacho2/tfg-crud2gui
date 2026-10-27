package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.FileDialogs;
import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.javamodelextractor.ProjectParser;
import com.crud2gui.model.ManagerCandidate;
import com.crud2gui.model.ModelIO;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.model.TypeInfo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Sección para extraer un modelo de clases desde código fuente Java. */
public final class ModelFromCodeSection extends Section {

    private final Function<Window, File> chooseCodeFolder;
    private final Button selectFolderButton =
            UiComponents.actionButton("Seleccionar carpeta", List.of("statement-select"),
                    this::selectFolder);
    private final Button extractModelButton =
            UiComponents.actionButton("Obtener modelo", List.of("statement-select"),
                    this::extractModel);
    private final Label selectedFolderName = UiComponents.labelWithGraphic(
            "Ninguna carpeta seleccionada.", List.of("statement-file-name"),
            UiComponents.label("FOLDER", List.of("statement-file-badge", "statement-neutral-badge")), 12);
    private final Label selectedFolderPath =
            UiComponents.label("Selecciona la carpeta que contiene los ficheros Java.",
                    List.of("statement-file-path"));
    private final Label folderTreeStatus = UiComponents.label(
            "Selecciona una carpeta para ver su contenido.", List.of("statement-file-path"));
    private final TreeView<FolderEntry> folderTree = new TreeView<>();
    private final VBox selectedFolderCard =
            UiComponents.vBox(List.of("statement-file-card"), selectedFolderName, selectedFolderPath,
                    folderTreeStatus, folderTree);
    private final Label selectionStatus = UiComponents.statusLabel("model-code-selection-status");
    private final Label extractionStatus = UiComponents.statusLabel("model-code-extraction-status");
    private final Label diagramStatus = UiComponents.statusLabel("model-code-diagram-status");
    private final TextArea generatedModel = UiComponents.textArea(
            false, false, 20, List.of("model-json-result"));
    private final ClassDiagramView classDiagram = new ClassDiagramView();
    private final ScrollPane diagramScroll = new ScrollPane(classDiagram);
    private final Label managerSelectionPrompt = UiComponents.label(
            "Hay empate entre las candidatas a clase gestora. Selecciona una:",
            List.of("model-llm-field-title"));
    private final ComboBox<ManagerCandidate> managerSelector = new ComboBox<>();
    private final List<Button> steps;
    private final List<VBox> panels;
    private Path sourceFolder;
    private boolean extracting;
    private boolean loadingFolderContents;

    /** Crea la sección con un selector de carpetas del sistema. */
    public ModelFromCodeSection() {
        this(owner -> FileDialogs.chooseDirectory(owner, null, "Seleccionar carpeta con código Java"));
    }

    ModelFromCodeSection(Function<Window, File> chooseCodeFolder) {
        super("MODELO DESDE CÓDIGO", "Modelo desde código",
                "Extracción de un modelo de clases a partir de código fuente Java.");
        this.chooseCodeFolder = Objects.requireNonNull(chooseCodeFolder, "chooseCodeFolder");
        setMinHeight(Region.USE_PREF_SIZE);

        selectFolderButton.setId("model-code-select-folder");
        extractModelButton.setId("model-code-extract-model");
        extractModelButton.setDisable(true);
        selectedFolderCard.setId("model-code-selected-folder");
        folderTreeStatus.setVisible(false);
        folderTreeStatus.setManaged(false);
        generatedModel.setId("model-code-generated-json");
        generatedModel.setMinHeight(0);
        generatedModel.setMaxHeight(Double.MAX_VALUE);
        generatedModel.setVisible(false);
        generatedModel.setManaged(false);
        folderTree.setId("model-code-folder-tree");
        folderTree.getStyleClass().add("model-code-folder-tree");
        folderTree.setShowRoot(true);
        folderTree.setEditable(false);
        folderTree.setFocusTraversable(false);
        folderTree.setSelectionModel(null);
        folderTree.setFixedCellSize(28);
        folderTree.setMinHeight(140);
        folderTree.setPrefHeight(300);
        folderTree.setMaxHeight(Double.MAX_VALUE);
        folderTree.setVisible(false);
        folderTree.setManaged(false);
        folderTree.setCellFactory(ignored -> new TreeCell<>() {
            {
                setMouseTransparent(true);
            }

            @Override
            protected void updateItem(FolderEntry entry, boolean empty) {
                super.updateItem(entry, empty);
                setText(empty || entry == null ? null
                        : entry.name() + (entry.directory() ? File.separator : ""));
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("java-source"),
                        !empty && entry != null && !entry.directory() && entry.name().endsWith(".java"));
            }
        });
        VBox.setVgrow(folderTree, Priority.ALWAYS);
        VBox.setVgrow(selectedFolderCard, Priority.ALWAYS);
        folderTreeStatus.setId("model-code-folder-tree-status");

        VBox selectContent = new VBox(14,
                UiComponents.panelHeading("Seleccionar código",
                        "Selecciona la carpeta que contiene los ficheros Java de la aplicación."),
                selectFolderButton, selectedFolderCard, selectionStatus);
        VBox.setVgrow(selectContent, Priority.ALWAYS);
        VBox selectPanel = UiComponents.surface(selectContent);
        selectPanel.maxHeightProperty().bind(javafx.beans.binding.Bindings
                .when(folderTree.managedProperty())
                .then(Double.MAX_VALUE).otherwise(Region.USE_PREF_SIZE));
        StackPane.setAlignment(selectPanel, javafx.geometry.Pos.TOP_LEFT);

        VBox extractionContent = new VBox(14,
                UiComponents.panelHeading("Obtener modelo",
                        "Analiza los ficheros Java y genera un modelo JSON compatible con el modelo del enunciado."),
                extractModelButton, generatedModel, extractionStatus);
        VBox.setVgrow(generatedModel, Priority.ALWAYS);
        VBox.setVgrow(extractionContent, Priority.ALWAYS);
        VBox extractionPanel = UiComponents.surface(extractionContent);

        classDiagram.setId("model-code-class-diagram-view");
        managerSelector.setId("model-code-manager-selector");
        managerSelector.getStyleClass().add("model-llm-selector");
        managerSelector.setMaxWidth(Double.MAX_VALUE);
        managerSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ManagerCandidate candidate) {
                return candidate == null ? "" : managerOptionLabel(candidate);
            }

            @Override
            public ManagerCandidate fromString(String value) {
                return null;
            }
        });
        managerSelector.valueProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                classDiagram.setModel(classDiagram.getModel(), selected.id());
            }
        });
        managerSelectionPrompt.setId("model-code-manager-selection-prompt");
        managerSelectionPrompt.setVisible(false);
        managerSelectionPrompt.setManaged(false);
        diagramScroll.setId("model-code-class-diagram-scroll");
        diagramScroll.setPannable(true);
        diagramScroll.setFitToWidth(false);
        diagramScroll.setFitToHeight(false);
        diagramScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        diagramScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        diagramScroll.setMinHeight(0);
        VBox diagramContent = new VBox(14,
                UiComponents.panelHeading("Diagrama de clases UML",
                        "Representación visual del modelo extraído del código."),
                managerSelectionPrompt, managerSelector,
                diagramScroll, diagramStatus);
        VBox.setVgrow(diagramScroll, Priority.ALWAYS);
        VBox.setVgrow(diagramContent, Priority.ALWAYS);
        VBox diagramPanel = UiComponents.surface(diagramContent);

        panels = List.of(selectPanel, extractionPanel, diagramPanel);
        steps = List.of(
                UiComponents.stepButton(0, "SELECCIONAR CÓDIGO", "Carpeta de código Java", this::showStep),
                UiComponents.stepButton(1, "OBTENER MODELO", "Extracción del modelo", this::showStep),
                UiComponents.stepButton(2, "DIAGRAMA DE CLASES", "Representación visual", this::showStep));
        HBox flow = new HBox(8,
                steps.get(0), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(1), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(2));
        flow.getStyleClass().add("statement-flow");
        flow.getStyleClass().add("model-statement-flow");
        flow.setMaxHeight(Region.USE_PREF_SIZE);
        flow.setMaxWidth(Region.USE_PREF_SIZE);

        StackPane panelHost = new StackPane();
        panelHost.getChildren().addAll(panels);
        VBox.setVgrow(panelHost, Priority.ALWAYS);
        getChildren().addAll(flow, panelHost);
        steps.get(1).setDisable(true);
        steps.get(2).setDisable(true);
        showStep(0);
    }

    private void showStep(int index) {
        if (steps.get(index).isDisabled()) {
            return;
        }
        for (int i = 0; i < panels.size(); i++) {
            panels.get(i).setVisible(i == index);
            panels.get(i).setManaged(i == index);
            steps.get(i).pseudoClassStateChanged(SELECTED, i == index);
        }
        if (index == 2) {
            Platform.runLater(() -> {
                diagramScroll.applyCss();
                diagramScroll.layout();
                diagramScroll.setHvalue(diagramScroll.getHmin());
                diagramScroll.setVvalue((diagramScroll.getVmin() + diagramScroll.getVmax()) / 2);
            });
        }
    }

    private void selectFolder() {
        File folder = chooseCodeFolder.apply(getScene() == null ? null : getScene().getWindow());
        if (folder == null) {
            return;
        }
        if (!folder.isDirectory()) {
            showStatus(selectionStatus, "La ruta seleccionada no es una carpeta.", true);
            return;
        }
        sourceFolder = folder.toPath().toAbsolutePath().normalize();
        Path selectedFolder = sourceFolder;
        selectedFolderName.setText(folder.getName().isBlank() ? sourceFolder.toString() : folder.getName());
        selectedFolderPath.setText(sourceFolder.toString());
        selectedFolderCard.setVisible(true);
        selectedFolderCard.setManaged(true);
        selectedFolderCard.pseudoClassStateChanged(SELECTED, true);
        invalidateModel();
        folderTree.setRoot(null);
        folderTree.setVisible(true);
        folderTree.setManaged(true);
        folderTreeStatus.setVisible(true);
        folderTreeStatus.setManaged(true);
        folderTreeStatus.setText("Leyendo contenido de la carpeta...");
        showStatus(selectionStatus, "Leyendo contenido de la carpeta seleccionada...", false);
        steps.get(0).pseudoClassStateChanged(COMPLETED, true);
        steps.get(1).setDisable(false);
        loadingFolderContents = true;
        updateControls();
        runTask(() -> readFolderTree(selectedFolder), root -> {
            TreeItem<FolderEntry> treeRoot = toTreeItem(root);
            expandTree(treeRoot);
            folderTree.setRoot(treeRoot);
            folderTreeStatus.setText("Contenido de la carpeta cargado.");
            showStatus(selectionStatus,
                    "Carpeta de código fuente seleccionada. Puedes continuar con el paso 2: Obtener modelo.", false);
        }, failure -> {
            logger.error("MODEL_CODE_FOLDER_TREE_ERROR | No se pudo leer el contenido de la carpeta.", failure);
            String detail = failure.getMessage() == null
                    ? failure.getClass().getSimpleName() : failure.getMessage();
            folderTreeStatus.setText("No se pudo mostrar el contenido: " + detail);
            showStatus(selectionStatus, "Carpeta seleccionada, pero no se pudo mostrar su contenido: "
                    + detail + ". Puedes intentar obtener el modelo en el paso 2.", true);
        }, () -> {
            loadingFolderContents = false;
            updateControls();
        });
    }

    private void extractModel() {
        if (sourceFolder == null || extracting || loadingFolderContents) {
            return;
        }
        Path folder = sourceFolder;
        invalidateModel();
        extracting = true;
        updateControls();
        showStatus(extractionStatus, "Analizando los ficheros Java de la carpeta seleccionada...", false);

        runTask(() -> {
            ProjectModel model = new ProjectParser().parse(folder);
            if (model.source().filesParsed() == 0) {
                throw new IOException("No se encontraron ficheros .java en la carpeta seleccionada.");
            }
            if (model.types().isEmpty()) {
                throw new IOException("No se pudieron extraer tipos Java de los ficheros encontrados.");
            }
            String json = ModelIO.toJson(model);
            return new ExtractedModel(model, json);
        }, result -> {
            generatedModel.setText(result.json());
            generatedModel.positionCaret(0);
            generatedModel.setVisible(true);
            generatedModel.setManaged(true);
            configureManagerSelection(result.model());
            String warnings = result.model().source().warnings().isEmpty()
                    ? "" : " Se encontraron " + result.model().source().warnings().size() + " aviso(s).";
            showStatus(extractionStatus,
                    "Modelo obtenido correctamente. Puedes continuar con el paso 3: Diagrama de clases.\n"
                    + result.model().types().size()
                    + " tipo(s) en " + result.model().source().filesParsed() + " fichero(s)." + warnings, false);
            steps.get(1).pseudoClassStateChanged(COMPLETED, true);
            steps.get(2).setDisable(false);
        }, failure -> {
            String detail = failure.getMessage() == null
                    ? failure.getClass().getSimpleName() : failure.getMessage();
            showStatus(extractionStatus, "No se pudo obtener el modelo: " + detail, true);
        }, () -> {
            extracting = false;
            updateControls();
        });
    }

    private void invalidateModel() {
        steps.get(2).setDisable(true);
        steps.get(1).pseudoClassStateChanged(COMPLETED, false);
        steps.get(2).pseudoClassStateChanged(COMPLETED, false);
        generatedModel.clear();
        showStatus(extractionStatus, "", false);
        generatedModel.setVisible(false);
        generatedModel.setManaged(false);
        classDiagram.setModel(null);
        showStatus(diagramStatus, "", false);
        managerSelector.getItems().clear();
        managerSelector.setValue(null);
        managerSelector.setVisible(false);
        managerSelector.setManaged(false);
        managerSelectionPrompt.setVisible(false);
        managerSelectionPrompt.setManaged(false);
    }

    private void configureManagerSelection(ProjectModel model) {
        List<ManagerCandidate> candidates = managerCandidates(model);
        double highestScore = candidates.stream()
                .mapToDouble(ManagerCandidate::score)
                .max()
                .orElse(Double.NaN);
        List<ManagerCandidate> tiedCandidates = candidates.stream()
                .filter(candidate -> Double.compare(candidate.score(), highestScore) == 0)
                .toList();
        boolean tie = tiedCandidates.size() > 1;
        ManagerCandidate detected = model.analysis() == null ? null : model.analysis().managerClass();
        classDiagram.setModel(model, detected == null ? null : detected.id());
        showStatus(diagramStatus, "Diagrama de clases generado correctamente. Flujo completado.", false);
        managerSelector.setItems(FXCollections.observableArrayList(tiedCandidates));
        managerSelector.setVisible(tie);
        managerSelector.setManaged(tie);
        managerSelectionPrompt.setVisible(tie);
        managerSelectionPrompt.setManaged(tie);

        if (tie && detected != null && tiedCandidates.stream().anyMatch(candidate -> candidate.id().equals(detected.id()))) {
            managerSelector.setValue(detected);
        } else {
            managerSelector.setValue(null);
        }
    }

    private static List<ManagerCandidate> managerCandidates(ProjectModel model) {
        if (model.analysis() == null || model.analysis().managerClass() == null) {
            return List.of();
        }
        List<ManagerCandidate> candidates = new ArrayList<>();
        candidates.add(model.analysis().managerClass());
        candidates.addAll(model.analysis().alternatives());
        return candidates.stream()
                .filter(candidate -> candidate.id() != null)
                .collect(java.util.stream.Collectors.toMap(
                        ManagerCandidate::id, candidate -> candidate, (first, ignored) -> first,
                        java.util.LinkedHashMap::new))
                .values().stream()
                .sorted(Comparator.comparingDouble(ManagerCandidate::score).reversed()
                        .thenComparing(ManagerCandidate::id))
                .toList();
    }

    private String managerOptionLabel(ManagerCandidate candidate) {
        TypeInfo type = classDiagram.getModel() == null ? null : classDiagram.getModel().types().stream()
                .filter(item -> item.id().equals(candidate.id()))
                .findFirst()
                .orElse(null);
        String name = type == null || type.name() == null ? candidate.id() : type.name();
        if (type != null) {
            String simpleName = name;
            if (classDiagram.getModel().types().stream()
                    .filter(item -> simpleName.equals(item.name()))
                    .count() > 1) {
                name = candidate.id();
            }
        }
        return name + " — certeza " + switch (candidate.confidence()) {
            case HIGH -> "alta";
            case MEDIUM -> "media";
            case LOW -> "baja";
        };
    }

    private void updateControls() {
        boolean busy = extracting || loadingFolderContents;
        selectFolderButton.setDisable(busy);
        extractModelButton.setDisable(busy || sourceFolder == null);
        steps.get(0).setDisable(busy);
        steps.get(1).setDisable(busy || sourceFolder == null);
    }

    private static FolderEntry readFolderTree(Path directory) throws IOException {
        List<Path> children;
        try (var entries = Files.list(directory)) {
            children = entries.sorted(Comparator
                    .comparing((Path path) -> !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }
        List<FolderEntry> childEntries = new ArrayList<>(children.size());
        for (Path child : children) {
            boolean childDirectory = Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS);
            childEntries.add(childDirectory
                    ? readFolderTree(child)
                    : new FolderEntry(child.getFileName().toString(), false, List.of()));
        }
        String name = directory.getFileName() == null ? directory.toString() : directory.getFileName().toString();
        return new FolderEntry(name, true, childEntries);
    }

    private static TreeItem<FolderEntry> toTreeItem(FolderEntry entry) {
        TreeItem<FolderEntry> item = new TreeItem<>(entry);
        entry.children().stream().map(ModelFromCodeSection::toTreeItem).forEach(item.getChildren()::add);
        return item;
    }

    private static void expandTree(TreeItem<FolderEntry> item) {
        item.setExpanded(true);
        item.getChildren().forEach(ModelFromCodeSection::expandTree);
    }

    private record FolderEntry(String name, boolean directory, List<FolderEntry> children) {
        private FolderEntry {
            children = List.copyOf(children);
        }
    }

    private record ExtractedModel(ProjectModel model, String json) { }
}

package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.common.ui.JsonEditorView;
import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.FileDialogs;
import com.crud2gui.model.ModelIO;
import com.crud2gui.model.ProjectModel;
import com.crud2gui.statementanalyzer.client.ModelClient;
import com.crud2gui.statementanalyzer.client.ModelClientProvider;
import com.crud2gui.statementanalyzer.exception.InvalidModelOutputException;
import com.crud2gui.statementanalyzer.model.AnalysisResult;
import com.crud2gui.statementanalyzer.model.ModelClients;
import com.crud2gui.statementanalyzer.model.GenerationOptions;
import com.crud2gui.statementanalyzer.model.ModelInfo;
import com.crud2gui.statementanalyzer.prompt.StatementAnalyzer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Sección para generar un modelo de clases a partir de un enunciado. */
public final class ModelFromStatementSection extends Section {

    private final Function<Window, File> chooseTextFile;
    private final Function<Window, File> choosePromptFile;
        private final Label selectedTextName = UiComponents.labelWithGraphic(
            "Ningún texto filtrado seleccionado.", List.of("statement-file-name"),
            UiComponents.label("TXT", List.of("statement-file-badge", "statement-neutral-badge")), 12);
        private final Label selectedFile = UiComponents.label(
            "Selecciona el fichero de texto filtrado para comenzar.", List.of("statement-file-path"));
        private final VBox selectedTextCard =
            UiComponents.vBox(List.of("statement-file-card"), selectedTextName, selectedFile);
    private final TextArea statementText =
            UiComponents.textArea(false, true, 16, List.of("statement-text"));
    private final Label status = UiComponents.statusLabel("model-statement-status");
        private final Label selectedPromptName = UiComponents.labelWithGraphic(
            "Ningún prompt seleccionado.", List.of("statement-file-name"),
            UiComponents.label("TXT", List.of("statement-file-badge", "statement-neutral-badge")), 12);
        private final Label selectedPromptFile = UiComponents.label(
            "Selecciona el fichero con las instrucciones para inferir el modelo.",
            List.of("statement-file-path"));
        private final VBox selectedPromptCard =
            UiComponents.vBox(List.of("statement-file-card"), selectedPromptName, selectedPromptFile);
    private final TextArea promptText =
            UiComponents.textArea(false, true, 16, List.of("statement-prompt-text"));
    private final Label promptStatus = UiComponents.statusLabel("model-statement-prompt-status");
        private final ComboBox<ModelClientProvider> providerSelector = new ComboBox<>();
        private final ComboBox<ModelInfo> modelSelector = new ComboBox<>();
        private final Button refreshModelsButton = UiComponents.actionButton(
            "Actualizar modelos", List.of("statement-select"), this::refreshModels);
        private final Label modelStatus = UiComponents.statusLabel("model-statement-llm-status");
        private ModelClient selectedClient;
        private final BiFunction<Window, Path, File> chooseOutputFolder;
        private Path outputFolder;
        private final Label outputFilename = UiComponents.labelWithGraphic("Resultado JSON con fecha y hora",
            List.of("statement-file-name"), UiComponents.label("JSON", List.of("statement-file-badge")), 12);
        private final Label outputFolderLabel = UiComponents.label("", List.of("statement-file-path", "statement-output-folder"));
        private final Button changeOutputFolderButton = UiComponents.actionButton("Cambiar carpeta de salida",
            List.of("statement-change-folder", "secondary-button"), this::changeOutputFolder);
        private final Button openOutputFolderButton = UiComponents.actionButton("Abrir carpeta de salida",
            List.of("statement-open-folder", "secondary-button"), this::openOutputFolder);
        private final Label requestProvider = requestValue("model-statement-request-provider");
        private final Label requestModel = requestValue("model-statement-request-model");
        private final Label requestPrompt = requestValue("model-statement-request-prompt");
        private final Label requestStatement = requestValue("model-statement-request-statement");
        private final Button generateModelButton = UiComponents.actionButton(
            "Obtener modelo", List.of("statement-select"), this::generateModel);
        private final Label generationStatus = UiComponents.statusLabel("model-statement-generation-status");
        private final Label diagramStatus = UiComponents.statusLabel("model-statement-diagram-status");
        private final Label generationOutputPath = UiComponents.label(
            "", List.of("statement-file-path", "model-generation-output-path"), true);
        private final TextArea generatedModel = UiComponents.textArea(
            false, false, 20, List.of("model-json-result"));
        private final ClassDiagramView classDiagram = new ClassDiagramView();
        private final ScrollPane diagramScroll = new ScrollPane(classDiagram);
        private final StackPane editorHost = new StackPane();
        private final Button confirmModelButton = UiComponents.actionButton(
            "Confirmar modelo", List.of("statement-select"), this::confirmModel);
        private final Label refineStatus = UiComponents.statusLabel("model-statement-refine-status");
        private JsonEditorView modelEditor;
        private Path generatedModelPath;
        private boolean generationBusy;
    private final List<Button> steps;
    private final List<VBox> panels;

    /** Crea la sección con un selector de archivos de texto. */
    public ModelFromStatementSection() {
        this(owner -> chooseTextFile(owner, "Seleccionar texto filtrado del enunciado"),
            owner -> chooseTextFile(owner, "Seleccionar prompt de análisis"),
                (owner, current) -> FileDialogs.chooseDirectory(owner, current, "Carpeta de salida del modelo"),
                ModelClients.providers(), defaultOutputDirectory());
    }

    ModelFromStatementSection(Function<Window, File> chooseTextFile) {
        this(chooseTextFile, owner -> chooseTextFile(owner, "Seleccionar prompt de análisis"),
                (owner, current) -> FileDialogs.chooseDirectory(owner, current, "Carpeta de salida del modelo"),
                ModelClients.providers(), defaultOutputDirectory());
    }

    ModelFromStatementSection(Function<Window, File> chooseTextFile,
                              Function<Window, File> choosePromptFile) {
        this(chooseTextFile, choosePromptFile, ModelClients.providers(),
            defaultOutputDirectory());
        }

        ModelFromStatementSection(Function<Window, File> chooseTextFile,
                      Function<Window, File> choosePromptFile,
                      List<ModelClientProvider> providers, Path outputDirectory) {
        this(chooseTextFile, choosePromptFile,
            (owner, current) -> FileDialogs.chooseDirectory(owner, current, "Carpeta de salida del modelo"),
            providers, outputDirectory);
        }

        ModelFromStatementSection(Function<Window, File> chooseTextFile,
                      Function<Window, File> choosePromptFile,
                      BiFunction<Window, Path, File> chooseOutputFolder,
                      List<ModelClientProvider> providers, Path outputDirectory) {
        super("MODELO DESDE ENUNCIADO", "Modelo desde enunciado",
                "Generación de un modelo de clases a partir del texto filtrado de un enunciado.");
        setMinHeight(Region.USE_PREF_SIZE);
        this.chooseTextFile = Objects.requireNonNull(chooseTextFile, "chooseTextFile");
        this.choosePromptFile = Objects.requireNonNull(choosePromptFile, "choosePromptFile");
        this.chooseOutputFolder = Objects.requireNonNull(chooseOutputFolder, "chooseOutputFolder");
        this.outputFolder = Objects.requireNonNull(outputDirectory, "outputDirectory")
            .toAbsolutePath().normalize();
        Button selectTextButton = UiComponents.actionButton("Seleccionar texto filtrado",
                List.of("statement-select"), this::selectTextFile);
        selectTextButton.setId("model-statement-select-text");
        selectedTextCard.setId("model-statement-text-card");
        selectedFile.setId("model-statement-source");
        statementText.setId("model-statement-text");
        statementText.setVisible(false);
        statementText.setManaged(false);
        Button selectPromptButton = UiComponents.actionButton("Seleccionar prompt",
                List.of("statement-select"), this::selectPromptFile);
        selectPromptButton.setId("model-statement-select-prompt");
        selectedPromptCard.setId("model-statement-prompt-card");
        selectedPromptFile.setId("model-statement-prompt-source");
        promptText.setId("model-statement-prompt-text");
        promptText.setVisible(false);
        promptText.setManaged(false);
        providerSelector.setId("model-statement-provider");
        styleSelector(providerSelector);
        providerSelector.setPromptText("Seleccionar cliente");
        providerSelector.setMaxWidth(Double.MAX_VALUE);
        providerSelector.setItems(FXCollections.observableArrayList(providers));
        providerSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModelClientProvider provider) {
                return provider == null ? "" : provider.displayName();
            }

            @Override
            public ModelClientProvider fromString(String value) {
                return null;
            }
        });
        modelSelector.setId("model-statement-model");
        styleSelector(modelSelector);
        modelSelector.setPromptText("Seleccionar modelo");
        modelSelector.setMaxWidth(Double.MAX_VALUE);
        modelSelector.setDisable(true);
        modelSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(ModelInfo model) {
                return model == null ? "" : model.displayName();
            }

            @Override
            public ModelInfo fromString(String value) {
                return null;
            }
        });
        refreshModelsButton.setId("model-statement-refresh-models");
        refreshModelsButton.setDisable(true);
        modelStatus.setText(providerSelector.getItems().isEmpty()
                ? "No hay clientes LLM registrados." : "Selecciona un cliente para consultar sus modelos.");
        generateModelButton.setId("model-statement-generate-model");
        changeOutputFolderButton.setId("model-statement-change-output-folder");
        openOutputFolderButton.setId("model-statement-open-output-folder");
        outputFolderLabel.setId("model-statement-output-folder");
        updateOutputFolderLabel();
        generatedModel.setId("model-statement-generated-json");
        generatedModel.setVisible(false);
        generatedModel.setManaged(false);

        VBox statementContent = new VBox(14,
                UiComponents.panelHeading("Seleccionar texto del enunciado",
                        "Selecciona el fichero de texto filtrado generado en la sección Enunciado."),
                selectTextButton, selectedTextCard, statementText, status);
        VBox.setVgrow(statementText, Priority.ALWAYS);
        VBox.setVgrow(statementContent, Priority.ALWAYS);
        VBox statementPanel = UiComponents.surface(statementContent);
        statementPanel.maxHeightProperty().bind(javafx.beans.binding.Bindings
                .when(statementText.managedProperty())
                .then(Double.MAX_VALUE).otherwise(Region.USE_PREF_SIZE));
        StackPane.setAlignment(statementPanel, javafx.geometry.Pos.TOP_LEFT);
        VBox promptContent = new VBox(14,
            UiComponents.panelHeading("Seleccionar prompt",
                "Selecciona el fichero de texto con las instrucciones para inferir el modelo."),
            selectPromptButton, selectedPromptCard, promptText, promptStatus);
        VBox.setVgrow(promptText, Priority.ALWAYS);
        VBox.setVgrow(promptContent, Priority.ALWAYS);
        VBox promptPanel = UiComponents.surface(promptContent);
        promptPanel.maxHeightProperty().bind(javafx.beans.binding.Bindings
                .when(promptText.managedProperty())
                .then(Double.MAX_VALUE).otherwise(Region.USE_PREF_SIZE));
        StackPane.setAlignment(promptPanel, javafx.geometry.Pos.TOP_LEFT);
        VBox llmPanel = UiComponents.surface(new VBox(14,
            UiComponents.panelHeading("Seleccionar LLM",
                "Selecciona un proveedor y consulta los modelos disponibles."),
            fieldTitle("Cliente", "model-statement-provider-title"),
            providerSelector, refreshModelsButton,
            fieldTitle("Modelo", "model-statement-model-title"),
            modelSelector, modelStatus));
            VBox requestSummary = new VBox(8,
                UiComponents.panelHeading("Petición al LLM",
                    "Se enviarán estas entradas para inferir el modelo de clases."),
                requestRow("Cliente", requestProvider),
                requestRow("Modelo", requestModel),
                requestRow("Prompt", requestPrompt),
                requestRow("Enunciado", requestStatement));
            requestSummary.getStyleClass().add("model-request-summary");
            VBox generationContent = new VBox(14,
                UiComponents.panelHeading("Obtener modelo",
                    "Envía el prompt y el enunciado al cliente seleccionado."),
                requestSummary, UiComponents.outputSettings(outputFilename, outputFolderLabel,
                    changeOutputFolderButton),
                generateModelButton, generationOutputPath,
                openOutputFolderButton, generatedModel, generationStatus);
            VBox.setVgrow(generatedModel, Priority.ALWAYS);
            VBox.setVgrow(generationContent, Priority.ALWAYS);
            VBox generationPanel = UiComponents.surface(generationContent);
            editorHost.setId("model-statement-editor-host");
            editorHost.setMinHeight(480);
            confirmModelButton.setId("model-statement-confirm-model");
            VBox refineContent = new VBox(14,
                UiComponents.panelHeading("Afinar modelo",
                    "Revisa y edita el modelo obtenido. Al confirmarlo se guardará una copia validada."),
                editorHost, confirmModelButton, refineStatus);
            VBox.setVgrow(editorHost, Priority.ALWAYS);
            VBox.setVgrow(refineContent, Priority.ALWAYS);
            VBox refinePanel = UiComponents.surface(refineContent);
            classDiagram.setId("model-statement-class-diagram-view");
            diagramScroll.setId("model-statement-class-diagram-scroll");
            diagramScroll.setPannable(true);
            diagramScroll.setFitToWidth(false);
            diagramScroll.setFitToHeight(false);
            diagramScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            diagramScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            diagramScroll.prefViewportHeightProperty().bind(classDiagram.prefHeightProperty());
            diagramScroll.setMinHeight(Region.USE_PREF_SIZE);
            diagramScroll.setMaxHeight(Region.USE_PREF_SIZE);
            VBox diagramContent = new VBox(14,
                UiComponents.panelHeading("Diagrama de clases UML",
                    "La clase gestora se sitúa en el centro; las relaciones enlazan las clases del modelo."),
                diagramScroll, diagramStatus);
            VBox.setVgrow(diagramScroll, Priority.ALWAYS);
            VBox.setVgrow(diagramContent, Priority.ALWAYS);
            VBox diagramPanel = UiComponents.surface(diagramContent);
        panels = List.of(
                statementPanel,
            promptPanel,
            llmPanel,
                generationPanel,
                refinePanel,
                diagramPanel);

        steps = List.of(
                UiComponents.stepButton(0, "SELECCIONAR TEXTO", "Enunciado filtrado", this::showStep),
                UiComponents.stepButton(1, "SELECCIONAR PROMPT", "Prompt de análisis", this::showStep),
                UiComponents.stepButton(2, "SELECCIONAR LLM", "Modelo de lenguaje", this::showStep),
                UiComponents.stepButton(3, "OBTENER MODELO", "Generación del modelo", this::showStep),
                UiComponents.stepButton(4, "AFINAR MODELO", "Edición del modelo", this::showStep),
                UiComponents.stepButton(5, "DIAGRAMA DE CLASES", "Representación visual", this::showStep));
        HBox flow = new HBox(8,
                steps.get(0), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(1), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(2), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(3), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(4), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(5));
        flow.getStyleClass().add("statement-flow");
        flow.getStyleClass().add("model-statement-flow");
        flow.setMaxHeight(Region.USE_PREF_SIZE);
        flow.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane panelHost = new StackPane();
        panelHost.getChildren().addAll(panels);
        VBox.setVgrow(panelHost, Priority.ALWAYS);
        getChildren().addAll(flow, panelHost);
        for (int i = 1; i < steps.size(); i++) {
            steps.get(i).setDisable(true);
        }
        providerSelector.setOnAction(event -> refreshModels());
        modelSelector.valueProperty().addListener((observable, previous, selected) -> updateModelSelection());
        if (providerSelector.getItems().isEmpty()) {
            showStatus(modelStatus, "No hay clientes LLM registrados.", true);
        }
        showStep(0);
    }

    private static VBox placeholderPanel(String title, String description) {
        return UiComponents.surface(new VBox(14,
                UiComponents.panelHeading(title, description)));
    }

    private static Label fieldTitle(String text, String id) {
        Label title = UiComponents.label(text, List.of("model-llm-field-title"));
        title.setId(id);
        return title;
    }

    private static Label requestValue(String id) {
        Label value = UiComponents.label("Sin seleccionar", List.of("model-request-field-value"));
        value.setId(id);
        value.setWrapText(true);
        return value;
    }

    private static HBox requestRow(String title, Label value) {
        Label fieldTitle = UiComponents.label(title, List.of("model-request-field-title"));
        fieldTitle.setMinWidth(100);
        return new HBox(10, fieldTitle, value);
    }

    private static <T> void styleSelector(ComboBox<T> selector) {
        selector.getStyleClass().add("model-llm-selector");
        selector.setCellFactory(list -> new ListCell<>() {
            {
                getStyleClass().add("model-llm-selector-cell");
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : selector.getConverter().toString(item));
            }
        });
    }

    private static File chooseTextFile(Window owner, String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos de texto", "*.txt", "*.TXT"));
        return chooser.showOpenDialog(owner);
    }

    private static Path defaultOutputDirectory() {
        Path directory = Path.of("").toAbsolutePath().normalize();
        while (directory != null) {
            if (Files.isRegularFile(directory.resolve("modules/statement-analyzer/pom.xml"))) {
                return directory.resolve("modules/statement-analyzer/output");
            }
            directory = directory.getParent();
        }
        return Path.of("output");
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
        if (index == 5) {
            Platform.runLater(() -> {
                diagramScroll.applyCss();
                diagramScroll.layout();
                diagramScroll.setHvalue(diagramScroll.getHmin());
                diagramScroll.setVvalue(diagramScroll.getVmin());
            });
        }
    }

    private void selectTextFile() {
        File file = chooseTextFile.apply(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            statementText.setText(text);
            statementText.positionCaret(0);
            statementText.setVisible(true);
            statementText.setManaged(true);
            selectedTextName.setText(file.getName());
            selectedFile.setText(file.getAbsolutePath());
            selectedTextCard.pseudoClassStateChanged(SELECTED, true);
            updateRequestSummary();
            invalidateGeneratedModel();
            showStatus(status, "Texto del enunciado cargado correctamente. "
                    + "Puedes continuar con el paso 2: Seleccionar prompt.", false);
            steps.get(0).pseudoClassStateChanged(COMPLETED, true);
            steps.get(1).setDisable(false);
        } catch (IOException | SecurityException exception) {
            logger.error("MODEL_STATEMENT_TEXT_READ_ERROR | No se pudo leer el texto del enunciado.", exception);
            showStatus(status, "No se pudo leer el fichero seleccionado. Comprueba que sea un archivo de texto UTF-8.",
                    true);
        }
    }

    private void selectPromptFile() {
        File file = choosePromptFile.apply(getScene() == null ? null : getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            promptText.setText(text);
            promptText.positionCaret(0);
            promptText.setVisible(true);
            promptText.setManaged(true);
            selectedPromptName.setText(file.getName());
            selectedPromptFile.setText(file.getAbsolutePath());
            selectedPromptCard.pseudoClassStateChanged(SELECTED, true);
            updateRequestSummary();
            invalidateGeneratedModel();
            showStatus(promptStatus, "Prompt cargado correctamente. "
                    + "Puedes continuar con el paso 3: Seleccionar LLM.", false);
            steps.get(1).pseudoClassStateChanged(COMPLETED, true);
            steps.get(2).setDisable(false);
        } catch (IOException | SecurityException exception) {
            logger.error("MODEL_STATEMENT_PROMPT_READ_ERROR | No se pudo leer el prompt seleccionado.", exception);
            showStatus(promptStatus, "No se pudo leer el fichero seleccionado. Comprueba que sea un archivo de texto UTF-8.",
                    true);
        }
    }

    private void refreshModels() {
        ModelClientProvider provider = providerSelector.getValue();
        if (provider == null) {
            return;
        }
        selectedClient = null;
        updateRequestSummary();
        invalidateGeneratedModel();
        modelSelector.getItems().clear();
        modelSelector.setValue(null);
        modelSelector.setDisable(true);
        steps.get(3).setDisable(true);
        steps.get(2).pseudoClassStateChanged(COMPLETED, false);
        providerSelector.setDisable(true);
        refreshModelsButton.setDisable(true);
        showStatus(modelStatus, "Consultando los modelos disponibles en " + provider.displayName() + "...", false);
        runTask(() -> {
            ModelClient client = provider.createClient();
            return new AvailableModels(client, client.listModels());
        }, available -> {
            selectedClient = available.client();
            modelSelector.setItems(FXCollections.observableArrayList(available.models()));
            modelSelector.setDisable(available.models().isEmpty());
            if (available.models().isEmpty()) {
                showStatus(modelStatus, "El cliente no tiene modelos disponibles.", false);
            } else {
                showStatus(modelStatus, "Se encontraron " + available.models().size()
                        + " modelos. Selecciona uno.", false);
            }
        }, failure -> {
            selectedClient = null;
            String detail = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            showStatus(modelStatus, "No se pudieron consultar los modelos: " + detail, true);
        }, () -> {
            providerSelector.setDisable(false);
            refreshModelsButton.setDisable(providerSelector.getValue() == null);
        });
    }

    private void updateModelSelection() {
        ModelClientProvider provider = providerSelector.getValue();
        ModelInfo model = modelSelector.getValue();
        boolean selected = provider != null
            && selectedClient != null
            && model != null;
        steps.get(3).setDisable(!selected);
        steps.get(2).pseudoClassStateChanged(COMPLETED, selected);
        updateRequestSummary();
        invalidateGeneratedModel();
        generateModelButton.setDisable(!selected || generationBusy);
        if (selected) {
            showStatus(modelStatus, "Modelo de lenguaje seleccionado. "
                    + "Puedes continuar con el paso 4: Obtener modelo.\nCliente: " + provider.displayName()
                    + " | Modelo: " + model.displayName() + ".", false);
        }
    }

    private void updateRequestSummary() {
        ModelClientProvider provider = providerSelector.getValue();
        ModelInfo model = modelSelector.getValue();
        requestProvider.setText(provider == null ? "Sin seleccionar" : provider.displayName());
        requestModel.setText(model == null ? "Sin seleccionar" : model.displayName());
        requestPrompt.setText(selectedPromptCard.getPseudoClassStates().contains(SELECTED)
                ? selectedPromptName.getText() + "\n" + selectedPromptFile.getText() : "Sin seleccionar");
        requestStatement.setText(selectedTextCard.getPseudoClassStates().contains(SELECTED)
                ? selectedTextName.getText() + "\n" + selectedFile.getText() : "Sin seleccionar");
    }

    private void invalidateGeneratedModel() {
        steps.get(4).setDisable(true);
        steps.get(5).setDisable(true);
        steps.get(3).pseudoClassStateChanged(COMPLETED, false);
        steps.get(4).pseudoClassStateChanged(COMPLETED, false);
        modelEditor = null;
        generatedModelPath = null;
        editorHost.getChildren().clear();
        showStatus(refineStatus, "", false);
        generatedModel.clear();
        generatedModel.setVisible(false);
        generatedModel.setManaged(false);
        classDiagram.setModel(null);
        showStatus(diagramStatus, "", false);
        generationOutputPath.setText("");
        showStatus(generationStatus, "", false);
    }

    private void generateModel() {
        ModelClientProvider provider = providerSelector.getValue();
        ModelInfo model = modelSelector.getValue();
        if (provider == null || selectedClient == null || model == null) {
            return;
        }
        String prompt = promptText.getText();
        String statement = statementText.getText();
        if (prompt.isBlank() || statement.isBlank()) {
            showStatus(generationStatus,
                    "El prompt y el enunciado deben contener texto antes de iniciar el análisis.", true);
            return;
        }

        invalidateGeneratedModel();
        generationBusy = true;
        updateGenerationControls();
        showStatus(generationStatus, "Analizando enunciado con " + provider.id()
                + " (" + model.id() + ")...", false);
        ModelClient client = selectedClient;
        Path resultPath = outputPath(outputFolder, client.provider(), model.id(), LocalDateTime.now());
        runTask(() -> {
            AnalysisResult result = new StatementAnalyzer(client)
                    .analyze(model.id(), prompt, statement, GenerationOptions.defaults());
            String prettyJson = result.prettyJson();
            ProjectModel projectModel;
            try {
                projectModel = ModelIO.fromJson(prettyJson);
            } catch (IOException exception) {
                throw new InvalidModelOutputException(exception.getMessage(), result.response(), exception);
            }
            result.writeTo(resultPath);
                return new GeneratedModel(result, projectModel, prettyJson,
                    resultPath.toAbsolutePath().normalize());
        }, result -> {
            generatedModel.setText(result.prettyJson());
            generatedModel.positionCaret(0);
            generatedModel.setVisible(true);
            generatedModel.setManaged(true);
            generationOutputPath.setText("Resultado: " + result.outputPath());
            showStatus(generationStatus, successSummary(result.analysis().response()), false);
            steps.get(3).pseudoClassStateChanged(COMPLETED, true);
            openModelEditor(result.outputPath());
        }, failure -> {
            logger.error("MODEL_STATEMENT_GENERATION_ERROR | No se pudo generar el modelo.", failure);
            if (failure instanceof InvalidModelOutputException invalidOutput) {
                generatedModel.setText(invalidOutput.rawText());
                generatedModel.positionCaret(0);
                generatedModel.setVisible(true);
                generatedModel.setManaged(true);
            }
            String detail = failure.getMessage() == null
                    ? failure.getClass().getSimpleName() : failure.getMessage();
            showStatus(generationStatus, "No se pudo obtener un JSON válido: " + detail, true);
        }, () -> {
            generationBusy = false;
            updateGenerationControls();
        });
    }

    private void updateGenerationControls() {
        boolean hasModel = providerSelector.getValue() != null
                && selectedClient != null && modelSelector.getValue() != null;
        providerSelector.setDisable(generationBusy);
        modelSelector.setDisable(generationBusy || modelSelector.getItems().isEmpty());
        refreshModelsButton.setDisable(generationBusy || providerSelector.getValue() == null);
        generateModelButton.setDisable(generationBusy || !hasModel);
        steps.get(0).setDisable(generationBusy);
        steps.get(1).setDisable(generationBusy || !selectedTextCard.getPseudoClassStates().contains(SELECTED));
        steps.get(2).setDisable(generationBusy || !selectedPromptCard.getPseudoClassStates().contains(SELECTED));
        steps.get(3).setDisable(generationBusy || !hasModel);
    }

    private String successSummary(com.crud2gui.statementanalyzer.model.ModelResponse response) {
        double seconds = response.elapsed().toMillis() / 1000.0;
        return "Análisis completado. Puedes continuar con el paso 5: Afinar modelo.\nModelo: "
                + response.provider() + "/" + response.model()
                + " | " + String.format(Locale.getDefault(), "%.1f", seconds) + " s"
                + " | tokens entrada/salida: " + tokenCount(response.inputTokens())
                + "/" + tokenCount(response.outputTokens())
                + " | fin: " + (response.finishReason() == null ? "desconocido" : response.finishReason());
    }

    private void openModelEditor(Path modelPath) {
        try {
            modelEditor = new JsonEditorView(modelPath);
        } catch (IOException | RuntimeException exception) {
            logger.error("MODEL_STATEMENT_EDITOR_ERROR | No se pudo abrir el modelo en el editor.", exception);
            showStatus(refineStatus, "No se pudo abrir el modelo en el editor: " + exception.getMessage(), true);
            return;
        }
        modelEditor.setSaveVisible(false);
        generatedModelPath = modelPath;
        editorHost.getChildren().setAll(modelEditor.getView());
        showStatus(refineStatus, "Edita el modelo si es necesario y pulsa «Confirmar modelo».", false);
        steps.get(4).setDisable(false);
    }

    private void confirmModel() {
        if (modelEditor == null || generatedModelPath == null) {
            return;
        }
        Path validatedPath = validatedPath(generatedModelPath);
        try {
            String json = modelEditor.currentJson();
            ProjectModel projectModel = ModelIO.fromJson(json);
            Files.writeString(validatedPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
            classDiagram.setModel(projectModel);
        } catch (IOException | RuntimeException exception) {
            logger.error("MODEL_STATEMENT_CONFIRM_ERROR | No se pudo confirmar el modelo.", exception);
            String detail = exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage();
            showStatus(refineStatus, "El modelo no es válido: " + detail, true);
            steps.get(4).pseudoClassStateChanged(COMPLETED, false);
            steps.get(5).setDisable(true);
            return;
        }
        showStatus(diagramStatus, "Diagrama de clases generado correctamente. Flujo completado.", false);
        showStatus(refineStatus, "Modelo confirmado y guardado en: " + validatedPath
                + "\nPuedes continuar con el paso 6: Diagrama de clases.", false);
        steps.get(4).pseudoClassStateChanged(COMPLETED, true);
        steps.get(5).setDisable(false);
    }

    static Path validatedPath(Path modelPath) {
        String name = modelPath.getFileName().toString();
        int extension = name.lastIndexOf('.');
        String validated = extension > 0
                ? name.substring(0, extension) + "-validated" + name.substring(extension)
                : name + "-validated.json";
        return modelPath.resolveSibling(validated);
    }

    private static String tokenCount(Integer count) {
        return count == null ? "N/D" : count.toString();
    }

    private Path outputPath(Path folder, String provider, String model, LocalDateTime timestamp) {
        String filename = AnalysisResult.timestampedFileNameFor(provider, model, timestamp);
        String modelDirectory = AnalysisResult.fileNameFor(provider, model);
        modelDirectory = modelDirectory.substring(0, modelDirectory.lastIndexOf('.'));
        Path output = folder.resolve(modelDirectory).resolve(filename);
        int collision = 1;
        while (Files.exists(output)) {
            int extension = filename.lastIndexOf('.');
            output = folder.resolve(modelDirectory).resolve(
                    filename.substring(0, extension) + "_" + collision++ + filename.substring(extension));
        }
        return output;
    }

    private void changeOutputFolder() {
        File folder = chooseOutputFolder.apply(getScene() == null ? null : getScene().getWindow(), outputFolder);
        if (folder != null) {
            outputFolder = folder.toPath().toAbsolutePath().normalize();
            updateOutputFolderLabel();
            showStatus(generationStatus, "Carpeta de salida actualizada. Los resultados anteriores no se han movido.",
                    false);
        }
    }

    private void openOutputFolder() {
        Path folder = outputFolder;
        runTask(() -> {
            if (!Files.isDirectory(folder)) {
                throw new IOException("La carpeta de salida no existe: " + folder);
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("El sistema no permite abrir carpetas desde la aplicación.");
            }
            Desktop.getDesktop().open(folder.toFile());
            return folder;
        }, opened -> { },
                failure -> showStatus(generationStatus, "No se pudo abrir la carpeta de salida: " + folder, true),
                () -> { });
    }

    private void updateOutputFolderLabel() {
        outputFolderLabel.setText(outputFolder.toString());
    }

    private record GeneratedModel(AnalysisResult analysis, ProjectModel projectModel,
                                  String prettyJson, Path outputPath) { }

    private record AvailableModels(ModelClient client, List<ModelInfo> models) { }
}

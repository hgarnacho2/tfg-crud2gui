package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.modelvalidator.ModelComparisonMain;
import com.crud2gui.modelvalidator.ValidationOptions;
import com.crud2gui.desktop.workflow.WorkflowContext;
import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.FileDialogs;
import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.ui.ValidationOptionsView;
import com.crud2gui.desktop.workflow.ValidationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ModelValidationSection extends Section {

    private final List<Button> steps;
    private final List<VBox> panels;
    private final WorkflowContext workflow;
    private ModelJsonSelection sourceSelection;
    private ModelJsonSelection inferredSelection;
        private final BiFunction<Window, Path, File> chooseOutputFolder;
        private Path outputFolder;
        private Path actualModelFile;
        private Path expectedModelFile;
        private final Label outputName = UiComponents.labelWithGraphic("Informe JSON",
            List.of("statement-file-name"), UiComponents.label("JSON", List.of("statement-file-badge")), 12);
        private final Label outputPath = UiComponents.label("", List.of("statement-file-path"));
        private final Button changeFolder = UiComponents.actionButton("Cambiar carpeta de salida",
            List.of("statement-change-folder", "secondary-button"), this::changeOutputFolder);
        private final Button openFolder = UiComponents.actionButton("Abrir carpeta de salida",
            List.of("statement-open-folder", "secondary-button"), this::openOutputFolder);
        private final Button validateModels = UiComponents.actionButton("Validar modelos",
            List.of("statement-select"), () -> compareModels(actualModelFile, expectedModelFile));
    private final Label validationStatus = UiComponents.statusLabel("model-validation-status");
    private final StackPane resultHost = new StackPane();
    private final ValidationOptionsView optionsView;

    public ModelValidationSection() {
        this((WorkflowContext) null);
    }

    public ModelValidationSection(WorkflowContext workflow) {
        this(ModelJsonSelection::chooseJson, workflow);
    }

    public ModelValidationSection(String breadcrumb, WorkflowContext workflow) {
        this(breadcrumb, ModelJsonSelection::chooseJson, ModelValidationSection::defaultReportOutputDirectory,
                (owner, current) -> FileDialogs.chooseDirectory(
                        owner, current, "Carpeta de salida de los informes de validación"), workflow);
    }

    private ModelValidationSection(Function<Window, File> chooseFile, WorkflowContext workflow) {
        this(chooseFile, ModelValidationSection::defaultReportOutputDirectory, workflow);
    }

    private ModelValidationSection(Function<Window, File> chooseFile, Supplier<Path> reportOutputDirectory,
                                  WorkflowContext workflow) {
        this(chooseFile, reportOutputDirectory, (owner, current) -> FileDialogs.chooseDirectory(
                owner, current, "Carpeta de salida de los informes de validación"), workflow);
    }

    ModelValidationSection(Function<Window, File> chooseFile) {
        this(chooseFile, ModelValidationSection::defaultReportOutputDirectory, (WorkflowContext) null);
    }

    ModelValidationSection(Function<Window, File> chooseFile, Supplier<Path> reportOutputDirectory) {
        this(chooseFile, reportOutputDirectory, (owner, current) -> FileDialogs.chooseDirectory(
            owner, current, "Carpeta de salida de los informes de validación"), null);
        }

        ModelValidationSection(Function<Window, File> chooseFile, Supplier<Path> reportOutputDirectory,
            BiFunction<Window, Path, File> chooseOutputFolder) {
        this(chooseFile, reportOutputDirectory, chooseOutputFolder, null);
        }

        private ModelValidationSection(Function<Window, File> chooseFile, Supplier<Path> reportOutputDirectory,
            BiFunction<Window, Path, File> chooseOutputFolder, WorkflowContext workflow) {
            this("VALIDAR MODELOS", chooseFile, reportOutputDirectory, chooseOutputFolder, workflow);
        }

        private ModelValidationSection(String breadcrumb, Function<Window, File> chooseFile,
                                      Supplier<Path> reportOutputDirectory,
                                      BiFunction<Window, Path, File> chooseOutputFolder,
                                      WorkflowContext workflow) {
            super(breadcrumb, "Validar modelos",
            "Selecciona los modelos JSON de código y enunciado para preparar su validación.");
        Objects.requireNonNull(chooseFile, "chooseFile");
        this.workflow = workflow;
        outputFolder = Objects.requireNonNull(reportOutputDirectory.get(), "reportOutputDirectory")
            .toAbsolutePath().normalize();
        this.chooseOutputFolder = Objects.requireNonNull(chooseOutputFolder, "chooseOutputFolder");
        optionsView = new ValidationOptionsView(loadInitialOptions());
        outputPath.setText(outputFolder.toString());
        outputPath.setId("model-validation-output-path");
        outputName.setId("model-validation-output-name");
        changeFolder.setId("model-validation-change-output-folder");
        openFolder.setId("model-validation-open-output-folder");
        validateModels.setId("model-validation-run");
        validateModels.setDisable(true);
        steps = List.of(
                UiComponents.stepButton(0, "MODELO FUENTE JAVA", "Modelo extra\u00eddo del c\u00f3digo", this::showStep),
                UiComponents.stepButton(1, "MODELO INFERIDO ENUNCIADO", "Modelo del enunciado", this::showStep),
                UiComponents.stepButton(2, "RESULTADO DE LA VALIDACI\u00d3N", "Resultado", this::showStep));
        steps.get(0).setId("model-validation-step-java");
        steps.get(1).setId("model-validation-step-statement");
        steps.get(2).setId("model-validation-step-result");
        steps.get(1).setDisable(true);
        steps.get(2).setDisable(true);

        sourceSelection = new ModelJsonSelection("model-validation-java",
                "Modelo fuente Java", "JSON extra\u00eddo de los fuentes Java.", selected -> {
                    actualModelFile = selected;
                    if (workflow != null) {
                        workflow.selectJavaModel(selected);
                    }
                    invalidateResult();
                    expectedModelFile = inferredSelection.getSelectedFile();
                    updateSelectionState();
            }, chooseFile,
            "Modelo cargado correctamente. Puedes continuar con el paso 2: Modelo inferido enunciado.");
        inferredSelection = new ModelJsonSelection("model-validation-statement",
                "Modelo inferido enunciado", "JSON del modelo inferido a partir del enunciado.",
            selected -> {
                invalidateResult();
                actualModelFile = sourceSelection.getSelectedFile();
                expectedModelFile = selected;
                if (workflow != null) {
                    workflow.selectInferredModel(selected);
                }
                updateSelectionState();
            }, chooseFile,
            "Modelo inferido cargado correctamente. Puedes continuar con el paso 3: Resultado de la validación.");
        Label resultTitle = UiComponents.label("Resultado de la validación", List.of("validation-block-title"));
        resultTitle.setId("model-validation-result-title");
        VBox resultGroup = new VBox(14, resultTitle, resultHost);
        resultGroup.visibleProperty().bind(Bindings.isNotEmpty(resultHost.getChildren()));
        resultGroup.managedProperty().bind(resultGroup.visibleProperty());
        VBox result = UiComponents.vBox(List.of("model-validation-result-panel"),
            UiComponents.label("Configuración del validador", List.of("validation-block-title")), optionsView,
            UiComponents.label("Salida", List.of("validation-block-title")),
                UiComponents.outputSettings(outputName, outputPath, changeFolder, openFolder),
            validateModels, validationStatus, resultGroup);
        VBox sourcePanel = UiComponents.surface(sourceSelection);
        VBox inferredPanel = UiComponents.surface(inferredSelection);
        VBox.setVgrow(sourceSelection, Priority.ALWAYS);
        VBox.setVgrow(inferredSelection, Priority.ALWAYS);
        sourceSelection.setMaxHeight(Double.MAX_VALUE);
        inferredSelection.setMaxHeight(Double.MAX_VALUE);
        panels = List.of(sourcePanel, inferredPanel, UiComponents.surface(result));
        for (VBox panel : panels) {
            panel.setMaxHeight(Double.MAX_VALUE);
            StackPane.setAlignment(panel, javafx.geometry.Pos.TOP_LEFT);
        }
        HBox flow = new HBox(8,
                steps.get(0), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(1), UiComponents.arrowConnector(UiComponents.Direction.RIGHT), steps.get(2));
        flow.getStyleClass().addAll("statement-flow", "model-statement-flow");
        flow.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane panelHost = new StackPane();
        panelHost.getChildren().addAll(panels);
        VBox.setVgrow(panelHost, Priority.ALWAYS);
        getChildren().addAll(flow, panelHost);
        showStep(0);
        if (workflow != null) {
            sourceSelection.setRestoredFile(workflow.getSelectedJavaModel(), workflow.isJavaModelManual());
            inferredSelection.setRestoredFile(workflow.getSelectedInferredModel(),
                    workflow.isInferredModelManual());
            refreshModelSelections();
            restoreValidationReport();
        }
    }

    private void restoreValidationReport() {
        Path reportFile = workflow.getValidationReport();
        if (reportFile == null || actualModelFile == null || expectedModelFile == null) {
            return;
        }
        try {
            var report = new ObjectMapper().readTree(reportFile.toFile());
            if (report == null || !report.isObject()) {
                throw new IOException("El informe guardado no contiene un objeto JSON.");
            }
            outputName.setText(reportFile.getFileName().toString());
            outputPath.setText(reportFile.toAbsolutePath().normalize().getParent().toString());
            resultHost.getChildren().setAll(new ModelValidationResultView(report, reportFile));
            workflow.recordValidationReport(reportFile, statusFromReport(report));
            steps.get(2).setDisable(false);
            steps.get(2).pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("completed"), true);
            showStatus(validationStatus, "Informe de validación restaurado.", false);
            showStep(2);
        } catch (IOException | RuntimeException exception) {
            workflow.recordValidationReport(reportFile, ValidationStatus.UNKNOWN);
            showStatus(validationStatus, "No se pudo restaurar el informe guardado. "
                    + "Puedes volver a ejecutar la validación.", true);
        }
    }

    public void refreshModelSelections() {
        if (workflow == null) {
            return;
        }
        sourceSelection.setSuggestedFile(workflow.getSelectedJavaModel());
        inferredSelection.setSuggestedFile(workflow.getSelectedInferredModel());
        Path selectedActual = sourceSelection.getSelectedFile();
        Path selectedExpected = inferredSelection.getSelectedFile();
        if (!Objects.equals(actualModelFile, selectedActual)
                || !Objects.equals(expectedModelFile, selectedExpected)) {
            invalidateResult();
        }
        actualModelFile = selectedActual;
        expectedModelFile = selectedExpected;
        updateSelectionState();
    }

    private void updateSelectionState() {
        steps.get(0).pseudoClassStateChanged(COMPLETED, actualModelFile != null);
        steps.get(1).setDisable(actualModelFile == null);
        steps.get(1).pseudoClassStateChanged(COMPLETED, expectedModelFile != null);
        boolean canValidate = actualModelFile != null && expectedModelFile != null;
        steps.get(2).setDisable(!canValidate);
        validateModels.setDisable(!canValidate);
    }

    private void compareModels(Path actualModel, Path expectedModel) {
        actualModelFile = actualModel;
        expectedModelFile = expectedModel;
        Path destination = outputFolder;
        ValidationOptions options = optionsView.getOptions();
        setBusy(true);
        steps.get(2).setDisable(true);
        steps.get(2).pseudoClassStateChanged(COMPLETED, false);
        resultHost.getChildren().clear();
        if (workflow != null) {
            workflow.recordValidationReport(null, ValidationStatus.NOT_VALIDATED);
        }
        showStatus(validationStatus, "Comparando los modelos y generando el informe...", false);
        runTask(() -> {
            Path reportFile = validationReportPath(actualModel, expectedModel, destination);
            ObjectNode report = ModelComparisonMain.compareFiles(expectedModel, actualModel,
                    options, reportFile);
            return new ValidationOutput(report, reportFile);
        }, output -> {
            outputName.setText(output.reportFile().getFileName().toString());
            outputPath.setText(output.reportFile().toAbsolutePath().normalize().getParent().toString());
            resultHost.getChildren().setAll(new ModelValidationResultView(output.report(), output.reportFile()));
            if (workflow != null) {
                workflow.recordValidationReport(output.reportFile(), statusFromReport(output.report()));
            }
            showStatus(validationStatus, "", false);
            steps.get(2).setDisable(false);
            steps.get(2).pseudoClassStateChanged(COMPLETED, true);
            showStep(2);
        }, failure -> {
            Label error = UiComponents.label("No se pudo completar la validación: "
                    + Objects.toString(failure.getMessage(), "Error inesperado."),
                    List.of("model-validation-error"));
            error.setWrapText(true);
            resultHost.getChildren().setAll(error);
            showStatus(validationStatus, "La validación no pudo completarse.", true);
            steps.get(2).setDisable(false);
            showStep(2);
        }, () -> setBusy(false));
    }

    private static ValidationStatus statusFromReport(JsonNode report) {
        return report.has("valid")
                ? ValidationStatus.fromReport(report.path("valid").asBoolean(),
                        report.path("readyForCrud").asBoolean())
                : ValidationStatus.UNKNOWN;
    }

    private Path validationReportPath(Path actualModel, Path expectedModel, Path destination) {
        String fileName = modelBaseName(actualModel) + "__" + modelBaseName(expectedModel) + ".json";
        return uniqueOutputPath(destination.resolve(fileName));
    }

    private static String modelBaseName(Path modelFile) {
        String fileName = modelFile.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
    }

    private void setBusy(boolean busy) {
        optionsView.setDisable(busy);
        changeFolder.setDisable(busy);
        openFolder.setDisable(busy);
        validateModels.setDisable(busy || expectedModelFile == null || actualModelFile == null);
        panels.get(0).setDisable(busy);
        panels.get(1).setDisable(busy);
    }

    private void changeOutputFolder() {
        File folder = chooseOutputFolder.apply(getScene() == null ? null : getScene().getWindow(), outputFolder);
        if (folder != null) {
            outputFolder = folder.toPath().toAbsolutePath().normalize();
            outputName.setText("Informe JSON");
            outputPath.setText(outputFolder.toString());
            showStatus(validationStatus, "Carpeta de salida actualizada. Los informes anteriores no se han movido.",
                    false);
        }
    }

    private void openOutputFolder() {
        Path folder = outputFolder;
        openOutputFolder(folder, () -> setBusy(true),
                failure -> showStatus(validationStatus,
                        "No se pudo abrir la carpeta de salida: " + folder, true),
                () -> setBusy(false));
    }

    private void invalidateResult() {
        validateModels.setDisable(true);
        steps.get(2).setDisable(true);
        steps.get(2).pseudoClassStateChanged(COMPLETED, false);
        resultHost.getChildren().clear();
        showStatus(validationStatus, "", false);
    }

    private static Path defaultReportOutputDirectory() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path module = current.resolve("modules/model-validator");
            if (Files.isDirectory(module)) {
                return module.resolve("output");
            }
            current = current.getParent();
        }
        return Path.of("output").toAbsolutePath().normalize();
    }

    private ValidationOptions loadInitialOptions() {
        Path config = defaultReportOutputDirectory().resolveSibling("conf").resolve("validation-options.json");
        try {
            return Files.isRegularFile(config)
                    ? ValidationOptions.fromJson(new ObjectMapper().readTree(config.toFile()))
                    : ValidationOptions.loadDefault();
        } catch (IOException | IllegalArgumentException exception) {
            logger.warn("VALIDATION_CONFIG_ERROR | No se pudo cargar la configuración del validador.", exception);
            showStatus(validationStatus, "No se pudo cargar la configuración inicial. Todas las comprobaciones están activadas.",
                    true);
            return ValidationOptions.defaults();
        }
    }

    private record ValidationOutput(ObjectNode report, Path reportFile) { }

    private void showStep(int index) {
        if (steps.get(index).isDisabled()) {
            return;
        }
        showStepPanels(panels, steps, index);
    }
}
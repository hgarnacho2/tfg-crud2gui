package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherWorkflowContext;
import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Vista independiente de los ficheros asociados al contexto de trabajo docente. */
public final class TeacherContextSection extends Section {

    private final TeacherWorkflowContext workflow;
    private final Pane connectorLayer = new Pane();
    private final VBox flowContent = new VBox();
    private final Map<String, VBox> stages = new LinkedHashMap<>();
    private final StackPane diagram = new StackPane() {
        @Override
        protected void layoutChildren() {
            super.layoutChildren();
            layoutTree(flowContent);
            drawConnectors();
        }
    };

    public TeacherContextSection(TeacherWorkflowContext workflow) {
        super("CONTEXTO", "Contexto", "Ficheros asociados a las etapas del trabajo docente.");
        this.workflow = Objects.requireNonNull(workflow, "workflow");
        diagram.setId("teacher-context-diagram");
        diagram.getStyleClass().add("workflow-context-diagram");
        connectorLayer.setId("teacher-context-connectors");
        connectorLayer.setMouseTransparent(true);
        flowContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        diagram.getChildren().setAll(connectorLayer, flowContent);
        VBox.setVgrow(diagram, Priority.ALWAYS);
        getChildren().add(diagram);
        refresh();
    }

    /** Actualiza los nombres de archivo cuando se vuelve a abrir esta vista. */
    public void refresh() {
        stages.clear();
        VBox statement = lane("MODELO DE REFERENCIA", "Modelo de datos válido para la validación");
        addStage(statement, "statement-pdf", "Enunciado original", "PDF", workflow.getStatementPdf(),
            "Todavía no se ha seleccionado un PDF.");
        addStage(statement, "statement-extracted", "Texto del enunciado", "TXT", workflow.getExtractedText(),
            "Todavía no se ha extraído el texto.");
        addStage(statement, "statement-filtered", "Texto filtrado", "TXT", workflow.getFilteredText(),
            "Todavía no se ha generado el texto filtrado.");
        VBox inferredModel = stage("statement-model", "Modelo inferido del enunciado", "JSON",
            workflow.getGeneratedStatementModel(), "Todavía no se ha generado el modelo.", null);
        inferredModel.getChildren().add(promptInput(workflow.getSelectedPromptFile()));
        statement.getChildren().add(inferredModel);
        addStage(statement, "statement-refined-model", "Modelo revisado del enunciado", "JSON",
            workflow.getSuggestedInferredModel(), "Todavía no se ha confirmado el modelo revisado.");

        VBox code = lane("CÓDIGO FUENTE", "Fuentes Java y modelo generado");
        addStage(code, "code-folder", "Clases de código Java", "CARPETA", workflow.getSelectedCodeFolder(),
            "Todavía no se ha seleccionado una carpeta.");
        addStage(code, "code-model", "Modelo obtenido del código", "JSON", workflow.getSuggestedJavaModel(),
            "Todavía no se ha generado el modelo.");

        VBox validation = stage("validation-report", "Validación de modelos", "INFORME JSON",
            workflow.getValidationReport(), "Todavía no se ha generado el informe.", null);
        HBox validationHeader = (HBox) validation.getChildren().get(0);
        validationHeader.setMaxWidth(Double.MAX_VALUE);
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        validationHeader.getChildren().addAll(headerSpacer, UiComponents.validationStatusBadge(
            workflow.getValidationStatus(), "teacher-context-validation-status"));
        validation.getChildren().add(validationInputs());
        VBox crud = stage("crud", "CRUD", "JAVA", null,
                "Los ficheros asociados se incorporarán más adelante.", null);

        GridPane branches = new GridPane();
        branches.setHgap(20);
        branches.getStyleClass().add("workflow-context-branches");
        ColumnConstraints statementColumn = new ColumnConstraints();
        statementColumn.setPercentWidth(50);
        statementColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints codeColumn = new ColumnConstraints();
        codeColumn.setPercentWidth(50);
        codeColumn.setHgrow(Priority.ALWAYS);
        branches.getColumnConstraints().setAll(statementColumn, codeColumn);
        GridPane.setHgrow(statement, Priority.ALWAYS);
        GridPane.setHgrow(code, Priority.ALWAYS);
        GridPane.setFillWidth(statement, true);
        GridPane.setFillWidth(code, true);
        branches.add(statement, 0, 0);
        branches.add(code, 1, 0);
        statement.setMaxWidth(Double.MAX_VALUE);
        code.setMaxWidth(Double.MAX_VALUE);
        Region mergeArea = new Region();
        mergeArea.setId("teacher-context-merge-area");
        mergeArea.setMinHeight(40);
        mergeArea.setPrefHeight(48);
        VBox.setVgrow(mergeArea, Priority.ALWAYS);
        Region outputGap = new Region();
        outputGap.setId("teacher-context-output-gap");
        outputGap.setMinHeight(24);
        outputGap.setPrefHeight(28);
        flowContent.getChildren().setAll(branches, mergeArea, validation, outputGap, crud);
        StackPane.setAlignment(flowContent, Pos.TOP_LEFT);
        StackPane.setAlignment(connectorLayer, Pos.TOP_LEFT);
        diagram.requestLayout();
    }

    private VBox lane(String title, String description) {
        Label heading = new Label(title);
        heading.getStyleClass().add("workflow-context-lane-title");
        Label subtitle = new Label(description);
        subtitle.getStyleClass().add("workflow-context-lane-description");
        VBox lane = new VBox(22, heading, subtitle);
        lane.getStyleClass().add("workflow-context-lane");
        return lane;
    }

    private VBox validationInputs() {
        VBox inputs = new VBox(6,
            new Label("Entradas para validar"),
            validationInput("CODIGO", "code", workflow.getSelectedJavaModel()),
            validationInput("ENUNCIADO", "statement", workflow.getSelectedInferredModel()));
        inputs.getStyleClass().add("workflow-context-validation-inputs");
        inputs.getChildren().get(0).getStyleClass().add("workflow-context-validation-inputs-title");
        return inputs;
    }

    private HBox promptInput(Path prompt) {
        Label badge = new Label("PROMPT");
        badge.setId("teacher-context-model-prompt-badge");
        badge.getStyleClass().addAll("workflow-context-input-badge", "workflow-context-input-prompt");
        Label fileName = new Label(prompt == null ? "Sin prompt seleccionado" : filename(prompt));
        fileName.setId("teacher-context-model-prompt-file");
        fileName.getStyleClass().add("workflow-context-input-file");
        fileName.setWrapText(true);
        fileName.setMinWidth(0);
        HBox row = new HBox(8, badge, fileName, UiComponents.revealFileButton(
            "teacher-context-model-prompt-reveal", prompt));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox validationInput(String text, String id, Path model) {
        Label badge = new Label(text);
        badge.setId("teacher-context-validation-input-" + id + "-badge");
        badge.getStyleClass().addAll("workflow-context-input-badge", "workflow-context-input-" + id);
        Label fileName = new Label(model == null ? "Sin modelo seleccionado" : filename(model));
        fileName.setId("teacher-context-validation-input-" + id + "-file");
        fileName.getStyleClass().add("workflow-context-input-file");
        fileName.setWrapText(true);
        fileName.setMinWidth(0);
        HBox row = new HBox(8, badge, fileName, UiComponents.revealFileButton(
            "teacher-context-validation-input-" + id + "-reveal", model));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void addStage(VBox lane, String id, String title, String type, Path file, String emptyMessage) {
        lane.getChildren().add(stage(id, title, type, file, emptyMessage, null));
    }

    private VBox stage(String id, String title, String type, Path file, String emptyMessage, String details) {
        Label marker = new Label(type);
        marker.getStyleClass().add("workflow-context-stage-type");
        Label heading = new Label(title);
        heading.getStyleClass().add("workflow-context-stage-title");
        HBox header = new HBox(10, marker, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        Label artifact = new Label(file == null ? emptyMessage : filename(file));
        artifact.setId("teacher-context-stage-" + id + "-file");
        artifact.getStyleClass().add(file == null
            ? "workflow-context-stage-empty" : "workflow-context-stage-file");
        artifact.setWrapText(true);
        Path locationPath = file == null ? null : file.toAbsolutePath().normalize();
        if (locationPath != null && !"CARPETA".equals(type) && locationPath.getParent() != null) {
            locationPath = locationPath.getParent();
        }
        Label location = new Label(locationPath == null ? "" : locationPath.toString());
        location.setId("teacher-context-stage-" + id + "-path");
        location.getStyleClass().add("workflow-context-stage-path");
        location.setWrapText(true);
        location.setVisible(file != null);
        location.setManaged(file != null);

        artifact.setMinWidth(0);
        artifact.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(artifact, Priority.ALWAYS);
        HBox artifactRow = new HBox(8, artifact, UiComponents.revealFileButton(
            "teacher-context-stage-" + id + "-reveal", file));
        artifactRow.setAlignment(Pos.CENTER_LEFT);
        VBox block = new VBox(8, header, artifactRow, location);
        block.setId("teacher-context-stage-" + id);
        block.getStyleClass().add("workflow-context-stage");
        if (file == null) {
            block.getStyleClass().add("workflow-context-stage-pending");
        }
        if (details != null) {
            Label detail = new Label(details);
            detail.getStyleClass().add("workflow-context-stage-details");
            detail.setWrapText(true);
            block.getChildren().add(detail);
        }
        block.setMaxWidth(Double.MAX_VALUE);
        stages.put(id, block);
        return block;
    }

    private void drawConnectors() {
        if (diagram.getScene() == null || stages.isEmpty()) {
            return;
        }
        List<Node> connectors = new ArrayList<>();
        addVerticalArrow(connectors, "statement-pdf", "statement-extracted", "statement-pdf-text");
        addVerticalArrow(connectors, "statement-extracted", "statement-filtered", "statement-text-filtered");
        addVerticalArrow(connectors, "statement-filtered", "statement-model", "statement-filtered-model");
        addVerticalArrow(connectors, "statement-model", "statement-refined-model", "statement-refined-model");
        addVerticalArrow(connectors, "code-folder", "code-model", "code-folder-model");

        Bounds statementModel = bounds(stages.get("statement-refined-model"));
        Bounds codeModel = bounds(stages.get("code-model"));
        Bounds validation = bounds(stages.get("validation-report"));
        Bounds mergeArea = bounds(diagram.lookup("#teacher-context-merge-area"));
        double joinX = validation.getMinX() + validation.getWidth() / 2;
        double joinY = mergeArea.getMinY() + mergeArea.getHeight() / 2;
        connectors.add(branchToMerge("teacher-context-merge-statement", statementModel, joinX, joinY));
        connectors.add(branchToMerge("teacher-context-merge-code", codeModel, joinX, joinY));
        connectors.add(verticalArrow("teacher-context-validation-arrow", joinX, joinY,
                validation.getMinY()));
        Bounds crud = bounds(stages.get("crud"));
        connectors.add(verticalArrow("teacher-context-crud-arrow",
                validation.getMinX() + validation.getWidth() / 2, validation.getMaxY(), crud.getMinY()));

        Circle junction = new Circle(joinX, joinY, 4, Color.web("#087e65"));
        junction.setId("teacher-context-merge-junction");
        junction.setMouseTransparent(true);
        connectors.add(junction);
        connectorLayer.getChildren().setAll(connectors);
    }

    private void addVerticalArrow(List<Node> connectors, String fromId, String toId, String connectorId) {
        Bounds from = bounds(stages.get(fromId));
        Bounds to = bounds(stages.get(toId));
        connectors.add(verticalArrow("teacher-context-arrow-" + connectorId,
                to.getMinX() + to.getWidth() / 2, from.getMaxY(), to.getMinY()));
    }

    private javafx.scene.shape.Path branchToMerge(String id, Bounds source, double joinX, double joinY) {
        double sourceX = source.getMinX() + source.getWidth() / 2;
        javafx.scene.shape.Path path = connectorPath(id,
                new MoveTo(sourceX, source.getMaxY()),
                new LineTo(sourceX, joinY),
                new LineTo(joinX, joinY));
        path.getStyleClass().add("workflow-context-merge-connector");
        return path;
    }

    private javafx.scene.shape.Path verticalArrow(String id, double x, double startY, double endY) {
        double tipY = endY - 1;
        double headLength = 9;
        double headHalfWidth = 6;
        return connectorPath(id,
                new MoveTo(x, startY),
                new LineTo(x, tipY),
                new MoveTo(x - headHalfWidth, tipY - headLength),
                new LineTo(x, tipY),
                new LineTo(x + headHalfWidth, tipY - headLength));
    }

    private javafx.scene.shape.Path connectorPath(String id, PathElement... elements) {
        javafx.scene.shape.Path path = new javafx.scene.shape.Path(elements);
        path.setId(id);
        path.getStyleClass().add("workflow-context-connector");
        path.setFill(Color.TRANSPARENT);
        path.setStroke(Color.web("#087e65"));
        path.setStrokeWidth(2.5);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        path.setMouseTransparent(true);
        return path;
    }

    private Bounds bounds(Node node) {
        return connectorLayer.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
    }

    private void layoutTree(Parent parent) {
        parent.layout();
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Parent nested) {
                layoutTree(nested);
            }
        }
    }

    private static String filename(Path path) {
        if (path == null) {
            return "pendiente";
        }
        Path name = path.getFileName();
        return name == null ? path.toString() : name.toString();
    }
}
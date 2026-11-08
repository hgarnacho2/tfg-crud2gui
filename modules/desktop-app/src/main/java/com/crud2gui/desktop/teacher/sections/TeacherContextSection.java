package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherWorkflowContext;
import com.crud2gui.desktop.ui.Section;
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
        diagram.getStyleClass().add("teacher-context-diagram");
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
        VBox statement = lane("RAMA DEL ENUNCIADO", "Del documento original al modelo inferido");
        addStage(statement, "statement-pdf", "Enunciado original", "PDF", workflow.getStatementPdf(),
            "Todavía no se ha seleccionado un PDF.");
        addStage(statement, "statement-extracted", "Texto del enunciado", "TXT", workflow.getExtractedText(),
            "Todavía no se ha extraído el texto.");
        addStage(statement, "statement-filtered", "Texto filtrado", "TXT", workflow.getFilteredText(),
            "Todavía no se ha generado el texto filtrado.");
        addStage(statement, "statement-model", "Modelo inferido del enunciado", "JSON",
            workflow.getGeneratedStatementModel(), "Todavía no se ha generado el modelo.");
        addStage(statement, "statement-refined-model", "Modelo afinado del enunciado", "JSON",
            workflow.getSuggestedInferredModel(), "Todavía no se ha confirmado el modelo afinado.");

        VBox code = lane("RAMA DEL CÓDIGO", "Fuentes Java y modelo extraído");
        addStage(code, "code-folder", "Clases de código Java", "CARPETA", workflow.getSelectedCodeFolder(),
            "Todavía no se ha seleccionado una carpeta.");
        addStage(code, "code-model", "Modelo obtenido del código", "JSON", workflow.getSuggestedJavaModel(),
            "Todavía no se ha generado el modelo.");

        String validationInputs = "Entradas: código " + filename(workflow.getSelectedJavaModel())
                + " · enunciado " + filename(workflow.getSelectedInferredModel());
        VBox validation = stage("validation-report", "Validación de modelos", "INFORME JSON",
            workflow.getValidationReport(), "Todavía no se ha generado el informe.", validationInputs);
        VBox crud = stage("crud", "CRUD", "JAVA", null,
                "Los ficheros asociados se incorporarán más adelante.", null);

        GridPane branches = new GridPane();
        branches.setHgap(20);
        branches.getStyleClass().add("teacher-context-branches");
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
        heading.getStyleClass().add("teacher-context-lane-title");
        Label subtitle = new Label(description);
        subtitle.getStyleClass().add("teacher-context-lane-description");
        VBox lane = new VBox(22, heading, subtitle);
        lane.getStyleClass().add("teacher-context-lane");
        return lane;
    }

    private void addStage(VBox lane, String id, String title, String type, Path file, String emptyMessage) {
        lane.getChildren().add(stage(id, title, type, file, emptyMessage, null));
    }

    private VBox stage(String id, String title, String type, Path file, String emptyMessage, String details) {
        Label marker = new Label(type);
        marker.getStyleClass().add("teacher-context-stage-type");
        Label heading = new Label(title);
        heading.getStyleClass().add("teacher-context-stage-title");
        HBox header = new HBox(10, marker, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        Label artifact = new Label(file == null ? emptyMessage : filename(file));
        artifact.setId("teacher-context-stage-" + id + "-file");
        artifact.getStyleClass().add(file == null
                ? "teacher-context-stage-empty" : "teacher-context-stage-file");
        artifact.setWrapText(true);
        Label location = new Label(file == null ? "" : file.toAbsolutePath().normalize().toString());
        location.setId("teacher-context-stage-" + id + "-path");
        location.getStyleClass().add("teacher-context-stage-path");
        location.setWrapText(true);
        location.setVisible(file != null);
        location.setManaged(file != null);

        VBox block = new VBox(8, header, artifact, location);
        block.setId("teacher-context-stage-" + id);
        block.getStyleClass().add("teacher-context-stage");
        if (file == null) {
            block.getStyleClass().add("teacher-context-stage-pending");
        }
        if (details != null) {
            Label detail = new Label(details);
            detail.getStyleClass().add("teacher-context-stage-details");
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
        path.getStyleClass().add("teacher-context-merge-connector");
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
        path.getStyleClass().add("teacher-context-connector");
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
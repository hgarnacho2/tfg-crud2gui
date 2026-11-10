package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.student.StudentWorkflowContext;
import com.crud2gui.desktop.ui.Section;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

/** Vista del flujo de modelos asociado al contexto de trabajo del alumno. */
public final class StudentContextSection extends Section {

    private final StudentWorkflowContext workflow;
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

    public StudentContextSection(StudentWorkflowContext workflow) {
        super("Contexto", "Contexto", "Ficheros asociados al flujo de código y validación.");
        this.workflow = Objects.requireNonNull(workflow, "workflow");
        diagram.setId("student-context-diagram");
        diagram.getStyleClass().add("workflow-context-diagram");
        connectorLayer.setId("student-context-connectors");
        connectorLayer.setMouseTransparent(true);
        flowContent.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        diagram.getChildren().setAll(connectorLayer, flowContent);
        VBox.setVgrow(diagram, Priority.ALWAYS);
        getChildren().add(diagram);
        refresh();
    }

    /** Actualiza las rutas visibles con el estado actual del workflow. */
    public void refresh() {
        stages.clear();
        VBox codeLane = lane("CÓDIGO FUENTE", "Fuentes Java y modelo generado");
        Label codeLaneTitle = (Label) codeLane.getChildren().get(0);
        codeLaneTitle.setId("student-context-code-lane-title");
        codeLaneTitle.getStyleClass().add("student-context-code-lane-title");
        Label codeLaneDescription = (Label) codeLane.getChildren().get(1);
        codeLaneDescription.setId("student-context-code-lane-description");
        codeLaneDescription.getStyleClass().add("student-context-lane-description");
        addStage(codeLane, "code-folder", "Clases de código Java", "CARPETA",
                workflow.getSelectedCodeFolder(), "Todavía no se ha seleccionado una carpeta.");
        addStage(codeLane, "code-model", "Modelo de datos obtenido del código", "JSON",
                workflow.getGeneratedCodeModel(), "Todavía no se ha generado el modelo.");
        VBox validation = stage("validation-report", "Validación del modelo", "INFORME JSON",
            workflow.getValidationReport(), "Todavía no se ha generado el informe.", validationInputs());
        VBox crud = stage("crud", "CRUD", "JAVA", null,
            "Los ficheros asociados se incorporarán más adelante.", null);

        VBox referenceLane = lane("MODELO DE REFERENCIA", "Modelo de datos válido para la comparación");
        Label referenceLaneTitle = (Label) referenceLane.getChildren().get(0);
        referenceLaneTitle.getStyleClass().add("student-context-reference-lane-title");
        Label referenceLaneDescription = (Label) referenceLane.getChildren().get(1);
        referenceLaneDescription.setId("student-context-reference-lane-description");
        referenceLaneDescription.getStyleClass().add("student-context-lane-description");
        addStage(referenceLane, "valid-model", "Modelo de datos válido", "JSON",
                workflow.getSelectedInferredModel(), "Todavía no se ha seleccionado un modelo válido.");

        GridPane branches = new GridPane();
        branches.setHgap(20);
        branches.setVgap(20);
        branches.getStyleClass().add("workflow-context-branches");
        ColumnConstraints codeColumn = new ColumnConstraints();
        codeColumn.setPercentWidth(50);
        codeColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints referenceColumn = new ColumnConstraints();
        referenceColumn.setPercentWidth(50);
        referenceColumn.setHgrow(Priority.ALWAYS);
        branches.getColumnConstraints().setAll(codeColumn, referenceColumn);
        GridPane.setHgrow(codeLane, Priority.ALWAYS);
        GridPane.setHgrow(referenceLane, Priority.ALWAYS);
        GridPane.setFillWidth(codeLane, true);
        GridPane.setFillWidth(referenceLane, true);
        branches.add(codeLane, 0, 0);
        branches.add(referenceLane, 1, 0);
        branches.add(validation, 0, 1);
        GridPane.setColumnSpan(validation, 2);
        GridPane.setHgrow(validation, Priority.ALWAYS);
        GridPane.setFillWidth(validation, true);
        branches.add(crud, 0, 2);
        GridPane.setColumnSpan(crud, 2);
        GridPane.setHgrow(crud, Priority.ALWAYS);
        GridPane.setFillWidth(crud, true);
        codeLane.setMaxWidth(Double.MAX_VALUE);
        referenceLane.setMaxWidth(Double.MAX_VALUE);
        validation.setMaxWidth(Double.MAX_VALUE);
        crud.setMaxWidth(Double.MAX_VALUE);
        flowContent.getChildren().setAll(branches);
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

    private void addStage(VBox lane, String id, String title, String type, Path file, String emptyMessage) {
        lane.getChildren().add(stage(id, title, type, file, emptyMessage, null));
    }

    private void addStage(VBox lane, String id, String title, String type, Path file,
                          String emptyMessage, Node details) {
        lane.getChildren().add(stage(id, title, type, file, emptyMessage, details));
    }

    private VBox validationInputs() {
        VBox inputs = new VBox(6,
                validationInput("CODIGO", "code", workflow.getSelectedJavaModel()),
                validationInput("ENUNCIADO", "statement", workflow.getSelectedInferredModel()));
        inputs.getStyleClass().add("workflow-context-validation-inputs");
        return inputs;
    }

    private javafx.scene.layout.HBox validationInput(String label, String id, Path model) {
        Label badge = new Label(label);
        badge.setId("student-context-validation-input-" + id + "-badge");
        badge.getStyleClass().addAll("workflow-context-input-badge", "workflow-context-input-" + id);
        Label fileName = new Label(model == null ? "Sin modelo seleccionado" : filename(model));
        fileName.setId("student-context-validation-input-" + id + "-file");
        fileName.getStyleClass().add("workflow-context-input-file");
        fileName.setWrapText(true);
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8, badge, fileName);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox stage(String id, String title, String type, Path file, String emptyMessage, Node details) {
        Label marker = new Label(type);
        marker.getStyleClass().add("workflow-context-stage-type");
        Label heading = new Label(title);
        heading.getStyleClass().add("workflow-context-stage-title");
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10, marker, heading);
        header.setAlignment(Pos.CENTER_LEFT);

        Label artifact = new Label(file == null ? emptyMessage : filename(file));
        artifact.setId("student-context-stage-" + id + "-file");
        artifact.getStyleClass().add(file == null
                ? "workflow-context-stage-empty" : "workflow-context-stage-file");
        artifact.setWrapText(true);
        Label location = new Label(file == null ? "" : file.toAbsolutePath().normalize().toString());
        location.setId("student-context-stage-" + id + "-path");
        location.getStyleClass().add("workflow-context-stage-path");
        location.setWrapText(true);
        location.setVisible(file != null);
        location.setManaged(file != null);

        VBox block = new VBox(8, header, artifact, location);
        block.setId("student-context-stage-" + id);
        block.getStyleClass().add("workflow-context-stage");
        if (file == null) {
            block.getStyleClass().add("workflow-context-stage-pending");
        }
        if (details != null) {
            block.getChildren().add(details);
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
        addVerticalArrow(connectors, "code-folder", "code-model", "student-context-code-model-arrow");
        addVerticalArrow(connectors, "code-model", "validation-report", "student-context-validation-arrow");
        addVerticalArrow(connectors, "valid-model", "validation-report", "student-context-reference-arrow");
        addVerticalArrow(connectors, "validation-report", "crud", "student-context-crud-arrow");
        connectorLayer.getChildren().setAll(connectors);
    }

    private void addVerticalArrow(List<Node> connectors, String fromId, String toId, String connectorId) {
        Bounds from = bounds(stages.get(fromId));
        Bounds to = bounds(stages.get(toId));
        double x = from.getMinX() + from.getWidth() / 2;
        double tipY = to.getMinY() - 1;
        double headLength = 9;
        double headHalfWidth = 6;
        connectors.add(connectorPath(connectorId,
                new MoveTo(x, from.getMaxY()),
                new LineTo(x, tipY),
                new MoveTo(x - headHalfWidth, tipY - headLength),
                new LineTo(x, tipY),
                new LineTo(x + headHalfWidth, tipY - headLength)));
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
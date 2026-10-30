package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class ModelValidationSection extends Section {

    private final List<Button> steps;
    private final List<VBox> panels;

    public ModelValidationSection() {
        this(ModelJsonSelection::chooseJson);
    }

    ModelValidationSection(Function<Window, File> chooseFile) {
        super("VALIDAR MODELOS", "Validar modelos", "");
        Objects.requireNonNull(chooseFile, "chooseFile");
        steps = List.of(
                UiComponents.stepButton(0, "MODELO FUENTE JAVA", "Modelo extra\u00eddo del c\u00f3digo", this::showStep),
                UiComponents.stepButton(1, "MODELO INFERIDO ENUNCIADO", "Modelo del enunciado", this::showStep),
                UiComponents.stepButton(2, "RESULTADO DE LA VALIDACI\u00d3N", "Resultado", this::showStep));
        steps.get(0).setId("model-validation-step-java");
        steps.get(1).setId("model-validation-step-statement");
        steps.get(2).setId("model-validation-step-result");
        steps.get(1).setDisable(true);
        steps.get(2).setDisable(true);

        ModelJsonSelection source = new ModelJsonSelection("model-validation-java",
                "Modelo fuente Java", "JSON extra\u00eddo de los fuentes Java.", selected -> {
                    steps.get(0).pseudoClassStateChanged(COMPLETED, true);
                    steps.get(1).setDisable(false);
                }, chooseFile);
        ModelJsonSelection inferred = new ModelJsonSelection("model-validation-statement",
                "Modelo inferido enunciado", "JSON del modelo inferido a partir del enunciado.",
                selected -> steps.get(1).pseudoClassStateChanged(COMPLETED, true), chooseFile);
        panels = List.of(UiComponents.surface(source), UiComponents.surface(inferred));
        for (VBox panel : panels) {
            panel.setMaxHeight(Region.USE_PREF_SIZE);
            StackPane.setAlignment(panel, javafx.geometry.Pos.TOP_LEFT);
        }
        HBox flow = new HBox(8,
                steps.get(0), UiComponents.arrowConnector(UiComponents.Direction.RIGHT),
                steps.get(1), UiComponents.arrowConnector(UiComponents.Direction.RIGHT), steps.get(2));
        flow.getStyleClass().addAll("statement-flow", "model-statement-flow");
        flow.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane panelHost = new StackPane();
        panelHost.getChildren().addAll(panels);
        getChildren().addAll(flow, panelHost);
        showStep(0);
    }

    private void showStep(int index) {
        if (steps.get(index).isDisabled()) {
            return;
        }
        for (int panelIndex = 0; panelIndex < panels.size(); panelIndex++) {
            panels.get(panelIndex).setVisible(panelIndex == index);
            panels.get(panelIndex).setManaged(panelIndex == index);
        }
        for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
            steps.get(stepIndex).pseudoClassStateChanged(SELECTED, stepIndex == index);
        }
    }
}
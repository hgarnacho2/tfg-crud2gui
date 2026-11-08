package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.workflow.WorkflowContext;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.io.File;
import java.util.function.Function;

public class CrudSection extends Section {

    public CrudSection() {
        this(ModelJsonSelection::chooseJson, null);
    }

    public CrudSection(WorkflowContext workflow) {
        this(ModelJsonSelection::chooseJson, workflow);
    }

    CrudSection(Function<Window, File> chooseFile) {
        this(chooseFile, null);
    }

    private CrudSection(Function<Window, File> chooseFile, WorkflowContext workflow) {
        super("CRUD", "CRUD", "");
        Button step = UiComponents.stepButton(0, "MODELO VALIDADO", "Modelo fuente Java", ignored -> { });
        step.setId("crud-step-model");
        step.pseudoClassStateChanged(SELECTED, true);
        step.setMaxWidth(Region.USE_PREF_SIZE);
        ModelJsonSelection model = new ModelJsonSelection("crud-model", "Modelo validado",
                "JSON validado extra\u00eddo de los fuentes Java.",
                selected -> {
                    step.pseudoClassStateChanged(COMPLETED, true);
                    if (workflow != null) {
                        workflow.selectJavaModel(selected);
                    }
                }, chooseFile);
        if (workflow != null) {
            model.setRestoredFile(workflow.getSelectedJavaModel(), workflow.isJavaModelManual());
        }
        getChildren().addAll(step, UiComponents.surface(model));
    }
}
package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.io.File;
import java.util.function.Function;

public final class CrudSection extends Section {

    public CrudSection() {
        this(ModelJsonSelection::chooseJson);
    }

    CrudSection(Function<Window, File> chooseFile) {
        super("CRUD", "CRUD", "");
        Button step = UiComponents.stepButton(0, "MODELO VALIDADO", "Modelo fuente Java", ignored -> { });
        step.setId("crud-step-model");
        step.pseudoClassStateChanged(SELECTED, true);
        step.setMaxWidth(Region.USE_PREF_SIZE);
        ModelJsonSelection model = new ModelJsonSelection("crud-model", "Modelo validado",
                "JSON validado extra\u00eddo de los fuentes Java.",
                selected -> step.pseudoClassStateChanged(COMPLETED, true), chooseFile);
        getChildren().addAll(step, UiComponents.surface(model));
    }
}
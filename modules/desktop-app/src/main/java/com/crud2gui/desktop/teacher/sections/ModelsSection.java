package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherPracticeData;
import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.ui.Section;
import javafx.scene.layout.VBox;

import java.util.List;

/** Página de modelos de clase y validaciones docentes. */
public final class ModelsSection extends Section {

    /** Crea los modelos y permite abrir el formulario compartido de nueva práctica. */
    public ModelsSection(TeacherPracticeData data) {
        super("DOCENCIA", "Modelos de clase", "Modelos esperados y validaciones recientes.",
                UiComponents.actionButton("Nueva práctica", List.of("section-header-action"),
                        data::showNewPracticeDialog));
        VBox panel = new VBox(14,
                UiComponents.panelHeading("Modelos de clase", "Modelos esperados y validaciones recientes"),
                TeacherComponents.modelRow("Red social", "6 clases · 8 relaciones", "Modelo disponible"),
                TeacherComponents.modelRow("Reparto de leña", "8 clases · 6 relaciones", "2 decisiones pendientes"),
                TeacherComponents.modelRow("CRUD2GUI", "Modelo en preparación", "Sin validar"));
        getChildren().add(UiComponents.surface(panel));
    }
}

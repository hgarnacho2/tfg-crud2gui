package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherPracticeData;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

/** Página de modelos de clase y validaciones docentes. */
public final class ModelsSection extends VBox {

    /** Crea los modelos y permite abrir el formulario compartido de nueva práctica. */
    public ModelsSection(TeacherPracticeData data) {
        super(20);
        setPadding(new Insets(32, 38, 36, 38));
        Button newPractice = UiComponents.actionButton(
                "Nueva práctica", List.of("section-header-action"), data::showNewPracticeDialog);
        VBox panel = new VBox(14,
                TeacherComponents.panelHeading("Modelos de clase", "Modelos esperados y validaciones recientes"),
                TeacherComponents.modelRow("Red social", "6 clases · 8 relaciones", "Modelo disponible"),
                TeacherComponents.modelRow("Reparto de leña", "8 clases · 6 relaciones", "2 decisiones pendientes"),
                TeacherComponents.modelRow("CRUD2GUI", "Modelo en preparación", "Sin validar"));
        getChildren().addAll(UiComponents.sectionHeader("DOCENCIA", "Modelos de clase",
                "Modelos esperados y validaciones recientes.", newPractice), TeacherComponents.surface(panel));
    }
}

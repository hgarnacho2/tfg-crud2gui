package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherPracticeData;
import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.ui.Section;
import javafx.scene.layout.VBox;

import java.util.List;

/** Página de entregas pendientes de revisión docente. */
public final class ReviewsSection extends Section {

    /** Crea las revisiones y permite abrir el formulario compartido de nueva práctica. */
    public ReviewsSection(TeacherPracticeData data) {
        super("DOCENCIA", "Revisiones", "Entregas pendientes de revisión.",
                UiComponents.actionButton("Nueva práctica", List.of("section-header-action"),
                        data::showNewPracticeDialog));
        VBox panel = new VBox(14,
                UiComponents.panelHeading("Revisiones pendientes", "Entregas que requieren atención"),
                TeacherComponents.reviewRow("María G.", "Red social · grupo 2", "Hace 18 min"),
                TeacherComponents.reviewRow("Aitor L.", "Reparto de leña · grupo 1", "Hace 1 h"),
                TeacherComponents.reviewRow("Nerea P.", "Red social · grupo 4", "Ayer"),
                TeacherComponents.reviewRow("Iker R.", "CRUD2GUI · grupo 3", "Ayer"));
        getChildren().add(UiComponents.surface(panel));
    }
}

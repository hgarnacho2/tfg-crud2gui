package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherPracticeData;
import com.crud2gui.desktop.teacher.TeacherPracticeData.PracticeItem;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import java.util.List;

/** Página de prácticas del curso docente. */
public final class PracticesSection extends VBox {
    private final TeacherPracticeData data;

    /** Crea la página y actualiza su tabla al cambiar las prácticas compartidas. */
    public PracticesSection(TeacherPracticeData data) {
        super(20);
        this.data = data;
        setPadding(new Insets(32, 38, 36, 38));
        Button newPractice = UiComponents.actionButton(
                "Nueva práctica", List.of("section-header-action"), data::showNewPracticeDialog);
        getChildren().addAll(UiComponents.sectionHeader("DOCENCIA", "Prácticas",
                "Enunciados y grupos asociados.", newPractice), createContent());
        data.practices().addListener((ListChangeListener<PracticeItem>) change ->
                getChildren().set(1, createContent()));
    }

    private Node createContent() {
        VBox panel = new VBox(14,
                TeacherComponents.panelHeading("Prácticas del curso", "Enunciados y grupos asociados"),
                TeacherComponents.practiceRows(data.practices(), data.practices().size()));
        return TeacherComponents.surface(panel);
    }
}

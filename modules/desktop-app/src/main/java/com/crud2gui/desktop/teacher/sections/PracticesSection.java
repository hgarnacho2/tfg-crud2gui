package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherPracticeData;
import com.crud2gui.desktop.teacher.TeacherPracticeData.PracticeItem;
import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.ui.Section;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.List;

/** Página de prácticas del curso docente. */
public final class PracticesSection extends Section {
    private final TeacherPracticeData data;

    /** Crea la página y actualiza su tabla al cambiar las prácticas compartidas. */
    public PracticesSection(TeacherPracticeData data) {
        super("DOCENCIA", "Prácticas", "Enunciados y grupos asociados.",
                UiComponents.actionButton("Nueva práctica", List.of("section-header-action"),
                        data::showNewPracticeDialog));
        this.data = data;
        getChildren().add(createContent());
        data.practices().addListener((ListChangeListener<PracticeItem>) change ->
                getChildren().set(1, createContent()));
    }

    private Node createContent() {
        VBox panel = new VBox(14,
                UiComponents.panelHeading("Prácticas del curso", "Enunciados y grupos asociados"),
                TeacherComponents.practiceRows(data.practices(), data.practices().size()));
        return UiComponents.surface(panel);
    }
}

package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherPracticeData;
import com.crud2gui.desktop.teacher.TeacherPracticeData.PracticeItem;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/** Página de resumen de la actividad docente. */
public final class OverviewSection extends VBox {
    private final TeacherPracticeData data;

    /** Crea el resumen y lo mantiene actualizado al cambiar las prácticas compartidas. */
    public OverviewSection(TeacherPracticeData data) {
        super(20);
        this.data = data;
        setPadding(new Insets(32, 38, 36, 38));
        Button newPractice = UiComponents.actionButton(
                "Nueva práctica", List.of("section-header-action"), data::showNewPracticeDialog);
        getChildren().addAll(UiComponents.sectionHeader("DOCENCIA", "Panel docente",
                "Estado de tus prácticas y actividad reciente.", newPractice), createContent());
        data.practices().addListener((ListChangeListener<PracticeItem>) change ->
                getChildren().set(1, createContent()));
    }

    private Node createContent() {
        String activeCount = Integer.toString((int) data.practices().stream()
                .filter(item -> !item.status().equals("Borrador")).count());
        HBox metrics = new HBox(14,
                TeacherComponents.metric("PRÁCTICAS ACTIVAS", activeCount, "En tus cursos", TeacherComponents.GREEN),
                TeacherComponents.metric("ENTREGAS PENDIENTES", "12", "Requieren revisión", "#b36a24"),
                TeacherComponents.metric("AVISOS DE MODELO", "03", "En validaciones recientes", "#526b91"));

        VBox practicePanel = new VBox(14,
                TeacherComponents.panelHeading("Prácticas recientes", "Seguimiento del curso"),
                TeacherComponents.practiceRows(data.practices(), 3));
        VBox reviewPanel = new VBox(14,
                TeacherComponents.panelHeading("Pendientes de revisión", "Actividad de tus grupos"),
                TeacherComponents.reviewRow("María G.", "Red social", "Hace 18 min"),
                TeacherComponents.reviewRow("Aitor L.", "Reparto de leña", "Hace 1 h"),
                TeacherComponents.reviewRow("Nerea P.", "Red social", "Ayer"));

        VBox left = TeacherComponents.surface(practicePanel);
        VBox right = TeacherComponents.surface(reviewPanel);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setPrefWidth(320);

        HBox columns = new HBox(16, left, right);
        columns.setAlignment(Pos.TOP_LEFT);
        return new VBox(18, metrics, columns);
    }
}

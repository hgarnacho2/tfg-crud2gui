package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.ui.UiComponents;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

import static com.crud2gui.desktop.student.sections.StudentSectionComponents.*;

/** Pagina de resumen del alumno con actividad y proximos pasos de muestra. */
public final class OverviewSection extends VBox {

    /**
     * Construye la cabecera y el resumen.
     *
     * @param openPractices accion de navegacion proporcionada por el dashboard
     */
    public OverviewSection(Runnable openPractices) {
        super(20);
        Objects.requireNonNull(openPractices, "openPractices");
        setPadding(new Insets(32, 38, 36, 38));
        HBox metrics = new HBox(14,
                metric("PRÁCTICAS ACTIVAS", "3", "En tus asignaturas", GREEN),
                metric("PRÓXIMA ENTREGA", "24 oct", "CRUD2GUI · en 2 días", "#b36a24"),
                metric("MODELOS VALIDADOS", "2 / 3", "Una revisión pendiente", "#526b91"));
        VBox practices = panel(
                panelHeading("Continuar con mis prácticas", "Actividad reciente"),
                practiceRow("CRUD2GUI", "Ingeniería del software · Práctica 3", "Entrega: 24 oct", "En curso"),
                practiceRow("Reparto de leña", "Programación orientada a objetos", "Entrega: 30 oct", "En curso"),
                practiceRow("Red social", "Programación orientada a objetos", "Entregada: 18 oct", "Revisada"));
        VBox nextStep = panel(
                panelHeading("Siguiente paso", "CRUD2GUI · Práctica 3"),
                UiComponents.mutedText(
                        "Completa el modelo de clases y valida las relaciones antes de preparar la entrega."),
                UiComponents.actionButton("Abrir mis prácticas", List.of(), openPractices));
        nextStep.setPrefWidth(300);
        practices.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(practices, Priority.ALWAYS);
        HBox columns = new HBox(16, practices, nextStep);
        columns.setAlignment(Pos.TOP_LEFT);
        getChildren().addAll(UiComponents.sectionHeader(
                "Resumen", "Mi espacio", "Tus prácticas, entregas y próximos pasos."),
                new VBox(18, metrics, columns));
    }
}

package com.crud2gui.desktop.student;

import com.crud2gui.authenticator.AuthenticatedUser;
import com.crud2gui.desktop.student.sections.DeliveriesSection;
import com.crud2gui.desktop.student.sections.ModelsSection;
import com.crud2gui.desktop.student.sections.OverviewSection;
import com.crud2gui.desktop.student.sections.PracticesSection;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Compone el panel del alumno y conserva sus paginas al cambiar de seccion. */
public final class StudentDashboardView extends BorderPane {

    private final ObjectProperty<StudentSection> selectedSection = new SimpleObjectProperty<>();
    private final Map<StudentSection, Node> pages = new EnumMap<>(StudentSection.class);
    private final ScrollPane content = new ScrollPane();

    /**
     * Monta la navegacion y muestra el resumen inicial.
     *
     * @param user identidad autenticada
     * @param onSignOut accion de cierre de sesion
     */
    public StudentDashboardView(AuthenticatedUser user, Runnable onSignOut) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(onSignOut, "onSignOut");
        setStyle("-fx-background-color: #f3f7f5; -fx-font-family: 'Segoe UI';");
        setLeft(UiComponents.dashboardSidebar("Panel alumno",
                UiComponents.currentUserCard(user, "alumno", onSignOut),
                List.of(StudentSection.values()), StudentSection::label, selectedSection));

        content.setFitToWidth(true);
        content.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        content.setStyle("-fx-background: #f3f7f5; -fx-background-color: #f3f7f5;");
        setCenter(content);
        selectedSection.addListener((observable, previous, current) -> showSection(current));
        selectedSection.set(StudentSection.OVERVIEW);
    }

    private void showSection(StudentSection section) {
        content.setContent(pages.computeIfAbsent(section, this::createSection));
    }

    private Node createSection(StudentSection section) {
        return switch (section) {
            case OVERVIEW -> new OverviewSection(() -> selectedSection.set(StudentSection.PRACTICES));
            case PRACTICES -> new PracticesSection();
            case DELIVERIES -> new DeliveriesSection();
            case MODELS -> new ModelsSection();
        };
    }

    /** Secciones disponibles en el menu del alumno, en orden de presentacion. */
    private enum StudentSection {
        OVERVIEW("Resumen"),
        PRACTICES("Mis prácticas"),
        DELIVERIES("Mis entregas"),
        MODELS("Mis modelos");

        private final String label;

        StudentSection(String label) {
            this.label = label;
        }

        /** @return nombre visible en el menu y en la ruta de la cabecera */
        private String label() {
            return label;
        }
    }
}

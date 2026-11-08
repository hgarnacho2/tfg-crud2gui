package com.crud2gui.desktop.student;

import com.crud2gui.authenticator.entraid.AuthenticatedUser;
import com.crud2gui.desktop.student.sections.CrudSection;
import com.crud2gui.desktop.student.sections.ModelFromCodeSection;
import com.crud2gui.desktop.student.sections.ModelValidationSection;
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
    private final StudentWorkflowContext workflow;

    /**
    * Monta la navegación y muestra el primer flujo de trabajo.
     *
     * @param user identidad autenticada
     * @param onSignOut accion de cierre de sesion
     */
    public StudentDashboardView(AuthenticatedUser user, Runnable onSignOut) {
        this(user, onSignOut, new StudentWorkflowContext(Objects.requireNonNull(user, "user").objectId()));
    }

    /** Crea el panel alumno usando el contexto de trabajo ya resuelto por la navegación. */
    public StudentDashboardView(AuthenticatedUser user, Runnable onSignOut, StudentWorkflowContext workflow) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(onSignOut, "onSignOut");
        this.workflow = Objects.requireNonNull(workflow, "workflow");
        getStyleClass().add("dashboard-root");
        setLeft(UiComponents.dashboardSidebar("Panel alumno",
                UiComponents.currentUserCard(user, "alumno", onSignOut),
                List.of(StudentSection.values()), StudentSection::label, selectedSection));

        content.setFitToWidth(true);
        content.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        content.getStyleClass().add("dashboard-scroll");
        setCenter(content);
        selectedSection.addListener((observable, previous, current) -> showSection(current));
        selectedSection.set(StudentSection.MODEL_FROM_CODE);
    }

    private void showSection(StudentSection section) {
        content.setFitToHeight(section == StudentSection.MODEL_FROM_CODE);
        Node page = pages.computeIfAbsent(section, this::createSection);
        if (page instanceof ModelValidationSection validation) {
            validation.refreshModelSelections();
        }
        content.setContent(page);
    }

    private Node createSection(StudentSection section) {
        return switch (section) {
            case MODEL_FROM_CODE -> new ModelFromCodeSection(workflow);
            case MODEL_VALIDATION -> new ModelValidationSection(workflow);
            case CRUD -> new CrudSection(workflow);
        };
    }

    /** Secciones disponibles en el menú del alumno, en orden de presentación. */
    private enum StudentSection {
        MODEL_FROM_CODE("C\u00f3digo >> Modelo"),
        MODEL_VALIDATION("Validar modelos"),
        CRUD("CRUD");

        private final String label;

        StudentSection(String label) {
            this.label = label;
        }

        /** @return nombre visible en el menú y en la ruta de la cabecera */
        private String label() {
            return label;
        }
    }
}

package com.crud2gui.desktop.teacher;

import com.crud2gui.authenticator.entraid.AuthenticatedUser;
import com.crud2gui.desktop.teacher.sections.CrudSection;
import com.crud2gui.desktop.teacher.sections.TeacherContextSection;
import com.crud2gui.desktop.teacher.sections.ModelFromStatementSection;
import com.crud2gui.desktop.teacher.sections.ModelFromCodeSection;
import com.crud2gui.desktop.teacher.sections.ModelValidationSection;
import com.crud2gui.desktop.teacher.sections.StatementSection;
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

/** Pantalla del perfil docente con los flujos de enunciado y modelos. */
public final class TeacherDashboardView extends BorderPane {

    /** Secciones disponibles en la navegación del panel docente. */
    private enum TeacherSection {
        CONTEXT("Contexto"),
        STATEMENT("Enunciado"),
        MODEL_FROM_STATEMENT("Enunciado >> Modelo"),
        MODEL_FROM_CODE("Código >> Modelo"),
        MODEL_VALIDATION("Validar modelos"),
        CRUD("CRUD");

        private final String label;

        /** Crea una sección con el texto que se muestra en el menú lateral. */
        TeacherSection(String label) {
            this.label = label;
        }

        /** Devuelve el texto que se muestra en el menú lateral. */
        public String label() {
            return label;
        }
    }

    private final ObjectProperty<TeacherSection> selectedSection =
            new SimpleObjectProperty<>(TeacherSection.CONTEXT);
    private final Map<TeacherSection, Node> pages = new EnumMap<>(TeacherSection.class);
    private final ScrollPane scroll = new ScrollPane();
    private final TeacherWorkflowContext workflow;

    /** Crea el panel docente y conserva la acción de cierre de sesión proporcionada. */
    public TeacherDashboardView(AuthenticatedUser user, Runnable onSignOut) {
        this(user, onSignOut, new TeacherWorkflowContext(Objects.requireNonNull(user, "user").objectId()));
    }

    /** Crea el panel docente usando el contexto de trabajo ya resuelto por la navegación. */
    public TeacherDashboardView(AuthenticatedUser user, Runnable onSignOut, TeacherWorkflowContext workflow) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(onSignOut, "onSignOut");
        this.workflow = Objects.requireNonNull(workflow, "workflow");
        getStyleClass().add("dashboard-root");
        setLeft(UiComponents.dashboardSidebar("Panel docente",
                UiComponents.currentUserCard(user, "docente", onSignOut),
                List.of(TeacherSection.values()), TeacherSection::label, selectedSection));

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("dashboard-scroll");
        setCenter(scroll);

        selectedSection.addListener((observable, oldValue, newValue) -> showSection(newValue));
        showSection(selectedSection.get());
    }

    private void showSection(TeacherSection section) {
        scroll.setFitToHeight(section == TeacherSection.MODEL_FROM_STATEMENT
                || section == TeacherSection.MODEL_FROM_CODE);
        Node page = pages.computeIfAbsent(section, this::createPage);
        if (page instanceof ModelFromStatementSection modelFromStatement) {
            modelFromStatement.refreshStatementInput();
        } else if (page instanceof TeacherContextSection context) {
            context.refresh();
        } else if (page instanceof ModelValidationSection validation) {
            validation.refreshModelSelections();
        }
        scroll.setContent(page);
    }

    private Node createPage(TeacherSection section) {
        return switch (section) {
            case CONTEXT -> new TeacherContextSection(workflow);
            case STATEMENT -> new StatementSection(workflow);
            case MODEL_FROM_STATEMENT -> new ModelFromStatementSection(workflow);
            case MODEL_FROM_CODE -> new ModelFromCodeSection(workflow);
            case MODEL_VALIDATION -> new ModelValidationSection(workflow);
            case CRUD -> new CrudSection();
        };
    }
}

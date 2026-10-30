package com.crud2gui.desktop.teacher;

import com.crud2gui.authenticator.AuthenticatedUser;
import com.crud2gui.desktop.teacher.sections.CrudSection;
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
            new SimpleObjectProperty<>(TeacherSection.STATEMENT);
    private final Map<TeacherSection, Node> pages = new EnumMap<>(TeacherSection.class);
    private final ScrollPane scroll = new ScrollPane();

    /** Crea el panel docente y conserva la acción de cierre de sesión proporcionada. */
    public TeacherDashboardView(AuthenticatedUser user, Runnable onSignOut) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(onSignOut, "onSignOut");
        setStyle("-fx-background-color: #f3f7f5; -fx-font-family: 'Segoe UI';");
        setLeft(UiComponents.dashboardSidebar("Panel docente",
                UiComponents.currentUserCard(user, "docente", onSignOut),
                List.of(TeacherSection.values()), TeacherSection::label, selectedSection));

        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #f3f7f5; -fx-background-color: #f3f7f5;");
        setCenter(scroll);

        selectedSection.addListener((observable, oldValue, newValue) -> showSection(newValue));
        showSection(selectedSection.get());
    }

    private void showSection(TeacherSection section) {
        scroll.setFitToHeight(section == TeacherSection.MODEL_FROM_STATEMENT
                || section == TeacherSection.MODEL_FROM_CODE);
        scroll.setContent(pages.computeIfAbsent(section, this::createPage));
    }

    private Node createPage(TeacherSection section) {
        return switch (section) {
            case STATEMENT -> new StatementSection();
            case MODEL_FROM_STATEMENT -> new ModelFromStatementSection();
            case MODEL_FROM_CODE -> new ModelFromCodeSection();
            case MODEL_VALIDATION -> new ModelValidationSection();
            case CRUD -> new CrudSection();
        };
    }
}

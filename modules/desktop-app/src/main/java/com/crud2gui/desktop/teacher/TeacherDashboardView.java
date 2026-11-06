package com.crud2gui.desktop.teacher;

import com.crud2gui.authenticator.entraid.AuthenticatedUser;
import com.crud2gui.desktop.teacher.sections.CrudSection;
import com.crud2gui.desktop.teacher.sections.ModelFromStatementSection;
import com.crud2gui.desktop.teacher.sections.ModelFromCodeSection;
import com.crud2gui.desktop.teacher.sections.ModelValidationSection;
import com.crud2gui.desktop.teacher.sections.StatementSection;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
        private final TeacherWorkflowContext workflow;

    /** Crea el panel docente y conserva la acción de cierre de sesión proporcionada. */
    public TeacherDashboardView(AuthenticatedUser user, Runnable onSignOut) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(onSignOut, "onSignOut");
        workflow = new TeacherWorkflowContext(user.objectId());
        resolvePreviousContext();
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

    /**
     * Ofrece continuar o reiniciar cuando existe un contexto anterior para la cuenta.
     * Cerrar el diálogo conserva el contexto; reiniciarlo solo elimina sus referencias.
     */
    private void resolvePreviousContext() {
        if (!workflow.hasSavedContext()) {
            return;
        }
        ButtonType continueWork = new ButtonType("Continuar con el contexto anterior", ButtonBar.ButtonData.YES);
        ButtonType resetWork = new ButtonType("Reiniciar contexto de trabajo", ButtonBar.ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Contexto de trabajo");
        alert.setHeaderText("Se ha encontrado un contexto de trabajo anterior.");
        alert.setContentText("Puedes continuarlo o reiniciarlo. Los archivos existentes no se eliminarán.");
        alert.getButtonTypes().setAll(continueWork, resetWork);
        alert.showAndWait().filter(resetWork::equals).ifPresent(button -> workflow.reset());
    }

    private void showSection(TeacherSection section) {
        scroll.setFitToHeight(section == TeacherSection.MODEL_FROM_STATEMENT
                || section == TeacherSection.MODEL_FROM_CODE);
        Node page = pages.computeIfAbsent(section, this::createPage);
        if (page instanceof ModelFromStatementSection modelFromStatement) {
            modelFromStatement.refreshStatementInput();
        } else if (page instanceof ModelValidationSection validation) {
            validation.refreshModelSelections();
        }
        scroll.setContent(page);
    }

    private Node createPage(TeacherSection section) {
        return switch (section) {
            case STATEMENT -> new StatementSection(workflow);
            case MODEL_FROM_STATEMENT -> new ModelFromStatementSection(workflow);
            case MODEL_FROM_CODE -> new ModelFromCodeSection(workflow);
            case MODEL_VALIDATION -> new ModelValidationSection(workflow);
            case CRUD -> new CrudSection();
        };
    }
}

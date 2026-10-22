package com.crud2gui.desktop.student;

import com.crud2gui.authenticator.AuthenticatedUser;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Pantalla inicial del perfil alumno, con información de ejemplo mientras se conecta el backend. */
public final class StudentDashboardView extends BorderPane {

    private static final String INK = "#17212b";
    private static final String MUTED = "#64736f";
    private static final String GREEN = "#087e65";

    private final AuthenticatedUser user;
    private final Runnable onSignOut;
    private final VBox pageContent = new VBox(20);
    private final Map<Section, Button> navigation = new EnumMap<>(Section.class);

    public StudentDashboardView(AuthenticatedUser user, Runnable onSignOut) {
        this.user = user;
        this.onSignOut = Objects.requireNonNull(onSignOut, "onSignOut");
        setStyle("-fx-background-color: #f3f7f5; -fx-font-family: 'Segoe UI';");
        setLeft(createSidebar());

        ScrollPane scroll = new ScrollPane(pageContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #f3f7f5; -fx-background-color: #f3f7f5;");
        setCenter(scroll);
        showSection(Section.OVERVIEW);
    }

    private Node createSidebar() {
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(236);
        sidebar.setPadding(new Insets(26, 18, 20, 18));
        sidebar.setStyle("-fx-background-color: #123b35;");

        Label brand = new Label("CRUD2GUI");
        brand.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label workspace = new Label("ESPACIO ALUMNO");
        workspace.setPadding(new Insets(0, 0, 16, 0));
        workspace.setStyle("-fx-text-fill: #9ec6b8; -fx-font-size: 10px; -fx-font-weight: bold;");
        sidebar.getChildren().addAll(brand, workspace);

        for (Section section : Section.values()) {
            Button button = new Button(section.label);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setOnAction(event -> showSection(section));
            navigation.put(section, button);
            sidebar.getChildren().add(button);
        }

        return UiComponents.dashboardSidebar(sidebar,
                UiComponents.currentUserCard(user, "alumno", onSignOut));
    }

    private void showSection(Section section) {
        navigation.forEach((item, button) -> {
            boolean selected = item == section;
            button.setStyle(selected
                    ? "-fx-background-color: #1b5b50; -fx-text-fill: white; -fx-font-weight: bold; "
                            + "-fx-background-radius: 5px; -fx-padding: 11px 12px;"
                    : "-fx-background-color: transparent; -fx-text-fill: #d8e8e2; "
                            + "-fx-background-radius: 5px; -fx-padding: 11px 12px;");
        });
        pageContent.setPadding(new Insets(32, 38, 36, 38));
        pageContent.getChildren().setAll(createHeader(section), createSection(section));
    }

    private VBox createHeader(Section section) {
        return UiComponents.sectionHeader(section.label, section.title, section.subtitle);
    }

    private Node createSection(Section section) {
        return switch (section) {
            case OVERVIEW -> createOverview();
            case PRACTICES -> createPractices();
            case DELIVERIES -> createDeliveries();
            case MODELS -> createModels();
        };
    }

    private Node createOverview() {
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
                UiComponents.actionButton(
                        "Abrir mis prácticas",
                        List.of(),
                        () -> showSection(Section.PRACTICES)));
        nextStep.setPrefWidth(300);
        practices.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(practices, Priority.ALWAYS);
        HBox columns = new HBox(16, practices, nextStep);
        columns.setAlignment(Pos.TOP_LEFT);
        return new VBox(18, metrics, columns);
    }

    private Node createPractices() {
        return panel(
                panelHeading("Mis prácticas", "Enunciados, fechas y estado de cada actividad"),
                practiceRow("CRUD2GUI", "Ingeniería del software · Práctica 3", "Entrega: 24 oct", "En curso"),
                practiceRow("Reparto de leña", "Programación orientada a objetos", "Entrega: 30 oct", "En curso"),
                practiceRow("Red social", "Programación orientada a objetos", "Entregada: 18 oct", "Revisada"),
                practiceRow("Gestor de biblioteca", "Diseño de software", "Entrega: 05 nov", "Pendiente"));
    }

    private Node createDeliveries() {
        return panel(
                panelHeading("Mis entregas", "Estado y comentarios de las entregas"),
                labeledRow("Red social", "Entrega enviada el 18 oct", "Revisada", GREEN),
                labeledRow("CRUD2GUI", "Borrador · todavía puedes editarlo", "Sin entregar", "#b36a24"),
                labeledRow("Reparto de leña", "No hay ninguna entrega todavía", "Pendiente", MUTED));
    }

    private Node createModels() {
        return panel(
                panelHeading("Mis modelos", "Modelos asociados a tus prácticas"),
                labeledRow("Red social", "6 clases · 8 relaciones", "Validación correcta", GREEN),
                labeledRow("Reparto de leña", "8 clases · 6 relaciones", "Revisa 2 avisos", "#b36a24"),
                labeledRow("CRUD2GUI", "Modelo en preparación", "Aún no validado", MUTED));
    }

    private Node metric(String title, String value, String detail, String accent) {
        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label number = new Label(value);
        number.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 26px; -fx-font-weight: bold;");
        Label caption = new Label(detail);
        caption.setWrapText(true);
        caption.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        VBox card = panel(heading, number, caption);
        card.setMinWidth(150);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Node practiceRow(String name, String course, String due, String status) {
        Label title = new Label(name);
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label details = new Label(course + "  ·  " + due);
        details.setWrapText(true);
        details.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        VBox identity = new VBox(5, title, details);
        HBox.setHgrow(identity, Priority.ALWAYS);
        HBox row = new HBox(12, identity, statusBadge(status));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 0, 12, 0));
        return row;
    }

    private Node labeledRow(String name, String detail, String status, String accent) {
        Label title = new Label(name);
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label description = new Label(detail);
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        Label state = new Label(status);
        state.setWrapText(true);
        state.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox text = new VBox(5, title, description);
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(14, text, state);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(13, 0, 13, 0));
        return row;
    }

    private static VBox panel(Node... children) {
        VBox panel = new VBox(12, children);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 7px; "
                + "-fx-border-color: #e5ece8; -fx-border-radius: 7px;");
        return panel;
    }

    private static VBox panelHeading(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label subtitle = new Label(subtitleText);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        return new VBox(4, title, subtitle);
    }

    private static Label statusBadge(String text) {
        String color = text.equals("Revisada") ? GREEN : text.equals("En curso") ? "#9a6414" : MUTED;
        String background = text.equals("Revisada") ? "#e4f2ed" : text.equals("En curso") ? "#fbf1dc" : "#edf1ef";
        Label badge = new Label(text);
        badge.setPadding(new Insets(5, 8, 5, 8));
        badge.setStyle("-fx-text-fill: " + color + "; -fx-background-color: " + background
                + "; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
        return badge;
    }

    private enum Section {
        OVERVIEW("Resumen", "Mi espacio", "Tus prácticas, entregas y próximos pasos."),
        PRACTICES("Mis prácticas", "Mis prácticas", "Enunciados y fechas de entrega."),
        DELIVERIES("Mis entregas", "Mis entregas", "Estado y comentarios de tus trabajos."),
        MODELS("Mis modelos", "Mis modelos", "Modelos de clase y resultados de validación.");

        private final String label;
        private final String title;
        private final String subtitle;

        Section(String label, String title, String subtitle) {
            this.label = label;
            this.title = title;
            this.subtitle = subtitle;
        }
    }
}
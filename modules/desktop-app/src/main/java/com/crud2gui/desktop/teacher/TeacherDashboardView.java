package com.crud2gui.desktop.teacher;

import com.crud2gui.authenticator.AuthenticatedUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Pantalla inicial del perfil docente, con datos de muestra mientras se conecta el backend. */
public final class TeacherDashboardView extends BorderPane {

    private static final String INK = "#17212b";
    private static final String MUTED = "#64736f";
    private static final String GREEN = "#087e65";
    private static final String BORDER = "#e1e9e5";

    private final AuthenticatedUser user;
    private final VBox pageContent = new VBox(20);
    private final Map<Section, Button> navigation = new EnumMap<>(Section.class);
    private final List<PracticeItem> practices = new ArrayList<>(List.of(
            new PracticeItem("Red social", "POO · 2023", "8 grupos", "En curso", "Actualizada hoy"),
            new PracticeItem("Reparto de leña", "POO · 2024", "6 grupos", "En revisión", "Actualizada ayer"),
            new PracticeItem("CRUD2GUI", "Proyecto · 2026", "4 grupos", "Borrador", "Actualizada el lunes")));

    public TeacherDashboardView(AuthenticatedUser user) {
        this.user = user;
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
        Label workspace = new Label("ESPACIO DOCENTE");
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

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label prototype = new Label("PROTOTIPO · DATOS DE MUESTRA");
        prototype.setWrapText(true);
        prototype.setStyle("-fx-text-fill: #9ec6b8; -fx-font-size: 10px;");

        String displayName = user.displayName() == null || user.displayName().isBlank()
                ? user.username() : user.displayName();
        Label accountName = new Label(displayName == null ? "Docente" : displayName);
        accountName.setWrapText(true);
        accountName.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label accountRole = new Label("Perfil docente");
        accountRole.setStyle("-fx-text-fill: #9ec6b8; -fx-font-size: 11px;");
        sidebar.getChildren().addAll(spacer, prototype, accountName, accountRole);
        return sidebar;
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

    private Node createHeader(Section section) {
        Label eyebrow = new Label("CRUD2GUI  /  DOCENCIA");
        eyebrow.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label title = new Label(section.title);
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 26px; -fx-font-weight: bold;");
        Label subtitle = new Label(section.subtitle);
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
        VBox heading = new VBox(6, eyebrow, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button newPractice = new Button("Nueva práctica");
        newPractice.setOnAction(event -> showNewPracticeDialog());
        newPractice.setStyle("-fx-background-color: " + GREEN + "; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-padding: 10px 16px; -fx-background-radius: 5px;");

        HBox header = new HBox(20, heading, spacer, newPractice);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Node createSection(Section section) {
        return switch (section) {
            case OVERVIEW -> createOverview();
            case PRACTICES -> createPractices();
            case REVIEWS -> createReviews();
            case MODELS -> createModels();
        };
    }

    private Node createOverview() {
        HBox metrics = new HBox(14,
                metric("PRÁCTICAS ACTIVAS", activePracticeCount(), "En tus cursos", GREEN),
                metric("ENTREGAS PENDIENTES", "12", "Requieren revisión", "#b36a24"),
                metric("AVISOS DE MODELO", "03", "En validaciones recientes", "#526b91"));

        VBox practicePanel = new VBox(14,
                panelHeading("Prácticas recientes", "Seguimiento del curso"),
                practiceRows(3));
        VBox reviewPanel = new VBox(14,
                panelHeading("Pendientes de revisión", "Actividad de tus grupos"),
                reviewRow("María G.", "Red social", "Hace 18 min"),
                reviewRow("Aitor L.", "Reparto de leña", "Hace 1 h"),
                reviewRow("Nerea P.", "Red social", "Ayer"));

        VBox left = surface(practicePanel);
        VBox right = surface(reviewPanel);
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setPrefWidth(320);

        HBox columns = new HBox(16, left, right);
        columns.setAlignment(Pos.TOP_LEFT);
        return new VBox(18, metrics, columns);
    }

    private Node createPractices() {
        VBox panel = new VBox(14,
                panelHeading("Prácticas del curso", "Enunciados y grupos asociados"),
                practiceRows(practices.size()));
        return surface(panel);
    }

    private Node createReviews() {
        VBox panel = new VBox(14,
                panelHeading("Revisiones pendientes", "Entregas que requieren atención"),
                reviewRow("María G.", "Red social · grupo 2", "Hace 18 min"),
                reviewRow("Aitor L.", "Reparto de leña · grupo 1", "Hace 1 h"),
                reviewRow("Nerea P.", "Red social · grupo 4", "Ayer"),
                reviewRow("Iker R.", "CRUD2GUI · grupo 3", "Ayer"));
        return surface(panel);
    }

    private Node createModels() {
        VBox panel = new VBox(14,
                panelHeading("Modelos de clase", "Modelos esperados y validaciones recientes"),
                modelRow("Red social", "6 clases · 8 relaciones", "Modelo disponible"),
                modelRow("Reparto de leña", "8 clases · 6 relaciones", "2 decisiones pendientes"),
                modelRow("CRUD2GUI", "Modelo en preparación", "Sin validar"));
        return surface(panel);
    }

    private Node metric(String title, String value, String detail, String accent) {
        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label number = new Label(value);
        number.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label caption = new Label(detail);
        caption.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        VBox metric = surface(new VBox(8, heading, number, caption));
        metric.setMinWidth(170);
        metric.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    private VBox practiceRows(int limit) {
        VBox rows = new VBox(0);
        rows.getChildren().add(new HBox(12,
                columnHeader("PRÁCTICA", 2), columnHeader("GRUPOS", 1),
                columnHeader("ESTADO", 1), columnHeader("ACTUALIZACIÓN", 1)));
        rows.getChildren().add(separator());
        practices.stream().limit(limit).forEach(practice -> {
            HBox row = new HBox(12,
                    practiceIdentity(practice),
                    valueCell(practice.groups, 1),
                    statusBadge(practice.status),
                    valueCell(practice.updated, 1));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 0, 14, 0));
            rows.getChildren().add(row);
            rows.getChildren().add(separator());
        });
        return rows;
    }

    private Node practiceIdentity(PracticeItem practice) {
        Label title = new Label(practice.title);
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label course = new Label(practice.course);
        course.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        VBox identity = new VBox(4, title, course);
        HBox.setHgrow(identity, Priority.ALWAYS);
        return identity;
    }

    private Node reviewRow(String student, String practice, String updated) {
        Label initials = new Label(initials(student));
        initials.setAlignment(Pos.CENTER);
        initials.setMinSize(34, 34);
        initials.setStyle("-fx-background-color: #e3f0eb; -fx-background-radius: 17px; "
                + "-fx-text-fill: " + GREEN + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        Label studentLabel = new Label(student);
        studentLabel.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        Label practiceLabel = new Label(practice);
        practiceLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        VBox details = new VBox(4, studentLabel, practiceLabel);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label time = new Label(updated);
        time.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px;");
        HBox row = new HBox(10, initials, details, time);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));
        return row;
    }

    private Node modelRow(String name, String summary, String status) {
        Label title = new Label(name);
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label details = new Label(summary);
        details.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        Label state = new Label(status);
        state.setStyle("-fx-text-fill: " + GREEN + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        VBox text = new VBox(5, title, details, state);
        text.setPadding(new Insets(12, 0, 12, 0));
        VBox.setVgrow(text, Priority.NEVER);
        return text;
    }

    private void showNewPracticeDialog() {
        Dialog<PracticeItem> dialog = new Dialog<>();
        dialog.setTitle("Nueva práctica");
        ButtonType create = new ButtonType("Crear borrador", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(create, ButtonType.CANCEL);

        TextField title = new TextField();
        title.setPromptText("Nombre de la práctica");
        TextField course = new TextField();
        course.setPromptText("Asignatura o curso");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.addRow(0, new Label("Nombre"), title);
        form.addRow(1, new Label("Curso"), course);
        dialog.getDialogPane().setContent(form);
        Button createButton = (Button) dialog.getDialogPane().lookupButton(create);
        createButton.setDisable(true);
        title.textProperty().addListener((observable, oldValue, newValue) ->
                createButton.setDisable(newValue == null || newValue.isBlank()));
        dialog.setResultConverter(button -> button == create
                ? new PracticeItem(title.getText().strip(),
                        course.getText().isBlank() ? "Curso sin especificar" : course.getText().strip(),
                        "0 grupos", "Borrador", "Creada ahora")
                : null);
        dialog.showAndWait().ifPresent(practice -> {
            practices.add(0, practice);
            showSection(Section.PRACTICES);
        });
    }

    private String activePracticeCount() {
        return Integer.toString((int) practices.stream().filter(item -> !item.status.equals("Borrador")).count());
    }

    private static VBox surface(Node content) {
        VBox surface = new VBox(content);
        surface.setPadding(new Insets(20));
        surface.setStyle("-fx-background-color: white; -fx-background-radius: 7px; "
                + "-fx-border-color: #e5ece8; -fx-border-radius: 7px;");
        return surface;
    }

    private static VBox panelHeading(String titleText, String subtitleText) {
        Label title = new Label(titleText);
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        return new VBox(4, title, subtitle);
    }

    private static Label columnHeader(String text, double grow) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 9px; -fx-font-weight: bold;");
        HBox.setHgrow(label, Priority.ALWAYS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Label valueCell(String text, double grow) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        HBox.setHgrow(label, Priority.ALWAYS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Label statusBadge(String text) {
        Label badge = new Label(text);
        badge.setPadding(new Insets(5, 8, 5, 8));
        String color = text.equals("En curso") ? GREEN : text.equals("En revisión") ? "#9a6414" : "#66736f";
        String background = text.equals("En curso") ? "#e4f2ed" : text.equals("En revisión") ? "#fbf1dc" : "#edf1ef";
        badge.setStyle("-fx-text-fill: " + color + "; -fx-background-color: " + background
                + "; -fx-background-radius: 4px; -fx-font-size: 10px; -fx-font-weight: bold;");
        return badge;
    }

    private static Region separator() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: " + BORDER + ";");
        return line;
    }

    private static String initials(String name) {
        String[] parts = name.strip().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private enum Section {
        OVERVIEW("Resumen", "Panel docente", "Estado de tus prácticas y actividad reciente."),
        PRACTICES("Prácticas", "Prácticas", "Enunciados y grupos asociados."),
        REVIEWS("Revisiones", "Revisiones", "Entregas pendientes de revisión."),
        MODELS("Modelos", "Modelos de clase", "Modelos esperados y validaciones recientes.");

        private final String label;
        private final String title;
        private final String subtitle;

        Section(String label, String title, String subtitle) {
            this.label = label;
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    private record PracticeItem(String title, String course, String groups, String status, String updated) { }
}
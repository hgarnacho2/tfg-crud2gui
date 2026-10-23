package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.teacher.TeacherPracticeData.PracticeItem;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/** Elementos visuales compartidos exclusivamente por las páginas docentes. */
final class TeacherComponents {
    private static final String INK = "#17212b";
    private static final String MUTED = "#64736f";
    static final String GREEN = "#087e65";
    private static final String BORDER = "#e1e9e5";

    private TeacherComponents() { }

    static Node metric(String title, String value, String detail, String accent) {
        Label heading = new Label(title);
        heading.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label number = new Label(value);
        number.setStyle("-fx-text-fill: " + accent + "; -fx-font-size: 28px; -fx-font-weight: bold;");
        Label caption = new Label(detail);
        caption.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        VBox metric = UiComponents.surface(new VBox(8, heading, number, caption));
        metric.setMinWidth(170);
        metric.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    static VBox practiceRows(List<PracticeItem> practices, int limit) {
        VBox rows = new VBox(0);
        rows.getChildren().add(new HBox(12,
                columnHeader("PRÁCTICA"), columnHeader("GRUPOS"),
                columnHeader("ESTADO"), columnHeader("ACTUALIZACIÓN")));
        rows.getChildren().add(separator());
        practices.stream().limit(limit).forEach(practice -> {
            HBox row = new HBox(12,
                    practiceIdentity(practice),
                    valueCell(practice.groups()),
                    statusBadge(practice.status()),
                    valueCell(practice.updated()));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 0, 14, 0));
            rows.getChildren().add(row);
            rows.getChildren().add(separator());
        });
        return rows;
    }

    private static Node practiceIdentity(PracticeItem practice) {
        Label title = new Label(practice.title());
        title.setStyle("-fx-text-fill: " + INK + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label course = new Label(practice.course());
        course.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        VBox identity = new VBox(4, title, course);
        HBox.setHgrow(identity, Priority.ALWAYS);
        return identity;
    }

    static Node reviewRow(String student, String practice, String updated) {
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

    static Node modelRow(String name, String summary, String status) {
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

    private static Label columnHeader(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 9px; -fx-font-weight: bold;");
        HBox.setHgrow(label, Priority.ALWAYS);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private static Label valueCell(String text) {
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
}

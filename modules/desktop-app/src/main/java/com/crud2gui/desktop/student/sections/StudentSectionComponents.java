package com.crud2gui.desktop.student.sections;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Composiciones visuales compartidas exclusivamente por las secciones del alumno. */
final class StudentSectionComponents {

    static final String INK = "#17212b";
    static final String MUTED = "#64736f";
    static final String GREEN = "#087e65";

    private StudentSectionComponents() {
    }

    static Node metric(String title, String value, String detail, String accent) {
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

    static Node practiceRow(String name, String course, String due, String status) {
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

    static Node labeledRow(String name, String detail, String status, String accent) {
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

    static VBox panel(Node... children) {
        VBox panel = new VBox(12, children);
        panel.setPadding(new Insets(20));
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 7px; "
                + "-fx-border-color: #e5ece8; -fx-border-radius: 7px;");
        return panel;
    }

    static VBox panelHeading(String titleText, String subtitleText) {
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
}

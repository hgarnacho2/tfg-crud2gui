package com.crud2gui.desktop.teacher;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/** Estado de prácticas compartido exclusivamente por las secciones docentes. */
public final class TeacherPracticeData {

    private final ObservableList<PracticeItem> practices = FXCollections.observableArrayList(
            new PracticeItem("Red social", "POO · 2023", "8 grupos", "En curso", "Actualizada hoy"),
            new PracticeItem("Reparto de leña", "POO · 2024", "6 grupos", "En revisión", "Actualizada ayer"),
            new PracticeItem("CRUD2GUI", "Proyecto · 2026", "4 grupos", "Borrador", "Actualizada el lunes"));
    private final Runnable onPracticeCreated;

    /** Crea el estado y la acción de navegación tras guardar un borrador. */
    public TeacherPracticeData(Runnable onPracticeCreated) {
        this.onPracticeCreated = onPracticeCreated;
    }

    /** Devuelve la lista observable que mantiene actualizadas las páginas ya creadas. */
    public ObservableList<PracticeItem> practices() {
        return practices;
    }

    /** Abre el formulario de nueva práctica y añade el borrador al principio de la lista. */
    public void showNewPracticeDialog() {
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
            onPracticeCreated.run();
        });
    }

    /** Datos inmutables que se muestran en las tablas de prácticas docentes. */
    public record PracticeItem(String title, String course, String groups, String status, String updated) { }
}

package com.crud2gui.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Objects;

/** Pantalla que permite conservar o reiniciar el contexto de trabajo tras autenticarse. */
public final class WorkflowContextChoiceView extends VBox {

    public WorkflowContextChoiceView(String profile, Runnable onKeepContext, Runnable onResetContext) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(onKeepContext, "onKeepContext");
        Objects.requireNonNull(onResetContext, "onResetContext");
        getStyleClass().add("login-view");
        setAlignment(Pos.CENTER);

        Label brand = UiComponents.label("CRUD2GUI", List.of("login-brand"));
        Label heading = UiComponents.label("Contexto de trabajo", List.of("login-heading"));
        Label description = UiComponents.label(
                "Se ha encontrado un contexto de trabajo guardado para el perfil " + profile
                        + ". Puedes continuar trabajando con los ficheros de entrada y salida de cada etapa "
                        + "del contexto guardado, o comenzar con un nuevo contexto en blanco.",
                List.of("login-description", "workflow-context-copy"));
        Label resetNote = UiComponents.label(
                "Reiniciar elimina las selecciones guardadas, pero no borra los archivos.",
                List.of("text-muted", "workflow-context-copy"));
        resetNote.setWrapText(true);

        Button keep = UiComponents.actionButton("Mantener contexto", List.of("login-button"), onKeepContext);
        keep.setId("workflow-context-keep");
        Button reset = UiComponents.actionButton(
                "Reiniciar contexto", List.of("secondary-button", "login-button"), onResetContext);
        reset.setId("workflow-context-reset");

        VBox card = UiComponents.vBox(List.of("login-container", "workflow-context-container"),
                brand, heading, description, keep, reset, resetNote);
        getChildren().add(card);
    }
}
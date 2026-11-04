package com.crud2gui.desktop.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

/** Diálogo reutilizable para solicitar confirmación antes de ejecutar una acción. */
public final class ConfirmationDialog {

    private ConfirmationDialog() {
    }

    /**
     * Muestra una confirmación modal y distingue la aceptación de cancelar o cerrar.
     *
     * @param owner ventana propietaria, o null si todavía no está disponible
     * @param title título de la ventana
     * @param headerText encabezado; puede ser null
     * @param contentText explicación de la acción
     * @param confirmText texto del botón afirmativo
     * @param cancelText texto del botón de cancelación
     * @return true solo si se pulsa el botón afirmativo
     */
    public static boolean confirm(Window owner, String title, String headerText, String contentText,
            String confirmText, String cancelText) {
        ButtonType confirm = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (owner != null) {
            alert.initOwner(owner);
            alert.initModality(Modality.WINDOW_MODAL);
        }
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getButtonTypes().setAll(confirm, cancel);
        return alert.showAndWait().filter(confirm::equals).isPresent();
    }
}
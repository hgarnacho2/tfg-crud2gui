package com.crud2gui.desktop.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/** Diálogos de selección y confirmación de archivos; deben abrirse desde el hilo de JavaFX. */
public final class FileDialogs {

    private FileDialogs() {
    }

    /**
     * Solicita permiso para sobrescribir, sin dar por aceptado el cierre del diálogo.
     *
     * @param path archivo existente
     * @return true solo si se confirma el reemplazo
     */
    public static boolean confirmOverwrite(Path path) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "El archivo ya existe:\n" + path + "\n\n¿Quieres reemplazarlo?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar sobrescritura");
        alert.setHeaderText("Reemplazar archivo de salida");
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    /**
     * Abre un archivo limitado a los tipos indicados.
     *
     * @param owner ventana propietaria, o null
     * @param title título del diálogo
     * @param filters tipos de archivo permitidos
     * @return archivo seleccionado, o null al cancelar
     */
    public static File chooseFile(Window owner, String title, FileChooser.ExtensionFilter... filters) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(filters);
        return chooser.showOpenDialog(owner);
    }

    /**
     * Abre un selector de carpetas con un destino inicial opcional.
     *
     * @param owner ventana propietaria, o null
     * @param initialDirectory carpeta inicial; se omite si es null o no existe
     * @param title título del diálogo
     * @return carpeta seleccionada, o null al cancelar
     */
    public static File chooseDirectory(Window owner, Path initialDirectory, String title) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(title);
        if (initialDirectory != null && Files.isDirectory(initialDirectory)) {
            chooser.setInitialDirectory(initialDirectory.toFile());
        }
        return chooser.showDialog(owner);
    }
}

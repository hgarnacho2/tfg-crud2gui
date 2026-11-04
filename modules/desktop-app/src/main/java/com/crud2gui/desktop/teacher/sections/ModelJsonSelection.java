package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.FileDialogs;
import com.crud2gui.desktop.ui.JsonEditorViewer;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

final class ModelJsonSelection extends VBox {

    private final Label filename = UiComponents.labelWithGraphic(
            "Ning\u00fan archivo seleccionado.", List.of("statement-file-name"),
            UiComponents.label("JSON", List.of("statement-file-badge", "statement-neutral-badge")), 12);
    private final Label path = UiComponents.label("", List.of("statement-file-path"));
    private final Label status;
    private final VBox fileCard = UiComponents.vBox(List.of("statement-file-card"), filename, path);
    private final VBox preview = new VBox();
    private final String readyMessage;
    private Path selectedFile;

    static File chooseJson(Window owner) {
        return FileDialogs.chooseFile(owner, "Seleccionar modelo JSON",
                new FileChooser.ExtensionFilter("Modelo JSON (*.json)", "*.json"));
    }

    ModelJsonSelection(String id, String title, String description, Consumer<Path> onSelected,
                       Function<Window, File> chooseFile) {
        this(id, title, description, onSelected, chooseFile, null);
    }

    ModelJsonSelection(String id, String title, String description, Consumer<Path> onSelected,
                       Function<Window, File> chooseFile, String readyMessage) {
        super(14);
        Objects.requireNonNull(onSelected, "onSelected");
        Objects.requireNonNull(chooseFile, "chooseFile");
        this.readyMessage = readyMessage;
        setId(id);
        filename.setId(id + "-filename");
        filename.setWrapText(true);
        path.setId(id + "-path");
        path.setWrapText(true);
        preview.setId(id + "-preview");
        preview.setVisible(false);
        preview.setManaged(false);
        status = UiComponents.statusLabel(id + "-status");
        Button select = UiComponents.actionButton("Seleccionar JSON", List.of("statement-select"), () -> {
            File file = chooseFile.apply(getScene() == null ? null : getScene().getWindow());
            if (file == null) {
                return;
            }
            Path candidate = file.toPath().toAbsolutePath().normalize();
            if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".json")
                    || !Files.isRegularFile(candidate) || !Files.isReadable(candidate)) {
                status.setText("Selecciona un archivo JSON accesible.");
                status.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                return;
            }
            JsonEditorViewer viewer = null;
            if (readyMessage != null) {
                try {
                    viewer = new JsonEditorViewer(candidate, false);
                } catch (IOException exception) {
                    status.setText("No se pudo mostrar el modelo JSON seleccionado.");
                    status.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), true);
                    return;
                }
            }
            selectedFile = candidate;
            filename.setText(file.getName());
            path.setText(candidate.toString());
            fileCard.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
            if (viewer == null) {
                status.setText("");
            } else {
                preview.getChildren().setAll(viewer.getView());
                preview.setVisible(true);
                preview.setManaged(true);
                status.setText(readyMessage);
            }
            status.pseudoClassStateChanged(PseudoClass.getPseudoClass("error"), false);
            onSelected.accept(candidate);
        });
        select.setId(id + "-select");
        getChildren().addAll(UiComponents.panelHeading(title, description), select, fileCard, preview, status);
    }

    Path getSelectedFile() {
        return selectedFile;
    }
}
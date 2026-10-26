package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.ui.Section;
import javafx.scene.layout.VBox;

/** Sección de generación de modelos a partir de un enunciado. */
public final class ModelFromCodeSection extends Section {

    /** Crea la estructura inicial, sin acciones de generación conectadas todavía. */
    public ModelFromCodeSection() {
        super("MODELO DESDE CÓDIGO", "Modelo desde código",
                "Generación de un modelo a partir de código fuente Java.");
        VBox panel = new VBox(14,
                UiComponents.panelHeading("Generar modelo",
                        "Esta funcionalidad todavía no está disponible."));
        getChildren().add(UiComponents.surface(panel));
    }
}

package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.ui.UiComponents;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

import static com.crud2gui.desktop.student.sections.StudentSectionComponents.*;

/** Pagina de modelos del alumno y sus validaciones de muestra. */
public final class ModelsSection extends VBox {

    /** Construye la cabecera y la lista de modelos. */
    public ModelsSection() {
        super(20);
        setPadding(new Insets(32, 38, 36, 38));
        getChildren().addAll(UiComponents.sectionHeader(
                "Mis modelos", "Mis modelos", "Modelos de clase y resultados de validación."),
                panel(panelHeading("Mis modelos", "Modelos asociados a tus prácticas"),
                        labeledRow("Red social", "6 clases · 8 relaciones", "Validación correcta", GREEN),
                        labeledRow("Reparto de leña", "8 clases · 6 relaciones", "Revisa 2 avisos", "#b36a24"),
                        labeledRow("CRUD2GUI", "Modelo en preparación", "Aún no validado", MUTED)));
    }
}

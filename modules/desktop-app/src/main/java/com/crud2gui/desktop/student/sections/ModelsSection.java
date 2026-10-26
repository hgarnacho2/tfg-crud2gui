package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.ui.Section;

import static com.crud2gui.desktop.student.sections.StudentSectionComponents.*;

/** Pagina de modelos del alumno y sus validaciones de muestra. */
public final class ModelsSection extends Section {

    /** Construye la cabecera y la lista de modelos. */
    public ModelsSection() {
        super("Mis modelos", "Mis modelos", "Modelos de clase y resultados de validación.");
        getChildren().add(
                panel(panelHeading("Mis modelos", "Modelos asociados a tus prácticas"),
                        labeledRow("Red social", "6 clases · 8 relaciones", "Validación correcta", GREEN),
                        labeledRow("Reparto de leña", "8 clases · 6 relaciones", "Revisa 2 avisos", "#b36a24"),
                        labeledRow("CRUD2GUI", "Modelo en preparación", "Aún no validado", MUTED)));
    }
}

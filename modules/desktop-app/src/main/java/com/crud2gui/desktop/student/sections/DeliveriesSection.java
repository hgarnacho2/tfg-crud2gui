package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.ui.Section;

import static com.crud2gui.desktop.student.sections.StudentSectionComponents.*;

/** Pagina de entregas del alumno y sus estados de muestra. */
public final class DeliveriesSection extends Section {

    /** Construye la cabecera y las entregas. */
    public DeliveriesSection() {
        super("Mis entregas", "Mis entregas", "Estado y comentarios de tus trabajos.");
        getChildren().add(
                panel(panelHeading("Mis entregas", "Estado y comentarios de las entregas"),
                        labeledRow("Red social", "Entrega enviada el 18 oct", "Revisada", GREEN),
                        labeledRow("CRUD2GUI", "Borrador · todavía puedes editarlo", "Sin entregar", "#b36a24"),
                        labeledRow("Reparto de leña", "No hay ninguna entrega todavía", "Pendiente", MUTED)));
    }
}

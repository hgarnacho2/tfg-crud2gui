package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.ui.Section;

import static com.crud2gui.desktop.student.sections.StudentSectionComponents.*;

/** Pagina de practicas del alumno con los enunciados y fechas de muestra. */
public final class PracticesSection extends Section {

    /** Construye la cabecera y la lista de practicas. */
    public PracticesSection() {
        super("Mis prácticas", "Mis prácticas", "Enunciados y fechas de entrega.");
        getChildren().add(
                panel(panelHeading("Mis prácticas", "Enunciados, fechas y estado de cada actividad"),
                        practiceRow("CRUD2GUI", "Ingeniería del software · Práctica 3", "Entrega: 24 oct", "En curso"),
                        practiceRow("Reparto de leña", "Programación orientada a objetos", "Entrega: 30 oct", "En curso"),
                        practiceRow("Red social", "Programación orientada a objetos", "Entregada: 18 oct", "Revisada"),
                        practiceRow("Gestor de biblioteca", "Diseño de software", "Entrega: 05 nov", "Pendiente")));
    }
}

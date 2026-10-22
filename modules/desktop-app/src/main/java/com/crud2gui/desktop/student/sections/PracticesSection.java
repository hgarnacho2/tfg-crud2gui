package com.crud2gui.desktop.student.sections;

import com.crud2gui.desktop.ui.UiComponents;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

import static com.crud2gui.desktop.student.sections.StudentSectionComponents.*;

/** Pagina de practicas del alumno con los enunciados y fechas de muestra. */
public final class PracticesSection extends VBox {

    /** Construye la cabecera y la lista de practicas. */
    public PracticesSection() {
        super(20);
        setPadding(new Insets(32, 38, 36, 38));
        getChildren().addAll(UiComponents.sectionHeader(
                "Mis prácticas", "Mis prácticas", "Enunciados y fechas de entrega."),
                panel(panelHeading("Mis prácticas", "Enunciados, fechas y estado de cada actividad"),
                        practiceRow("CRUD2GUI", "Ingeniería del software · Práctica 3", "Entrega: 24 oct", "En curso"),
                        practiceRow("Reparto de leña", "Programación orientada a objetos", "Entrega: 30 oct", "En curso"),
                        practiceRow("Red social", "Programación orientada a objetos", "Entregada: 18 oct", "Revisada"),
                        practiceRow("Gestor de biblioteca", "Diseño de software", "Entrega: 05 nov", "Pendiente")));
    }
}

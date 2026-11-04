package com.crud2gui.desktop.ui;

import com.crud2gui.modelvalidator.ValidationOptions;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Seleccion visual de las comprobaciones aplicadas a una validacion de modelos. */
public final class ValidationOptionsView extends VBox {

    private final Map<String, CheckBox> options = new LinkedHashMap<>();

    public ValidationOptionsView(ValidationOptions initial) {
        super(12);
        Objects.requireNonNull(initial, "initial");
        setId("model-validation-options");
        getStyleClass().add("validation-options");
        FlowPane types = new FlowPane(18, 12,
                option("classes", "Clases", initial.classes(), "Presencia, tipo, visibilidad y modificadores de clases."),
                option("interfaces", "Interfaces", initial.interfaces(), "Declaraciones de interfaces."),
                option("enums", "Enums", initial.enums(), "Declaraciones de enumeraciones."),
                option("records", "Records", initial.records(), "Declaraciones de records."),
                option("annotations", "Anotaciones", initial.annotations(), "Declaraciones de anotaciones."),
                option("abstractness", "Clases abstractas", initial.abstractness(), "Comprobacion del modificador ABSTRACT."));
        FlowPane members = new FlowPane(18, 12,
                option("relationships", "Relaciones", initial.relationships(), "Herencia y relaciones entre tipos."),
                option("attributes", "Atributos", initial.attributes(), "Atributos requeridos y sus tipos."),
                option("constructors", "Constructores", initial.constructors(), "Firmas y visibilidad de constructores."),
                option("methods", "Métodos", initial.methods(), "Metodos requeridos y accesores."),
                option("enumConstants", "Constantes de enums", initial.enumConstants(), "Constantes requeridas de enumeraciones."),
                option("collectionOperations", "Operaciones de colecciones", initial.collectionOperations(),
                        "Operaciones publicas de alta y baja de elementos."));
        getChildren().addAll(heading("Tipos"), types, heading("Estructura y comportamiento"), members);
    }

    public ValidationOptions getOptions() {
        return new ValidationOptions(selected("classes"), selected("interfaces"), selected("enums"),
                selected("records"), selected("annotations"), selected("abstractness"),
                selected("relationships"), selected("attributes"), selected("constructors"),
                selected("methods"), selected("enumConstants"), selected("collectionOperations"));
    }

    private CheckBox option(String key, String text, boolean selected, String description) {
        CheckBox control = new CheckBox(text);
        control.setId("validation-option-" + key);
        control.setSelected(selected);
        control.setTooltip(new Tooltip(description));
        options.put(key, control);
        return control;
    }

    private boolean selected(String key) {
        return options.get(key).isSelected();
    }

    private static javafx.scene.control.Label heading(String text) {
        return UiComponents.label(text, List.of("validation-options-group-title"));
    }
}
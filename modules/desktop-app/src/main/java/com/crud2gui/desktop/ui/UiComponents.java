package com.crud2gui.desktop.ui;

import com.crud2gui.authenticator.AuthenticatedUser;

import javafx.css.PseudoClass;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javafx.beans.property.ObjectProperty;

/**
 * Factorias de controles y composiciones visuales reutilizables de la aplicacion.
 * Asigna clases CSS para separar la presentacion de la construccion de componentes
 * y recibe las acciones de los controladores sin ejecutar logica de autenticacion.
 *
 * <p>Los componentes deben crearse y modificarse en el hilo de JavaFX.
 * Cada llamada devuelve una instancia nueva.
 */
public final class UiComponents {

    /** Impide instanciar esta clase de utilidades con metodos estaticos. */
    private UiComponents() {
    }

    /**
     * Crea la tarjeta de cuenta compartida por los paneles.
     *
     * @param user identidad autenticada cuyo nombre se muestra
     * @param profile perfil mostrado y nombre alternativo si la identidad no contiene nombre
     * @param onSignOut accion de cierre de sesion
     * @return tarjeta con avatar, nombre, perfil y boton de cierre
     */
    public static VBox currentUserCard(AuthenticatedUser user, String profile, Runnable onSignOut) {
        SVGPath icon = new SVGPath();
        icon.setContent("M8 0 A4 4 0 1 0 8 8 A4 4 0 1 0 8 0 M0 18 C0 8 16 8 16 18 Z");
        icon.getStyleClass().add("current-user-icon");
        icon.setMouseTransparent(true);
        StackPane avatar = new StackPane(icon);
        avatar.getStyleClass().add("current-user-avatar");
        String displayName = user.displayName() == null || user.displayName().isBlank()
                ? user.username() : user.displayName();
        Label name = label(displayName == null || displayName.isBlank() ? profile : displayName,
                List.of("current-user-name"));
        Label role = label("Perfil " + profile, List.of("current-user-role"));
        VBox details = vBox(List.of("current-user-details"), name, role);
        details.setMinWidth(0);
        HBox.setHgrow(details, Priority.ALWAYS);
        HBox identity = hBox(List.of("current-user-identity"), avatar, details);
        Button signOut = actionButton("Cerrar sesión", List.of("sign-out-button"), onSignOut);
        return vBox(List.of("current-user-card"), identity, signOut);
    }

    /**
     * Anade la cuenta al pie de la barra y permite desplazarla cuando falta altura.
     *
     * @param sidebar contenedor con la cabecera y los botones de navegacion
     * @param accountCard tarjeta de la cuenta activa
     * @return barra desplazable que conserva la altura natural de sus controles
     */
    private static ScrollPane scrollableSidebar(VBox sidebar, VBox accountCard) {
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, accountCard);
        sidebar.setMinHeight(Region.USE_PREF_SIZE);
        ScrollPane scroll = new ScrollPane(sidebar);
        scroll.getStyleClass().add("dashboard-sidebar");
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(0);
        return scroll;
    }

    /**
     * Construye la barra completa compartida por todos los perfiles, con logo UNED,
     * cabecera, enlaces y cuenta. Notifica la seleccion sin construir las paginas.
     *
     * @param <S> tipo que identifica las secciones
     * @param workspace nombre del panel mostrado junto al logo
     * @param accountCard tarjeta de la cuenta activa
     * @param sections secciones disponibles, en orden de presentacion
     * @param sectionLabel funcion que obtiene el texto de cada enlace
     * @param selectedSection propiedad compartida con el dashboard para navegar
     * @return barra desplazable con cabecera, enlaces y cuenta
     * @throws NullPointerException si el recurso del logo no existe
     */
    public static <S> ScrollPane dashboardSidebar(String workspace, VBox accountCard,
            List<S> sections, Function<S, String> sectionLabel, ObjectProperty<S> selectedSection) {
        VBox branding = vBox(List.of("current-user-details"),
                label("CRUD2GUI", List.of("sidebar-brand-name")),
                label(workspace, List.of("current-user-role")));
        branding.setMinWidth(0);
        HBox.setHgrow(branding, Priority.ALWAYS);
        ImageView logo = new ImageView(new Image(Objects.requireNonNull(
                UiComponents.class.getResource("/uned.png"), "No se encontro el logo de la UNED")
                .toExternalForm()));
        logo.setFitWidth(36);
        logo.setFitHeight(36);
        logo.setPreserveRatio(true);
        logo.setSmooth(true);
        logo.setAccessibleText("UNED");
        logo.getStyleClass().add("sidebar-uned-logo");
        StackPane frame = new StackPane(logo);
        frame.getStyleClass().add("sidebar-logo-frame");
        HBox header = hBox(List.of("sidebar-branding"), frame, branding);
        return createDashboardSidebar(header, accountCard, sections, sectionLabel, selectedSection);
    }

    /**
     * Compone el contenedor y sincroniza los enlaces con la seleccion del dashboard.
     *
     * @param <S> tipo que identifica las secciones
     * @param header cabecera ya construida
     * @param accountCard tarjeta de la cuenta
     * @param sections secciones ordenadas
     * @param sectionLabel funcion que obtiene sus etiquetas
     * @param selectedSection seleccion compartida con el dashboard
     * @return barra lateral completa y desplazable
     */
    private static <S> ScrollPane createDashboardSidebar(Node header, VBox accountCard,
            List<S> sections, Function<S, String> sectionLabel, ObjectProperty<S> selectedSection) {
        VBox sidebar = vBox(List.of("sidebar-content"), header);
        PseudoClass selected = PseudoClass.getPseudoClass("selected");
        for (S section : sections) {
            Button button = new Button(sectionLabel.apply(section));
            button.getStyleClass().add("sidebar-navigation-button");
            Runnable updateSelection = () -> button.pseudoClassStateChanged(
                    selected, Objects.equals(section, selectedSection.get()));
            button.setOnAction(event -> selectedSection.set(section));
            selectedSection.addListener((observable, previous, current) -> updateSelection.run());
            updateSelection.run();
            sidebar.getChildren().add(button);
        }
        return scrollableSidebar(sidebar, accountCard);
    }

    /**
     * Crea un contenedor horizontal con la clase CSS {@code hbox} y las clases indicadas.
     * La separacion, alineacion y demas propiedades visuales se pueden definir en CSS.
     *
     * @param styleClasses clases CSS adicionales; puede ser una lista vacia, pero no {@code null}
     * @param children nodos hijos, en el orden en que se incorporan al contenedor
     * @return contenedor horizontal con los hijos y las clases proporcionados
     */
    public static HBox hBox(List<String> styleClasses, Node... children) {
        HBox box = new HBox(children);
        box.getStyleClass().add("hbox");
        box.getStyleClass().addAll(styleClasses);
        return box;
    }

    /**
     * Crea un contenedor vertical con la clase CSS {@code vbox} y las clases indicadas.
     * La separacion, alineacion y demas propiedades visuales se pueden definir en CSS.
     *
     * @param styleClasses clases CSS adicionales; puede ser una lista vacia, pero no {@code null}
     * @param children nodos hijos, en el orden en que se incorporan al contenedor
     * @return contenedor vertical con los hijos y las clases proporcionados
     */
    public static VBox vBox(List<String> styleClasses, Node... children) {
        VBox box = new VBox(children);
        box.getStyleClass().add("vbox");
        box.getStyleClass().addAll(styleClasses);
        return box;
    }

    /**
     * Crea una etiqueta para informacion secundaria con la clase CSS {@code text-muted}.
     * El color, el tamano de fuente y el ajuste de linea se definen en la hoja de estilos.
     *
     * @param value texto que muestra la etiqueta
     * @return etiqueta con el estilo de texto secundario
     */
    public static Label mutedText(String value) {
        return label(value, List.of("text-muted"));
    }

    /**
     * Crea una etiqueta y le asigna las clases CSS indicadas, ademas de {@code label}.
     *
     * @param text texto que muestra la etiqueta
     * @param styleClasses clases CSS adicionales; puede ser una lista vacia, pero no {@code null}
     * @return etiqueta con el texto y las clases proporcionados
     */
    public static Label label(String text, List<String> styleClasses) {
        Label label = new Label(text);
        label.getStyleClass().add("label");
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    /**
     * Crea un boton con la clase CSS {@code action-button} y conecta su activacion a una accion.
     * La accion se ejecuta en el hilo de JavaFX cuando se activa el boton, no al crearlo.
     *
     * @param label texto que muestra el boton
     * @param styleClasses clases CSS adicionales; puede ser una lista vacia, pero no {@code null}
     * @param action accion que ejecuta el boton; no debe ser {@code null} ni bloquear la interfaz
     * @return boton configurado con su accion y sus clases CSS
     */
    public static Button actionButton(String label, List<String> styleClasses, Runnable action) {
        Button button = new Button(label);
        button.setOnAction(event -> action.run());
        button.getStyleClass().add("action-button");
        button.getStyleClass().addAll(styleClasses);
        return button;
    }

    /**
     * Crea un indicador de progreso indeterminado con el estado inicial de visibilidad indicado.
     * Inicializa {@code visible} y {@code managed} con el mismo valor para que, si esta oculto,
     * no reserve espacio. Las dos propiedades no quedan vinculadas para cambios posteriores.
     *
     * @param visible si el indicador se muestra y participa inicialmente en el layout
     * @param styleClasses clases CSS adicionales a {@code progress-indicator};
     *                     puede ser una lista vacia, pero no {@code null}
     * @return indicador de progreso con el estado inicial y las clases proporcionados
     */
    public static ProgressIndicator progressIndicator(
            boolean visible, List<String> styleClasses) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setVisible(visible);
        progress.setManaged(visible);
        progress.getStyleClass().add("progress-indicator");
        progress.getStyleClass().addAll(styleClasses);
        return progress;
    }

    /**
     * Crea la cabecera con la seccion actual, el titulo y la descripcion.
     *
     * @param sectionName nombre de la seccion seleccionada en el menu
     * @param titleText titulo principal
     * @param subtitleText descripcion de la seccion
     * @return contenedor vertical de la cabecera
     */
    public static VBox sectionHeader(String sectionName, String titleText, String subtitleText) {
        Label breadCrumb = label("CRUD2GUI  /  " + sectionName, List.of("section-breadcrumb"));
        Label title = label(titleText, List.of("section-title"));
        Label subtitle = label(subtitleText, List.of("section-subtitle"));
        return new VBox(6, breadCrumb, title, subtitle);
    }

    /**
     * Crea una cabecera con los textos a la izquierda y un boton de accion a la derecha.
     * Para una cabecera sin boton, utiliza la sobrecarga de tres parametros.
     * La vista que llama al metodo configura el texto, el estilo y la accion del boton.
     *
     * @param sectionName nombre que aparece en la ruta de la cabecera
     * @param titleText titulo principal
     * @param subtitleText descripcion de la seccion
     * @param actionButton boton previamente configurado; no debe ser {@code null}
     * @return contenedor horizontal con los textos y el boton
     * @throws NullPointerException si el boton es {@code null}
     */
    public static HBox sectionHeader(
            String sectionName, String titleText, String subtitleText, Button actionButton) {
        Objects.requireNonNull(actionButton, "actionButton");
        VBox heading = sectionHeader(sectionName, titleText, subtitleText);
        heading.setMinWidth(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actionButton.setMinWidth(Region.USE_PREF_SIZE);
        return hBox(List.of("section-header"), heading, spacer, actionButton);
    }
}

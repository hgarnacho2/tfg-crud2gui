package com.crud2gui.desktop.login;

import com.crud2gui.authenticator.AuthenticatedUser;
import com.crud2gui.desktop.ui.UiComponents;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * Vista JavaFX del acceso a la aplicacion, implementada como un contenedor {@link VBox}.
 * Presenta el formulario de login, los mensajes de estado y el navegador integrado de autorizacion.
 * No representa la ventana ({@code Stage}) ni realiza la autenticacion: delega la accion de acceso
 * en {@link LoginController}, sin conocer los detalles de MSAL ni la configuracion de Entra ID.
 *
 * <p>Los estilos se definen mediante clases CSS. Salvo {@link #showEmbeddedBrowser(URL)},
 * que programa su actualizacion con {@link Platform#runLater(Runnable)}, la creacion y las
 * modificaciones de esta vista deben realizarse en el hilo de JavaFX.
 */
public final class LoginView extends VBox {

    private final Label status;
    private final Label role;
    private Runnable onSignIn;
    private final Button signInButton;
    private final ProgressIndicator progress;
    private final VBox loginContainer;

    /**
     * Construye el formulario con los controles de acceso y salida, el indicador de progreso
     * inicialmente oculto y los mensajes de estado. Configura el acceso como boton predeterminado
     * y asigna las clases CSS a los controles y contenedores.
     *
     * <p>La accion de acceso debe registrarse con {@link #setOnSignIn(Runnable)} antes de activar
     * el boton; de lo contrario, su manejador lanza {@link NullPointerException}.
     */
    public LoginView() {
        //Inicialización de los componentes de clase (visibles desde cualquier método de LoginView)
        status = UiComponents.label("No has iniciado sesión.", List.of("login-status"));

        role = UiComponents.label("", List.of("login-role"));

        progress = UiComponents.progressIndicator(false, List.of("login-progress"));        

        signInButton = UiComponents.actionButton(
            "Iniciar sesión con Microsoft",
            List.of("login-button"),
            () -> Objects.requireNonNull(onSignIn, "No se ha configurado la accion de login").run());

        signInButton.setDefaultButton(true);

        //Inicializacion de los componentes locales de la vista de login
        Label brand = UiComponents.label("CRUD2GUI", List.of("login-brand"));

        Label heading = UiComponents.label("Acceso", List.of("login-heading"));

        Label description = UiComponents.label(
                "Inicia sesión con tu cuenta de la aplicación.", List.of("login-description"));

        Button exitButton = UiComponents.actionButton("Salir", List.of("exit-button"), Platform::exit);

        HBox statusRow = UiComponents.hBox(List.of("login-status-row"), progress, status);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        loginContainer = UiComponents.vBox(List.of("login-container"),
                brand, heading, description, signInButton, exitButton, statusRow, role, spacer);

        //Establecemos la clase y los estilos para el Stage>Scene>loginView (this)
        getStyleClass().add("login-view");
                getChildren().add(loginContainer);
        }

        /**
         * Sustituye el formulario por un {@link WebView} que carga la pagina de autorizacion.
         * Activa el estilo sin margen interior y permite al navegador ocupar el espacio disponible.
         * La actualizacion se programa en el hilo de JavaFX, por lo que puede llamarse desde otro hilo.
         *
         * @param authorizationUrl URL de autorizacion proporcionada por el servicio de autenticacion;
         *                         no debe ser {@code null}
         */
        public void showEmbeddedBrowser(URL authorizationUrl) {
                Platform.runLater(() -> {
                        WebView webView = new WebView();
                        webView.setContextMenuEnabled(false);
                        webView.setMinSize(0, 0);
                        VBox.setVgrow(webView, Priority.ALWAYS);
                        if (!getStyleClass().contains("embedded-browser")) {
                                getStyleClass().add("embedded-browser");
                        }
                        getChildren().setAll(webView);
                        webView.getEngine().load(authorizationUrl.toExternalForm());
                });
        }

        /**
         * Registra o reemplaza la accion que ejecuta el boton de acceso al activarse.
         *
         * @param action accion de inicio de sesion definida por el controlador
         * @throws NullPointerException si {@code action} es {@code null}
         */
        void setOnSignIn(Runnable action) {
                onSignIn = Objects.requireNonNull(action, "action");
        }

        /**
         * Indica que hay una autenticacion en curso: deshabilita nuevos intentos, limpia el aviso
         * de roles y muestra el indicador de progreso, reservando espacio para el en la fila de estado.
         */
        void showBusy() {
                signInButton.setDisable(true);
                status.setText("Iniciando autenticación...");
                role.setText("");
                progress.setVisible(true);
                progress.setManaged(true);
        }

        /**
         * Muestra que no se pudo cargar la configuracion de autenticacion y deshabilita el acceso
         * para evitar intentos con una configuracion no disponible.
         */
        public void showConfigurationError() {
                signInButton.setDisable(true);
                status.setText("No se pudo cargar la configuración de autenticación.");
        }

        /**
         * Restaura el formulario tras un fallo de autenticacion, oculta el indicador sin reservar
         * espacio, muestra el mensaje de error y habilita el boton para volver a intentarlo.
         */
        void showFailure() {
                restoreLoginContainer();
                signInButton.setDisable(false);
                progress.setVisible(false);
                progress.setManaged(false);
                status.setText("No se pudo completar el inicio de sesión.");
                role.setText("");
        }

        /**
         * Restablece los controles del formulario tras una autenticacion correcta y muestra un
         * aviso si la cuenta no tiene ninguno de los roles admitidos.
         * No realiza la navegacion: el controlador y la aplicacion abren el panel correspondiente
         * cuando la cuenta tiene el rol Alumno o Docente.
         *
         * @param user identidad autenticada cuyos roles se consultan; no debe ser {@code null}
         * @throws NullPointerException si {@code user} es {@code null}
         */
        void handleAuthenticationResult(AuthenticatedUser user) {
                restoreLoginContainer();
                signInButton.setDisable(false);
                progress.setVisible(false);
                progress.setManaged(false);

                if (!user.isAlumno() && !user.isDocente()) {
                        status.setText("Cuenta autenticada, sin rol de la aplicación");
                        role.setText("Por favor, solicita al administrador que te asigne el rol de Alumno o Docente.");
                }
        }

        /**
         * Vuelve a mostrar el contenedor del formulario si fue sustituido por el navegador integrado
         * y retira la clase CSS que elimina el margen interior. No modifica los mensajes ni el
         * estado de los controles.
         */
        private void restoreLoginContainer() {
                if (!getChildren().contains(loginContainer)) {
                        getStyleClass().remove("embedded-browser");
                        getChildren().setAll(loginContainer);
                }
        }
}

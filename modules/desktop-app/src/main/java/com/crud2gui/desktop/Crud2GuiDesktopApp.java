package com.crud2gui.desktop;

import com.crud2gui.authenticator.entraid.AuthenticatorConfig;
import com.crud2gui.authenticator.entraid.EntraIdAuthenticator;
import com.crud2gui.desktop.login.LoginController;
import com.crud2gui.desktop.login.LoginView;
import com.crud2gui.desktop.login.LogoutView;
import com.crud2gui.desktop.student.StudentWorkflowContext;
import com.crud2gui.desktop.student.StudentDashboardView;
import com.crud2gui.desktop.teacher.TeacherWorkflowContext;
import com.crud2gui.desktop.teacher.TeacherDashboardView;
import com.crud2gui.desktop.ui.WorkflowContextChoiceView;
import com.crud2gui.authenticator.entraid.AuthenticatedUser;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada JavaFX de CRUD2GUI.
 * Configura la ventana, coordina el acceso y dirige al usuario autenticado al panel
 * de su perfil, incluyendo la selección del contexto de trabajo.
 */
public final class Crud2GuiDesktopApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Crud2GuiDesktopApp.class);

    /**
     * Construye y muestra la ventana principal con el formulario de acceso.
     * Carga la hoja de estilos, configura la integración de autenticación y maximiza
     * la ventana para iniciar la aplicación.
     *
     * @param stage ventana principal proporcionada por el ciclo de vida de JavaFX
     * @throws NullPointerException si no se encuentra la hoja de estilos de la aplicación
     */
    @Override
    public void start(Stage stage) {
        LoginView loginView = new LoginView();
        Scene scene = new Scene(loginView, 1120, 760);
        scene.getStylesheets().add(Objects.requireNonNull(
                Crud2GuiDesktopApp.class.getResource("/styles/app.css"),
                "No se encontro la hoja de estilos /styles/app.css").toExternalForm());
        configureLogin(stage, scene, loginView);

        stage.setTitle("CRUD2GUI | Acceso");
        stage.setMinWidth(760);
        stage.setMinHeight(640);
        var iconResource = Crud2GuiDesktopApp.class.getResource("/uned.png");
        if (iconResource != null) stage.getIcons().add(new Image(iconResource.toExternalForm()));
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    /**
     * Inicia el ciclo de vida JavaFX de la aplicación.
     *
     * @param args argumentos de línea de comandos entregados por la JVM; no se interpretan aquí
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
    * Crea el cliente de autenticación y conecta el resultado del login con el área autenticada.
    * Si la configuración de Entra ID no se puede cargar, deja visible el formulario con
    * un mensaje de error en lugar de iniciar el flujo de acceso.
     *
    * @param stage ventana cuyo título se actualiza al cambiar de estado
    * @param scene escena cuya raíz se sustituye entre login, selección de contexto y panel
    * @param loginView formulario que inicia la autenticación y presenta sus estados
     */
    private void configureLogin(Stage stage, Scene scene, LoginView loginView) {
        try {
            AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
            EntraIdAuthenticator authenticator = new EntraIdAuthenticator(config, loginView::showEmbeddedBrowser);
            new LoginController(loginView, authenticator,
                    user -> showAuthenticatedArea(stage, scene, authenticator, config, user));
        } catch (RuntimeException | java.net.MalformedURLException exception) {
            loginView.showConfigurationError();
            System.err.println("Error al configurar Entra ID: " + exception.getMessage());
        }
    }

    /**
     * Selecciona el workflow del perfil autenticado y prepara su panel.
     * Si hay un contexto guardado, muestra primero la pantalla para conservarlo o reiniciarlo;
     * si no lo hay, abre directamente el panel correspondiente. Si la cuenta tiene ambos roles,
     * se prioriza el perfil docente.
     *
     * @param stage ventana principal de la aplicación
     * @param scene escena donde se presenta el panel o la selección de contexto
     * @param authenticator cliente de Entra ID usado también para cerrar la sesión
     * @param config configuración de autenticación necesaria para el cierre de sesión
     * @param user identidad autenticada cuyos roles e identificador determinan el perfil y workflow
     */
    private void showAuthenticatedArea(Stage stage, Scene scene, EntraIdAuthenticator authenticator,
                                      AuthenticatorConfig config, AuthenticatedUser user) {
        if (user.isDocente()) {
            TeacherWorkflowContext workflow = new TeacherWorkflowContext(user.objectId());
            Runnable openDashboard = () -> {
                stage.setTitle("CRUD2GUI | Panel docente");
                scene.setRoot(new TeacherDashboardView(user,
                        () -> signOut(stage, scene, authenticator, config), workflow));
            };
            showContextChoice(stage, scene, "docente", workflow.hasSavedContext(), workflow::reset, openDashboard);
        } else if (user.isAlumno()) {
            StudentWorkflowContext workflow = new StudentWorkflowContext(user.objectId());
            Runnable openDashboard = () -> {
                stage.setTitle("CRUD2GUI | Panel de alumno");
                scene.setRoot(new StudentDashboardView(user,
                        () -> signOut(stage, scene, authenticator, config), workflow));
            };
            showContextChoice(stage, scene, "alumno", workflow.hasSavedContext(), workflow::reset, openDashboard);
        }
    }

    /**
     * Presenta la elección de contexto guardado o ejecuta directamente la continuación.
     * Al reiniciar, limpia las referencias persistidas antes de abrir el panel; no elimina
     * los archivos a los que apuntaban.
     *
     * @param stage ventana principal cuyo título refleja la pantalla visible
     * @param scene escena donde se instala la pantalla de elección o el panel
     * @param profile nombre del perfil autenticado que se mostrará en la pantalla
     * @param hasSavedContext indica si existe estado anterior que requiera una decisión
     * @param resetContext acción que limpia el estado persistido del workflow
     * @param continueWithContext acción que abre el panel con el workflow actual
     */
    private void showContextChoice(Stage stage, Scene scene, String profile, boolean hasSavedContext,
                                   Runnable resetContext, Runnable continueWithContext) {
        if (!hasSavedContext) {
            continueWithContext.run();
            return;
        }
        stage.setTitle("CRUD2GUI | Contexto de trabajo");
        scene.setRoot(new WorkflowContextChoiceView(profile, continueWithContext, () -> {
            resetContext.run();
            continueWithContext.run();
        }));
    }

    /**
    * Retira el panel autenticado, limpia la caché MSAL y solicita el cierre remoto a Microsoft.
    * Si falla la limpieza local, vuelve al login; si se completa, presenta la vista que espera
    * la redirección de cierre remoto.
     *
    * @param stage ventana principal cuyo título refleja el estado del cierre
    * @param scene escena donde se reemplaza el panel por el estado de cierre o la vista de retorno
    * @param authenticator cliente MSAL cuya caché se limpia y que proporciona la URI de logout
    * @param config configuración del tenant y URI de retorno registrada en Entra ID
     */
    private void signOut(Stage stage, Scene scene, EntraIdAuthenticator authenticator,
                         AuthenticatorConfig config) {
        stage.setTitle("CRUD2GUI | Cerrando sesion");
        scene.setRoot(new VBox(new Label("Cerrando sesion...")));
        authenticator.clearAccounts().whenComplete((ignored, cacheFailure) -> Platform.runLater(() -> {
            if (cacheFailure != null) {
                returnToLogin(stage, scene, cacheFailure);
                return;
            }
            CompletableFuture<Void> logout = new CompletableFuture<>();
            logout.whenComplete((result, failure) ->
                    Platform.runLater(() -> returnToLogin(stage, scene, failure)));
            scene.setRoot(new LogoutView(authenticator.logoutUri(), config.redirectUri(), logout));
        }));
    }

    /**
    * Crea un formulario de acceso nuevo y configura una autenticación nueva.
    * Si el cierre remoto no se confirmó, muestra una advertencia después de restablecer el login.
     *
    * @param stage ventana principal cuyo título vuelve al estado de acceso
    * @param scene escena donde se instala el nuevo formulario de login
    * @param logoutFailure causa del cierre no confirmado, o {@code null} si Microsoft confirmó el retorno
     */
    private void returnToLogin(Stage stage, Scene scene, Throwable logoutFailure) {
        LoginView login = new LoginView();
        scene.setRoot(login);
        stage.setTitle("CRUD2GUI | Acceso");
        configureLogin(stage, scene, login);
        if (logoutFailure != null) {
            LOGGER.error("LOGOUT_ERROR | No se pudo confirmar el cierre de sesion de Microsoft.", logoutFailure);
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.initOwner(stage);
            alert.setHeaderText("Se ha cerrado CRUD2GUI, pero no se pudo confirmar el cierre de Microsoft.");
            alert.setContentText("El siguiente acceso solicitara una nueva autenticacion. "
                    + "La sesion de Microsoft podria seguir abierta.");
            alert.show();
        } else {
            LOGGER.info("LOGOUT_OK | Sesion de CRUD2GUI y Microsoft cerrada.");
        }
    }


    
}

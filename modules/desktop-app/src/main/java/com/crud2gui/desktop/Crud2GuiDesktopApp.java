package com.crud2gui.desktop;

import com.crud2gui.authenticator.AuthenticatorConfig;
import com.crud2gui.authenticator.EntraIdAuthenticator;
import com.crud2gui.desktop.login.LoginController;
import com.crud2gui.desktop.login.LoginView;
import com.crud2gui.desktop.login.LogoutView;
import com.crud2gui.desktop.student.StudentDashboardView;
import com.crud2gui.desktop.teacher.TeacherDashboardView;
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

/** Ventana JavaFX de acceso de CRUD2GUI con autenticacion embebida. */
public final class Crud2GuiDesktopApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Crud2GuiDesktopApp.class);

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
     * Inicia la aplicacion JavaFX con las opciones de JVM definidas en la configuracion de arranque.
     *
     * @param args argumentos de la aplicacion
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Conecta el acceso con la navegacion al panel correspondiente.
     *
     * @param stage ventana de la aplicacion
     * @param scene escena compartida por el acceso y los paneles
     * @param loginView formulario que recibira los resultados de autenticacion
     */
    private void configureLogin(Stage stage, Scene scene, LoginView loginView) {
        try {
            AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
            EntraIdAuthenticator authenticator = new EntraIdAuthenticator(config, loginView::showEmbeddedBrowser);
            new LoginController(loginView, authenticator, user -> {
                if (user.isDocente()) {
                    stage.setTitle("CRUD2GUI | Panel docente");
                    scene.setRoot(new TeacherDashboardView(user,
                            () -> signOut(stage, scene, authenticator, config)));
                } else if (user.isAlumno()) {
                    stage.setTitle("CRUD2GUI | Panel de alumno");
                    scene.setRoot(new StudentDashboardView(user,
                            () -> signOut(stage, scene, authenticator, config)));
                }
            });
        } catch (RuntimeException | java.net.MalformedURLException exception) {
            loginView.showConfigurationError();
            System.err.println("Error al configurar Entra ID: " + exception.getMessage());
        }
    }

    /**
     * Retira el panel autenticado, limpia MSAL y solicita el cierre remoto a Microsoft.
     *
     * @param stage ventana cuya cabecera muestra el estado del cierre
     * @param scene escena en la que se presenta el cierre integrado
     * @param authenticator cliente que conserva la cache de la sesion saliente
     * @param config configuracion del tenant y del retorno registrado
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
     * Crea un acceso nuevo sin reutilizar el cliente autenticado y avisa de cierres no confirmados.
     *
     * @param stage ventana principal
     * @param scene escena que vuelve a mostrar el acceso
     * @param logoutFailure error del cierre, o {@code null} si Microsoft confirmo el retorno
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

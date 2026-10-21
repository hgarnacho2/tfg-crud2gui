package com.crud2gui.desktop;

import com.crud2gui.authenticator.AuthenticatorConfig;
import com.crud2gui.authenticator.EntraIdAuthenticator;
import com.crud2gui.desktop.login.LoginController;
import com.crud2gui.desktop.login.LoginView;
import com.crud2gui.desktop.student.StudentDashboardView;
import com.crud2gui.desktop.teacher.TeacherDashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/** Ventana JavaFX de acceso de CRUD2GUI con autenticacion embebida. */
public final class Crud2GuiDesktopApp extends Application {

    @Override
    public void start(Stage stage) {
        LoginView loginView = new LoginView();
        Scene scene = new Scene(loginView, 1120, 760);
        try {
            AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
            EntraIdAuthenticator authenticator = new EntraIdAuthenticator(config, loginView::showEmbeddedBrowser);
            new LoginController(loginView, authenticator, user -> {
                if (user.isDocente()) {
                    stage.setTitle("CRUD2GUI | Panel docente");
                    scene.setRoot(new TeacherDashboardView(user));
                } else if (user.isAlumno()) {
                    stage.setTitle("CRUD2GUI | Panel de alumno");
                    scene.setRoot(new StudentDashboardView(user));
                }
            });
        } catch (RuntimeException | java.net.MalformedURLException exception) {
            loginView.showConfigurationError();
            System.err.println("Error al configurar Entra ID: " + exception.getMessage());
        }

        stage.setTitle("CRUD2GUI | Acceso");
        stage.setMinWidth(760);
        stage.setMinHeight(640);
        var iconResource = Crud2GuiDesktopApp.class.getResource("/uned.png");
        if (iconResource != null) stage.getIcons().add(new Image(iconResource.toExternalForm()));
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

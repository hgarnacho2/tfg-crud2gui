package com.crud2gui.desktop;

import com.crud2gui.authenticator.AuthenticatorConfig;
import com.crud2gui.authenticator.EntraIdAuthenticator;
import com.crud2gui.desktop.login.LoginController;
import com.crud2gui.desktop.login.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Ventana JavaFX de acceso de CRUD2GUI. El login interactivo se delega a MSAL y al navegador del sistema. */
public final class Crud2GuiDesktopApp extends Application {

    @Override
    public void start(Stage stage) {
                LoginView loginView = new LoginView();
                try {
                        new LoginController(loginView,
                                        new EntraIdAuthenticator(AuthenticatorConfig.fromEnvironment()));
                } catch (RuntimeException | java.net.MalformedURLException exception) {
                        loginView.showConfigurationError();
                        System.err.println("Error al configurar Entra ID: " + exception.getMessage());
                }

                stage.setTitle("CRUD2GUI | Acceso");
                stage.setMinWidth(420);
                stage.setMinHeight(440);
                stage.setScene(new Scene(loginView, 520, 560));
                stage.show();
        }

        public static void main(String[] args) {
                launch(args);
        }
}

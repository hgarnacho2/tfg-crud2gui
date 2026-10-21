package com.crud2gui.desktop;

import com.crud2gui.authenticator.AuthenticatorConfig;
import com.crud2gui.authenticator.EntraIdAuthenticator;
import com.crud2gui.desktop.login.LoginController;
import com.crud2gui.desktop.login.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Ventana JavaFX de acceso de CRUD2GUI con navegador de autenticacion configurable. */
public final class Crud2GuiDesktopApp extends Application {

    @Override
    public void start(Stage stage) {
                LoginView loginView = new LoginView();
                boolean embeddedBrowser = false;
                try {
                        AuthenticatorConfig config = AuthenticatorConfig.fromEnvironment();
                        embeddedBrowser = config.browserMode() == com.crud2gui.authenticator.AuthBrowserMode.EMBEDDED;
                        if (embeddedBrowser) loginView.setEmbeddedBrowserMode();
                        EntraIdAuthenticator authenticator = embeddedBrowser
                                ? new EntraIdAuthenticator(config, loginView::showEmbeddedBrowser)
                                : new EntraIdAuthenticator(config);
                        new LoginController(loginView, authenticator);
                } catch (RuntimeException | java.net.MalformedURLException exception) {
                        loginView.showConfigurationError();
                        System.err.println("Error al configurar Entra ID: " + exception.getMessage());
                }

                stage.setTitle("CRUD2GUI | Acceso");
                stage.setMinWidth(embeddedBrowser ? 760 : 420);
                stage.setMinHeight(embeddedBrowser ? 640 : 440);
                stage.setScene(new Scene(loginView, embeddedBrowser ? 960 : 520, embeddedBrowser ? 720 : 560));
                stage.show();
        }

        public static void main(String[] args) {
                launch(args);
        }
}

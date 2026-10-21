package com.crud2gui.desktop.login;

import com.crud2gui.authenticator.AuthenticatedUser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Vista JavaFX del proceso de login; no contiene detalles de MSAL ni configuración Entra. */
public final class LoginView extends VBox {

    private final Label status = new Label("No has iniciado sesión.");
    private final Label role = new Label();
    private final Button signInButton = new Button("Iniciar sesión con Microsoft");
    private final ProgressIndicator progress = new ProgressIndicator();

    public LoginView() {
        Label brand = new Label("CRUD2GUI");
        brand.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #087e65;");

        Label heading = new Label("Acceso");
        heading.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #17212b;");

        Label description = new Label("Inicia sesión con tu cuenta de la aplicación.");
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #52616b;");
        description.setWrapText(true);

        signInButton.setMaxWidth(Double.MAX_VALUE);
        signInButton.setDefaultButton(true);
        signInButton.setStyle("-fx-background-color: #087e65; -fx-text-fill: white; -fx-font-size: 14px; "
                + "-fx-font-weight: bold; -fx-padding: 12px 16px; -fx-background-radius: 4px;");

        progress.setMaxSize(22, 22);
        progress.setVisible(false);
        progress.setManaged(false);

        HBox statusRow = new HBox(10, progress, status);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        status.setWrapText(true);
        status.setStyle("-fx-text-fill: #17212b; -fx-font-size: 14px;");
        role.setStyle("-fx-text-fill: #087e65; -fx-font-size: 16px; -fx-font-weight: bold;");
        role.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label browserNote = new Label("La autenticación se completará en el navegador del sistema.");
        browserNote.setStyle("-fx-text-fill: #697780; -fx-font-size: 12px;");
        browserNote.setWrapText(true);

        VBox content = new VBox(18, brand, heading, description, signInButton, statusRow, role, spacer, browserNote);
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(34));
        content.setMaxWidth(430);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8px; "
                + "-fx-effect: dropshadow(gaussian, rgba(20, 45, 55, 0.12), 22, 0.12, 0, 6);");

        setAlignment(Pos.CENTER);
        setPadding(new Insets(28));
        setStyle("-fx-background-color: #f1f6f4; -fx-font-family: 'Segoe UI';");
                getChildren().add(content);
        }

        void setOnSignIn(Runnable action) {
                signInButton.setOnAction(event -> action.run());
        }

        void showBusy() {
                signInButton.setDisable(true);
                status.setText("Esperando autenticación en el navegador...");
                role.setText("");
                progress.setVisible(true);
                progress.setManaged(true);
        }

        public void showConfigurationError() {
                signInButton.setDisable(true);
                status.setText("No se pudo cargar la configuración de autenticación.");
        }

        void showFailure() {
                signInButton.setDisable(false);
                progress.setVisible(false);
                progress.setManaged(false);
                status.setText("No se pudo completar el inicio de sesión.");
                role.setText("");
        }

        void showAuthenticated(AuthenticatedUser user) {
                signInButton.setDisable(false);
                progress.setVisible(false);
                progress.setManaged(false);

                if (user.isAlumno() && user.isDocente()) {
                        status.setText("Autenticación correcta");
                        role.setText("Perfiles: Alumno y Docente");
                } else if (user.isAlumno()) {
                        status.setText("Autenticación correcta");
                        role.setText("Perfil: Alumno");
                } else if (user.isDocente()) {
                        status.setText("Autenticación correcta");
                        role.setText("Perfil: Docente");
                } else {
                        status.setText("Cuenta autenticada, sin rol de la aplicación");
                        role.setText("Por favor, solicita al administrador que te asigne el rol de Alumno o Docente.");
                }
        }
}

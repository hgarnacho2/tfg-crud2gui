package com.crud2gui.desktop.login;

import com.crud2gui.authenticator.AuthenticatedUser;
import com.crud2gui.authenticator.EntraIdAuthenticator;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;

/** Coordina los eventos de la vista Login con el servicio de autenticacion. */
public final class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private final LoginView view;
    private final EntraIdAuthenticator authenticator;

    public LoginController(LoginView view, EntraIdAuthenticator authenticator) {
        this.view = view;
        this.authenticator = authenticator;
        view.setOnSignIn(this::signIn);
    }

    private void signIn() {
        view.showBusy();
        authenticator.authenticate().whenComplete((user, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                        ? failure.getCause() : failure;
                view.showFailure();
                LOGGER.error("LOGIN_ERROR | {}", cause.getMessage());
                return;
            }
            showResult(user);
        }));
    }

    private void showResult(AuthenticatedUser user) {
        view.showAuthenticated(user);
        if (user.roles().isEmpty()) {
            LOGGER.warn("LOGIN_OK | Cuenta autenticada sin rol de aplicacion asignado.");
        } else {
            LOGGER.info("LOGIN_OK | Perfil: {}", displayRoles(user));
        }
    }

    private static String displayRoles(AuthenticatedUser user) {
        if (user.isAlumno() && user.isDocente()) return "Alumno, Docente";
        if (user.isAlumno()) return "Alumno";
        if (user.isDocente()) return "Docente";
        return String.join(", ", user.roles());
    }
}

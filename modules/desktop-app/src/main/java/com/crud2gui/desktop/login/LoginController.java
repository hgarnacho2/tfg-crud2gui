package com.crud2gui.desktop.login;

import com.crud2gui.authenticator.AuthenticatedUser;
import com.crud2gui.authenticator.EntraIdAuthenticator;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Coordina la vista de acceso {@link LoginView} con el servicio {@link EntraIdAuthenticator}.
 * Conecta el boton de acceso con el inicio de la autenticacion, actualiza los estados visuales
 * y notifica a la aplicacion cuando se obtiene una identidad autenticada.
 *
 * <p>No construye los controles ni decide que panel abrir: la vista gestiona los controles y
 * la accion {@code onAuthenticated} proporcionada por la aplicacion gestiona la navegacion.
 * La construccion y el inicio del login deben realizarse en el hilo de JavaFX; los resultados
 * asincronos se trasladan a ese hilo mediante {@link Platform#runLater(Runnable)}.
 */
public final class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private final LoginView view;
    private final EntraIdAuthenticator authenticator;
    private final Consumer<AuthenticatedUser> onAuthenticated;

    /**
     * Inicializa el controlador y registra {@link #signIn()} como accion del boton de acceso.
     * Crear el controlador no inicia automaticamente la autenticacion.
     *
     * @param view vista cuyos eventos y mensajes de estado se coordinan
     * @param authenticator servicio que ejecuta el flujo de autenticacion asincrono
     * @param onAuthenticated accion que recibe la identidad autenticada, incluso si no tiene
     *                        un rol admitido; la aplicacion decide si corresponde cambiar de vista
     * @throws NullPointerException si cualquiera de los parametros es {@code null}
     */
    public LoginController(LoginView view, EntraIdAuthenticator authenticator,
                           Consumer<AuthenticatedUser> onAuthenticated) {
        this.view = Objects.requireNonNull(view, "view");
        this.authenticator = Objects.requireNonNull(authenticator, "authenticator");
        this.onAuthenticated = Objects.requireNonNull(onAuthenticated, "onAuthenticated");
        view.setOnSignIn(this::signIn);
    }

    /**
     * Muestra el estado de espera e inicia la autenticacion sin bloquear el hilo de interfaz.
     * Al completarse el futuro, programa en el hilo de JavaFX la presentacion del resultado
     * o del error. Si el fallo esta envuelto en una {@link CompletionException}, utiliza su
     * causa para registrar el mensaje de error.
     */
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

    /**
     * Restablece la vista tras una autenticacion correcta, entrega la identidad a la accion
     * de la aplicacion y registra los roles disponibles o la ausencia de roles.
     * Se ejecuta en el hilo de JavaFX.
     *
     * @param user identidad devuelta por el servicio de autenticacion; no debe ser {@code null}
     */
    private void showResult(AuthenticatedUser user) {
        view.handleAuthenticationResult(user);
        onAuthenticated.accept(user);
        if (user.roles().isEmpty()) {
            LOGGER.warn("LOGIN_OK | Cuenta autenticada sin rol de aplicacion asignado.");
        } else {
            LOGGER.info("LOGIN_OK | Perfil: {}", displayRoles(user));
        }
    }

    /**
     * Obtiene una representacion legible de los roles para el registro de diagnostico.
     * Da prioridad a los nombres Alumno y Docente; si no existe ninguno de ellos,
     * concatena los roles recibidos separados por comas.
     *
     * @param user identidad autenticada cuyos roles se consultan; no debe ser {@code null}
     * @return nombres de los perfiles reconocidos o lista de los demas roles;
     *         una cadena vacia si la identidad no tiene roles
     */
    private static String displayRoles(AuthenticatedUser user) {
        if (user.isAlumno() && user.isDocente()) return "Alumno, Docente";
        if (user.isAlumno()) return "Alumno";
        if (user.isDocente()) return "Docente";
        return String.join(", ", user.roles());
    }
}

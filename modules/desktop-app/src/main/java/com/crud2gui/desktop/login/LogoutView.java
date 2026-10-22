package com.crud2gui.desktop.login;

import com.crud2gui.desktop.ui.UiComponents;
import javafx.animation.PauseTransition;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.net.URI;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Muestra el cierre de Microsoft en el navegador integrado y espera su redireccion de retorno.
 * Un error de red o el tiempo agotado no se interpretan como cierre correcto.
 */
public final class LogoutView extends VBox {

    /**
     * Abre el endpoint de Microsoft. Debe construirse en el hilo de JavaFX.
     *
     * @param logoutUri URL de cierre de Microsoft
     * @param redirectUri retorno registrado que indica que Microsoft ha terminado el cierre
     * @param completion futuro que recibe el resultado del cierre remoto
     */
    public LogoutView(URI logoutUri, URI redirectUri, CompletableFuture<Void> completion) {
        WebView browser = new WebView();
        browser.setContextMenuEnabled(false);
        VBox.setVgrow(browser, Priority.ALWAYS);
        getStyleClass().add("logout-view");
        Label message = UiComponents.label("Cerrando la sesion de Microsoft...", List.of("login-status"));
        getChildren().addAll(message, browser);

        PauseTransition timeout = new PauseTransition(Duration.seconds(120));
        timeout.setOnFinished(event -> completion.completeExceptionally(
                new TimeoutException("Microsoft no confirmo el cierre de sesion en 120 segundos.")));
        browser.getEngine().locationProperty().addListener((observable, previous, location) -> {
            if (matchesRedirect(redirectUri, URI.create(location))) {
                completion.complete(null);
            }
        });
        browser.getEngine().getLoadWorker().stateProperty().addListener((observable, previous, state) -> {
            if (state == Worker.State.FAILED && !completion.isDone()) {
                completion.completeExceptionally(new IllegalStateException(
                        "No se pudo cargar el cierre de Microsoft.",
                        browser.getEngine().getLoadWorker().getException()));
            }
        });
        completion.whenComplete((result, failure) -> javafx.application.Platform.runLater(() -> {
            timeout.stop();
            browser.getEngine().getLoadWorker().cancel();
            browser.getEngine().load(null);
        }));
        timeout.play();
        browser.getEngine().load(logoutUri.toString());
    }

    /**
     * Compara el retorno completo, normalizando la ruta vacia y el puerto HTTP por defecto.
     *
     * @param expected retorno registrado en Entra
     * @param actual destino de la navegacion observada
     * @return si ambas URI representan el mismo retorno, sin aceptar otros hosts, rutas o parametros
     */
    static boolean matchesRedirect(URI expected, URI actual) {
        return "http".equalsIgnoreCase(actual.getScheme())
                && expected.getHost().equalsIgnoreCase(actual.getHost())
                && (expected.getPort() == -1 ? 80 : expected.getPort())
                    == (actual.getPort() == -1 ? 80 : actual.getPort())
                && normalizedPath(expected).equals(normalizedPath(actual))
                && Objects.equals(expected.getRawQuery(), actual.getRawQuery())
                && actual.getRawUserInfo() == null
                && actual.getRawFragment() == null;
    }

    private static String normalizedPath(URI uri) {
        return uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
    }
}

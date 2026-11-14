package com.crud2gui.desktop.ui;

import javafx.geometry.Insets;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Estructura visual común de las secciones de ambos perfiles.
 * Mantiene la cabecera como primer hijo; las subclases añaden su contenido después.
 * Debe construirse y modificarse en el hilo de JavaFX.
 */
public abstract class Section extends VBox {

    private static final double SPACING = 20;
    private static final Insets PADDING = new Insets(32, 38, 36, 38);

    /** Estado visual de un mensaje de error. */
    protected static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    /** Estado visual de un elemento seleccionado. */
    protected static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    /** Estado visual de un elemento completado. */
    protected static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");

    /** Registro compartido por herencia que identifica la clase concreta de la sección. */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Crea una sección con cabecera sin acción.
     *
     * @param breadcrumb nombre de la sección en la ruta de navegación
     * @param title título principal
     * @param description descripción de la sección
     */
    protected Section(String breadcrumb, String title, String description) {
        this(UiComponents.sectionHeader(breadcrumb, title, description));
    }

    /**
     * Crea una sección con una acción en la cabecera.
     *
     * @param breadcrumb nombre de la sección en la ruta de navegación
     * @param title título principal
     * @param description descripción de la sección
     * @param action botón configurado por la subclase, no nulo
     */
    protected Section(String breadcrumb, String title, String description, Button action) {
        this(UiComponents.sectionHeader(breadcrumb, title, description, action));
    }

    /**
     * Aplica la distribución compartida e incorpora la cabecera.
     *
     * @param header cabecera construida por las factorías de interfaz
     */
    private Section(Node header) {
        super(SPACING);
        setPadding(PADDING);
        getChildren().add(header);
    }

    /**
     * Actualiza un mensaje y su estado visual sin imponer IDs ni estilos de sección.
     *
     * @param status etiqueta que recibe el mensaje
     * @param message texto que sustituye al anterior
     * @param error si debe activarse el estado CSS de error
     */
    protected final void showStatus(Label status, String message, boolean error) {
        status.setText(message);
        status.pseudoClassStateChanged(ERROR, error);
    }

    /** Muestra el panel elegido y actualiza el estado visual de los botones del flujo. */
    protected final void showStepPanels(List<? extends Node> panels, List<Button> steps, int index) {
        for (int panelIndex = 0; panelIndex < panels.size(); panelIndex++) {
            boolean selected = panelIndex == index;
            panels.get(panelIndex).setVisible(selected);
            panels.get(panelIndex).setManaged(selected);
        }
        for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
            steps.get(stepIndex).pseudoClassStateChanged(SELECTED, stepIndex == index);
        }
    }

    /** Devuelve una ruta libre, añadiendo _1, _2, etc. antes de la extensión. */
    protected final Path uniqueOutputPath(Path requestedPath) {
        String fileName = requestedPath.getFileName().toString();
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        String extension = extensionIndex > 0 ? fileName.substring(extensionIndex) : "";
        Path candidate = requestedPath;
        int suffix = 1;
        while (Files.exists(candidate)) {
            candidate = requestedPath.resolveSibling(baseName + "_" + suffix++ + extension);
        }
        return candidate;
    }

    /** Abre una carpeta existente fuera de JavaFX y delega el estado visual en la sección. */
    protected final void openOutputFolder(Path folder, Runnable onStarted,
                                         Consumer<Throwable> onFailure, Runnable onFinished) {
        onStarted.run();
        runTask(() -> {
            if (!Files.isDirectory(folder)) {
                throw new IOException("La carpeta de salida no existe: " + folder);
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("El sistema no permite abrir carpetas desde la aplicación.");
            }
            Desktop.getDesktop().open(folder.toFile());
            return folder;
        }, opened -> { }, onFailure, onFinished);
    }

    /**
     * Ejecuta una operación en un hilo daemon y entrega su resultado en JavaFX.
     * Registra los fallos de la operación o de su presentación y los comunica al
     * manejador de error. La finalización se ejecuta incluso si un manejador falla.
     *
     * @param <T> tipo del resultado
     * @param operation trabajo auxiliar que no debe modificar controles JavaFX
     * @param onSuccess presentación del resultado en el hilo de JavaFX
     * @param onFailure presentación del error en el hilo de JavaFX
     * @param onFinished limpieza de estado en JavaFX tras éxito, error o cancelación
     * @return tarea iniciada, que el llamante puede conservar para solicitar cancelación
     */
    protected final <T> Task<T> runTask(Callable<T> operation, Consumer<T> onSuccess,
                                      Consumer<Throwable> onFailure, Runnable onFinished) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> {
            try {
                onSuccess.accept(task.getValue());
            } catch (RuntimeException exception) {
                logger.error("SECTION_TASK_ERROR | No se pudo presentar el resultado.", exception);
                onFailure.accept(exception);
            } finally {
                onFinished.run();
            }
        });
        task.setOnFailed(event -> {
            try {
                logger.error("SECTION_TASK_ERROR | Falló la operación.", task.getException());
                onFailure.accept(task.getException());
            } finally {
                onFinished.run();
            }
        });
        task.setOnCancelled(event -> onFinished.run());
        Thread worker = new Thread(task, getClass().getSimpleName() + "-processing");
        worker.setDaemon(true);
        worker.start();
        return task;
    }
}

package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.UiComponents;
import javafx.concurrent.Task;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Previsualización local de una página PDF, sin selección de texto ni archivos abiertos persistentes. */
final class PdfPreview extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfPreview.class);
    private final ImageView image = new ImageView();
    private final Label status = UiComponents.mutedText("");
    private final Label pageLabel = UiComponents.label("", List.of("statement-preview-page"));
    private final Button previous;
    private final Button next;
    private final Button zoomOut;
    private final Button zoomIn;
    private Path pdf;
    private int page;
    private int pages;
    private int zoom = 100;
    private Task<RenderedPage> pending;

    /** Construye el visor con desplazamiento, navegación y zoom acotado. */
    PdfPreview() {
        super(12);
        getStyleClass().add("statement-preview");
        status.getStyleClass().add("statement-preview-status");
        previous = UiComponents.actionButton("Anterior", List.of("preview-previous"), () -> {
            page--;
            render();
        });
        next = UiComponents.actionButton("Siguiente", List.of("preview-next"), () -> {
            page++;
            render();
        });
        zoomOut = UiComponents.actionButton("−", List.of("preview-zoom-out"), () -> {
            zoom -= 25;
            render();
        });
        zoomIn = UiComponents.actionButton("+", List.of("preview-zoom-in"), () -> {
            zoom += 25;
            render();
        });
        zoomOut.setAccessibleText("Reducir zoom");
        zoomIn.setAccessibleText("Ampliar zoom");
        HBox toolbar = new HBox(10, previous, pageLabel, next, zoomOut, zoomIn);
        toolbar.getStyleClass().add("statement-preview-toolbar");
        image.setPreserveRatio(true);
        image.setSmooth(true);
        image.getStyleClass().add("statement-preview-image");
        StackPane paper = new StackPane(image);
        paper.getStyleClass().add("statement-preview-paper");
        ScrollPane viewport = new ScrollPane(paper);
        viewport.prefViewportHeightProperty().bind(Bindings.createDoubleBinding(
                () -> image.getBoundsInLocal().getHeight()
                        + paper.getPadding().getTop() + paper.getPadding().getBottom(),
                image.boundsInLocalProperty(), paper.paddingProperty()));
        viewport.setMinHeight(Region.USE_PREF_SIZE);
        viewport.setFitToWidth(true);
        viewport.getStyleClass().add("statement-preview-viewport");
        getChildren().addAll(toolbar, status, viewport);
        setVisible(false);
        managedProperty().bind(visibleProperty());
        updateControls(false);
    }

    /**
     * Reinicia el visor y carga la primera página; descarta resultados del PDF anterior.
     *
     * @param path documento local seleccionado
     */
    void load(Path path) {
        pdf = path;
        page = 0;
        pages = 0;
        zoom = 100;
        image.setImage(null);
        setVisible(true);
        render();
    }

    /** Renderiza una sola página fuera de JavaFX y cierra el documento incluso si falla. */
    private void render() {
        if (pending != null) {
            pending.cancel();
        }
        Path source = pdf;
        int requestedPage = page;
        int requestedZoom = zoom;
        Task<RenderedPage> task = new Task<>() {
            /** Limita la resolución para que documentos de gran formato no agoten la memoria. */
            @Override
            protected RenderedPage call() throws IOException {
                try (PDDocument document = Loader.loadPDF(source.toFile())) {
                    if (document.getNumberOfPages() == 0) {
                        throw new IOException("El PDF no contiene páginas.");
                    }
                    var pdfPage = document.getPage(requestedPage);
                    var bounds = pdfPage.getCropBox();
                    boolean rotated = Math.floorMod(pdfPage.getRotation(), 180) != 0;
                    float width = rotated ? bounds.getHeight() : bounds.getWidth();
                    float height = rotated ? bounds.getWidth() : bounds.getHeight();
                    if (width <= 0 || height <= 0) {
                        throw new IOException("Dimensiones de página no válidas.");
                    }
                    float scale = Math.min(900f * requestedZoom / 100 / width,
                            Math.min(1600f / width, 2200f / height));
                    PDFRenderer renderer = new PDFRenderer(document);
                    renderer.setSubsamplingAllowed(true);
                    BufferedImage rendered = renderer.renderImage(requestedPage, scale);
                    int[] pixels = rendered.getRGB(0, 0, rendered.getWidth(), rendered.getHeight(),
                            null, 0, rendered.getWidth());
                    return new RenderedPage(rendered.getWidth(), rendered.getHeight(), pixels,
                            document.getNumberOfPages());
                }
            }
        };
        pending = task;
        updateControls(true);
        status.setText("Cargando previsualización…");
        task.setOnSucceeded(event -> {
            if (pending != task) {
                return;
            }
            RenderedPage result = task.getValue();
            WritableImage rendered = new WritableImage(result.width(), result.height());
            rendered.getPixelWriter().setPixels(0, 0, result.width(), result.height(),
                    PixelFormat.getIntArgbInstance(), result.pixels(), 0, result.width());
            image.setImage(rendered);
            // La resolución puede limitarse por seguridad; el tamaño visual conserva el zoom elegido.
            image.setFitWidth(900.0 * requestedZoom / 100);
            pages = result.pages();
            status.setText("");
            updateControls(false);
        });
        task.setOnFailed(event -> {
            if (pending != task) {
                return;
            }
            image.setImage(null);
            pages = 0;
            status.setText("No se pudo previsualizar el PDF. Comprueba que sea legible "
                    + "y no esté protegido con contraseña. Puedes volver a seleccionarlo.");
            LOGGER.error("PDF_PREVIEW_ERROR | No se pudo previsualizar el documento.", task.getException());
            updateControls(false);
        });
        Thread worker = new Thread(task, "pdf-preview");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Mantiene los botones dentro de los límites de página y zoom.
     *
     * @param busy indica si hay una página cargándose
     */
    private void updateControls(boolean busy) {
        previous.setDisable(busy || pages == 0 || page == 0);
        next.setDisable(busy || pages == 0 || page >= pages - 1);
        zoomOut.setDisable(busy || pages == 0 || zoom <= 50);
        zoomIn.setDisable(busy || pages == 0 || zoom >= 150);
        pageLabel.setText(pages == 0 ? "Sin previsualización"
                : "Página " + (page + 1) + " de " + pages + " · " + zoom + " %");
    }

    /** Píxeles de una página y número total de páginas, sin conservar el documento PDF. */
    private record RenderedPage(int width, int height, int[] pixels, int pages) { }
}

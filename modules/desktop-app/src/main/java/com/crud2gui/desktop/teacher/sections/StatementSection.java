package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.statement.extractor.StatementExtractor;
import com.crud2gui.statement.filter.StatementFilter;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.concurrent.Callable;

/** Flujo ordenado de selección, extracción y filtrado de un enunciado local. */
public final class StatementSection extends VBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatementSection.class);
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");

    private final Button selectButton;
    private final Button extractButton;
    private final Button filterButton;
    private final Label filename = UiComponents.label("Ningún PDF seleccionado.",
            List.of("statement-file-name"));
    private final Label filePath = UiComponents.label("Selecciona un documento para comenzar.",
            List.of("statement-file-path"));
    private final VBox fileCard = UiComponents.vBox(List.of("statement-file-card"), filename, filePath);
    private final PdfPreview preview = new PdfPreview();
    private final Label status = UiComponents.label("", List.of("text-muted", "statement-status"));
    private final ProgressIndicator progress = new ProgressIndicator();
    private final TextArea text = new TextArea();
    private final TextArea filteredText = new TextArea();
    private final List<Button> steps;
    private final List<VBox> panels;
    private final StackPane panelHost = new StackPane();
    private Path selectedPdf;
    private boolean busy;
    private boolean filterCompleted;

    /** Crea el flujo y utiliza el selector de archivos del sistema. */
    public StatementSection() {
        this(StatementSection::choosePdf);
    }

    /**
     * Crea la vista con un selector sustituible en pruebas de la interfaz.
     *
     * @param chooseFile selector que devuelve el PDF o null al cancelar
     */
    StatementSection(Function<Window, File> chooseFile) {
        super(20);
        setPadding(new Insets(32, 38, 36, 38));
        selectButton = UiComponents.actionButton("Seleccionar PDF", List.of("statement-select"), () -> {
            File file = chooseFile.apply(getScene() == null ? null : getScene().getWindow());
            if (file != null) {
                selectPdf(file);
            }
        });
        extractButton = UiComponents.actionButton("Extraer texto", List.of("statement-extract"), this::extractText);
        filterButton = UiComponents.actionButton("Filtrar texto", List.of("statement-filter"), this::filterText);
        progress.getStyleClass().add("statement-progress");
        progress.setVisible(false);
        progress.managedProperty().bind(progress.visibleProperty());
        configureTextArea(text);
        text.setId("statement-raw-text");
        configureTextArea(filteredText);
        filteredText.getStyleClass().add("statement-filtered-text");
        Label pdfBadge = UiComponents.label("PDF", List.of("statement-file-badge"));
        filename.setGraphic(pdfBadge);
        filename.setGraphicTextGap(12);
        filename.setMaxWidth(Double.MAX_VALUE);
        filePath.setMaxWidth(Double.MAX_VALUE);
        VBox pdfPanel = TeacherComponents.surface(new VBox(14,
                TeacherComponents.panelHeading("Seleccionar PDF", "El documento de partida"),
                UiComponents.mutedText("Elige un PDF local. Después podrás continuar con la extracción del texto."),
                selectButton, fileCard, preview));
        VBox extractionPanel = TeacherComponents.surface(new VBox(14,
                TeacherComponents.panelHeading("Extraer texto", "Del PDF al texto plano"),
                UiComponents.mutedText("Extrae el contenido del documento. El resultado se mostrará aquí."),
                extractButton, text));
        VBox filterPanel = TeacherComponents.surface(new VBox(14,
                TeacherComponents.panelHeading("Filtrar texto", "Prepara el contenido del enunciado"),
                UiComponents.mutedText("Usa las reglas predeterminadas: desde «Introducción» hasta "
                        + "«Fechas y normas de entrega», omitiendo «Requisitos previos» y números de página. "
                        + "Si no se reconoce la estructura, se conserva el texto original."),
                filterButton, filteredText));
        panels = List.of(pdfPanel, extractionPanel, filterPanel);
        steps = List.of(
                stepButton(0, "SELECCIONAR PDF", "Documento de origen"),
                stepButton(1, "EXTRAER TEXTO", "Contenido en texto plano"),
                stepButton(2, "FILTRAR TEXTO", "Enunciado preparado"));
        HBox flow = new HBox(12, steps.get(0), connector(), steps.get(1), connector(), steps.get(2));
        flow.getStyleClass().add("statement-flow");
        flow.setMaxHeight(Region.USE_PREF_SIZE);
        panelHost.getChildren().addAll(panels);
        HBox feedback = new HBox(10, progress, status);
        feedback.getStyleClass().add("statement-feedback");
        getChildren().addAll(UiComponents.sectionHeader("ENUNCIADOS", "Enunciados",
                "Prepara tu enunciado paso a paso: selecciona el PDF, extrae el texto y aplica el filtro."),
                flow, panelHost, feedback);
        showStep(0);
        updateState();
    }

    /**
     * Crea una tarjeta de navegación con número, título y descripción.
     *
     * @param index posición del paso en el flujo
     * @param title título de la tarjeta
     * @param description explicación breve del paso
     * @return botón accesible por ratón y teclado
     */
    private Button stepButton(int index, String title, String description) {
        Label number = UiComponents.label(Integer.toString(index + 1), List.of("statement-step-number"));
        VBox labels = UiComponents.vBox(List.of("statement-step-labels"),
                UiComponents.label(title, List.of("statement-step-title")),
                UiComponents.label(description, List.of("statement-step-description")));
        labels.setMinWidth(0);
        labels.setMaxHeight(Region.USE_PREF_SIZE);
        HBox graphic = new HBox(12, number, labels);
        graphic.getStyleClass().add("statement-step-graphic");
        graphic.setMaxHeight(Region.USE_PREF_SIZE);
        Button button = new Button();
        button.setGraphic(graphic);
        button.setAccessibleText("Paso " + (index + 1) + ": " + title);
        button.getStyleClass().add("statement-step");
        button.setId("statement-step-" + (index + 1));
        button.setMinWidth(0);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefWidth(260);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setOnAction(event -> showStep(index));
        return button;
    }

    /** @return conector visual que indica el orden de los pasos */
    private StackPane connector() {
        SVGPath arrow = new SVGPath();
        arrow.setContent("M 3 3 L 11 11 L 3 19");
        arrow.getStyleClass().add("statement-connector-arrow");
        StackPane connector = new StackPane(arrow);
        connector.getStyleClass().add("statement-connector");
        connector.setMouseTransparent(true);
        return connector;
    }

    /**
     * Configura un resultado de texto seleccionable y de solo lectura.
     *
     * @param area control de texto del resultado
     */
    private void configureTextArea(TextArea area) {
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(16);
        area.getStyleClass().add("statement-text");
    }

    /**
     * Muestra únicamente el panel del paso elegido sin ejecutar su acción.
     *
     * @param index posición del paso habilitado
     */
    private void showStep(int index) {
        for (int i = 0; i < panels.size(); i++) {
            panels.get(i).setVisible(i == index);
            panels.get(i).setManaged(i == index);
            steps.get(i).pseudoClassStateChanged(SELECTED, i == index);
        }
    }

    /**
     * Conserva el PDF elegido e invalida cualquier resultado del documento anterior.
     *
     * @param file archivo elegido en el selector
     */
    private void selectPdf(File file) {
        selectedPdf = file.toPath();
        filename.setText(file.getName());
        filePath.setText(file.getAbsolutePath());
        fileCard.pseudoClassStateChanged(SELECTED, true);
        text.clear();
        resetFilter();
        updateState();
        showStep(0);
        preview.load(selectedPdf);
        showStatus("PDF seleccionado. Pulsa «Extraer texto» para continuar.", false);
    }

    /**
     * Abre el selector nativo limitado a archivos PDF.
     *
     * @param owner ventana propietaria del selector
     * @return archivo seleccionado o null si se cancela
     */
    private static File choosePdf(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar enunciado PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf", "*.PDF"));
        return chooser.showOpenDialog(owner);
    }

    /** Extrae el PDF en un hilo auxiliar y presenta el resultado en el hilo de JavaFX. */
    private void extractText() {
        Path pdf = selectedPdf;
        text.clear();
        resetFilter();
        runTask(() -> StatementExtractor.extractText(pdf), "Extrayendo texto del PDF…", result -> {
            if (result.isBlank()) {
                showStatus("El PDF no contiene texto extraíble. Si está escaneado, necesita OCR.", true);
                return;
            }
            text.setText(result);
            text.positionCaret(0);
            showStatus("Texto extraído correctamente. Ya puedes continuar al paso 3: Filtrar texto.", false);
        }, "No se pudo extraer el texto. Comprueba que el PDF sea válido, legible "
                + "y no esté protegido con contraseña.");
    }

    /** Ejecuta el filtro existente sin modificar el texto original extraído. */
    private void filterText() {
        String original = text.getText();
        resetFilter();
        runTask(() -> StatementFilter.filter(original), "Filtrando texto…", result -> {
            filteredText.setText(result.text());
            filteredText.positionCaret(0);
            filterCompleted = result.structureRecognized();
            showStatus(filterCompleted ? "Texto filtrado correctamente."
                    : "No se ha reconocido la estructura del enunciado. Se conserva el texto original sin filtrar.",
                    !filterCompleted);
        }, "No se pudo filtrar el texto. Puedes volver a intentarlo.");
    }

    /** Invalida el resultado del filtro cuando cambia su entrada o se vuelve a ejecutar. */
    private void resetFilter() {
        filteredText.clear();
        filterCompleted = false;
    }

    /**
     * Ejecuta una operación en segundo plano y comunica su resultado en JavaFX.
     *
     * @param <T> tipo del resultado
     * @param operation operación local que puede fallar
     * @param message mensaje durante la ejecución
     * @param onSuccess presentación del resultado
     * @param failureMessage mensaje recuperable en caso de error
     */
    private <T> void runTask(Callable<T> operation, String message, Consumer<T> onSuccess,
                            String failureMessage) {
        Task<T> task = new Task<>() {
            /** Ejecuta la operación fuera del hilo de interfaz. */
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };
        busy = true;
        updateState();
        showStatus(message, false);
        task.setOnSucceeded(event -> {
            onSuccess.accept(task.getValue());
            busy = false;
            updateState();
        });
        task.setOnFailed(event -> {
            showStatus(failureMessage, true);
            LOGGER.error("STATEMENT_PROCESSING_ERROR | {}", failureMessage, task.getException());
            busy = false;
            updateState();
        });
        Thread worker = new Thread(task, "statement-processing");
        worker.setDaemon(true);
        worker.start();
    }

    /** Sincroniza el bloqueo, el progreso y las marcas de pasos completados. */
    private void updateState() {
        boolean hasPdf = selectedPdf != null;
        boolean hasText = !text.getText().isBlank();
        selectButton.setDisable(busy);
        extractButton.setDisable(busy || !hasPdf);
        filterButton.setDisable(busy || !hasText);
        progress.setVisible(busy);
        steps.get(0).setDisable(busy);
        steps.get(1).setDisable(busy || !hasPdf);
        steps.get(2).setDisable(busy || !hasText);
        steps.get(0).pseudoClassStateChanged(COMPLETED, hasPdf);
        steps.get(1).pseudoClassStateChanged(COMPLETED, hasText);
        steps.get(2).pseudoClassStateChanged(COMPLETED, filterCompleted);
    }

    /**
     * Muestra un mensaje de estado o un error recuperable.
     *
     * @param message mensaje visible
     * @param error indica si debe resaltarse como error
     */
    private void showStatus(String message, boolean error) {
        status.setText(message);
        status.pseudoClassStateChanged(ERROR, error);
    }
}

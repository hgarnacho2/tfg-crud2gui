package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.statement.extractor.StatementExtractor;
import com.crud2gui.statement.filter.StatementFilter;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.concurrent.Callable;

/** Flujo ordenado de selección, extracción y filtrado de un enunciado local. */
public final class StatementSection extends VBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatementSection.class);
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass COMPLETED = PseudoClass.getPseudoClass("completed");

    private final Button selectPDFButton;
    private final Button extractButton;
    private final Button filterButton;
    private final Button changeFolderButton;
    private final Button filterChangeFolderButton;
    private final Button openFolderButton;
    private final Button filterOpenFolderButton;
    private final Label outputFolderLabel = UiComponents.label("", List.of("statement-file-path", "statement-output-folder"));
    private final Label filterOutputFolderLabel = UiComponents.label("", List.of("statement-file-path", "statement-output-folder"));
    private final Label outputFilename = UiComponents.labelWithGraphic("",
            List.of("statement-file-name"),
            UiComponents.label("TXT", List.of("statement-file-badge")), 12);
    private final Label filterOutputFilename = UiComponents.labelWithGraphic("",
            List.of("statement-file-name"),
            UiComponents.label("TXT", List.of("statement-file-badge")), 12);
    private final BiFunction<Window, Path, File> chooseFolder;
    private final Predicate<Path> confirmOverwrite;
    private final Label filename = UiComponents.labelWithGraphic("Ningún PDF seleccionado.",
            List.of("statement-file-name"),
            UiComponents.label("PDF", List.of("statement-file-badge")), 12);
    private final Label filePath = UiComponents.label("Selecciona un documento para comenzar.",
            List.of("statement-file-path"));
    private final VBox fileCard = UiComponents.vBox(List.of("statement-file-card"), filename, filePath);
    private final PdfPreview pdfPreview = new PdfPreview();
    private final List<Label> statuses = List.of(
            statusLabel("statement-selection-status"),
            statusLabel("statement-extraction-status"),
            statusLabel("statement-filter-status"));
    private final ProgressIndicator progress =
            UiComponents.progressIndicator(false, List.of("statement-progress"), true);
    private final TextArea text =
            UiComponents.textArea(false, true, 16, List.of("statement-text"));
    private final TextArea filteredText =
            UiComponents.textArea(false, true, 16, List.of("statement-text", "statement-filtered-text"));
    private final List<Button> steps;
    private final List<VBox> panels;
    private final StackPane panelHost = new StackPane();
    private Path selectedPdf;
    private Path outputFolder;
    private boolean busy;
    private boolean filterCompleted;
    private int currentStep;

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
        this(chooseFile, StatementSection::chooseOutputFolder, StatementSection::confirmOverwrite);
    }

    /**
     * Permite probar los destinos y confirmaciones sin abrir diálogos nativos.
     *
     * @param chooseFile selector del PDF
     * @param chooseFolder selector del destino, o null al cancelar
     * @param confirmOverwrite confirmación antes de reemplazar un archivo existente
     */
    StatementSection(Function<Window, File> chooseFile, BiFunction<Window, Path, File> chooseFolder,
                     Predicate<Path> confirmOverwrite) {
        super(20);
        this.chooseFolder = chooseFolder;
        this.confirmOverwrite = confirmOverwrite;
        setPadding(new Insets(32, 38, 36, 38));
        selectPDFButton = UiComponents.actionButton("Seleccionar PDF", List.of("statement-select"), () -> {
            File file = chooseFile.apply(getScene() == null ? null : getScene().getWindow());
            if (file != null) {
                selectPdf(file);
            }
        });
        extractButton = UiComponents.actionButton("Extraer texto", List.of("statement-extract"), this::extractText);
        filterButton = UiComponents.actionButton("Filtrar texto", List.of("statement-filter"), this::filterText);
        changeFolderButton = UiComponents.actionButton("Cambiar carpeta de salida",
                List.of("statement-change-folder", "secondary-button"), this::changeOutputFolder);
        filterChangeFolderButton = UiComponents.actionButton("Cambiar carpeta de salida",
                List.of("statement-change-folder", "secondary-button"), this::changeOutputFolder);
        openFolderButton = UiComponents.actionButton("Abrir carpeta de salida",
                List.of("statement-open-folder", "secondary-button"), this::openOutputFolder);
        filterOpenFolderButton = UiComponents.actionButton("Abrir carpeta de salida",
                List.of("statement-open-folder", "secondary-button"), this::openOutputFolder);
        text.setId("statement-raw-text");
        filePath.setMaxWidth(Double.MAX_VALUE);



        VBox pdfPanel = UiComponents.surface(new VBox(14,
                UiComponents.panelHeading("Seleccionar PDF",
                        "En el primer paso, se selecciona el documento PDF que contiene el enunciado de la práctica.",
                        17, 8),
//                UiComponents.mutedText("Pulsa el botón para seleccionar un PDF local. Después podrás continuar con la extracción del texto."),
                selectPDFButton, fileCard, pdfPreview, statuses.get(0)));
        VBox extractionPanel = UiComponents.surface(new VBox(14,
                UiComponents.panelHeading("Extraer texto", "Una vez seleccionado el PDF, se extrae el contenido como texto plano."),
                outputSettings(outputFilename, outputFolderLabel, changeFolderButton),
                UiComponents.mutedText("Extrae el contenido del documento. El resultado se mostrará aquí."),
                extractButton, text, statuses.get(1), openFolderButton));
        VBox filterPanel = UiComponents.surface(new VBox(14,
                UiComponents.panelHeading("Filtrar texto", "Antes de trabajar con un LLM que analice el contenido del enunciado, se aplica un filtro para optimizar el texto obtenido."),
                outputSettings(filterOutputFilename, filterOutputFolderLabel, filterChangeFolderButton),
                UiComponents.mutedText("Usa las reglas predeterminadas: desde «Introducción» hasta "
                        + "«Fechas y normas de entrega», omitiendo «Requisitos previos» y números de página. "
                        + "Si no se reconoce la estructura, se conserva el texto original."),
                filterButton, filteredText, statuses.get(2), filterOpenFolderButton));
        panels = List.of(pdfPanel, extractionPanel, filterPanel);


        
        steps = List.of(
                UiComponents.stepButton(0, "SELECCIONAR PDF", "Documento de origen", this::showStep),
                UiComponents.stepButton(1, "EXTRAER TEXTO", "Contenido en texto plano", this::showStep),
                UiComponents.stepButton(2, "FILTRAR TEXTO", "Enunciado preparado", this::showStep));
        HBox flow = new HBox(12, steps.get(0),
                UiComponents.arrowConnector(UiComponents.Direction.RIGHT), steps.get(1),
                UiComponents.arrowConnector(UiComponents.Direction.RIGHT), steps.get(2));
        flow.getStyleClass().add("statement-flow");
        flow.setMaxHeight(Region.USE_PREF_SIZE);
        panelHost.getChildren().addAll(panels);
        getChildren().addAll(UiComponents.sectionHeader("ENUNCIADO", "Enunciado",
                "Extracción de información del enunciado paso a paso: selecciona el PDF, extrae el texto y aplica el filtro para optimizar el texto obtenido."),
                flow, panelHost, progress);
        showStep(0);
        updateState();
    }

    /** Crea un mensaje de panel que no reserva espacio mientras está vacío. */
    private static Label statusLabel(String id) {
        Label label = UiComponents.label("", List.of("text-muted", "statement-status"));
        label.setId(id);
        label.setMaxWidth(Double.MAX_VALUE);
        label.visibleProperty().bind(label.textProperty().isNotEmpty());
        label.managedProperty().bind(label.visibleProperty());
        return label;
    }

    /**
     * Crea los controles de destino que permanecen dentro de su panel.
     *
     * @param nameLabel nombre del archivo de salida con insignia TXT
     * @param folderLabel ruta completa del archivo de salida
     * @param changeButton botón para cambiar el destino
     * @return selector de carpeta del panel
     */
    private static VBox outputSettings(Label nameLabel, Label folderLabel, Button changeButton) {
        folderLabel.setMaxWidth(Double.MAX_VALUE);
        VBox card = UiComponents.vBox(List.of("statement-file-card"), nameLabel, folderLabel);
        card.pseudoClassStateChanged(SELECTED, true);
        return UiComponents.vBox(List.of("statement-output-settings"), card, changeButton);
    }

    /**
     * Muestra únicamente el panel del paso elegido sin ejecutar su acción.
     *
     * @param index posición del paso habilitado
     */
    private void showStep(int index) {
        currentStep = index;
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
        selectedPdf = file.toPath().toAbsolutePath().normalize();
        outputFolder = selectedPdf.getParent();
        updateOutputFolderLabel();
        filename.setText(file.getName());
        filePath.setText(file.getAbsolutePath());
        fileCard.pseudoClassStateChanged(SELECTED, true);
        text.clear();
        statuses.get(1).setText("");
        resetFilter();
        updateState();
        showStep(0);
        pdfPreview.load(selectedPdf);
        showStatus(0, "PDF seleccionado. Pulsa «Extraer texto» para continuar.", false);
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

    /**
     * Abre el selector de destino en la carpeta actualmente elegida.
     *
     * @param owner ventana propietaria
     * @param current carpeta de salida actual
     * @return carpeta seleccionada, o null al cancelar
     */
    private static File chooseOutputFolder(Window owner, Path current) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Carpeta de salida de los enunciados");
        if (Files.isDirectory(current)) {
            chooser.setInitialDirectory(current.toFile());
        }
        return chooser.showDialog(owner);
    }

    /** Cambia el destino de las siguientes operaciones sin mover archivos ya generados. */
    private void changeOutputFolder() {
        File folder = chooseFolder.apply(getScene() == null ? null : getScene().getWindow(), outputFolder);
        if (folder != null) {
            outputFolder = folder.toPath().toAbsolutePath().normalize();
            updateOutputFolderLabel();
            showStatus(currentStep, "Carpeta de salida actualizada. Los archivos ya generados no se han movido.", false);
        }
    }

    /** Abre el destino en el explorador del sistema y comunica cualquier error. */
    private void openOutputFolder() {
        Path folder = outputFolder;
        int step = currentStep;
        runTask(step, () -> {
            if (!Files.isDirectory(folder)) {
                throw new IOException("La carpeta de salida no existe: " + folder);
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("El sistema no permite abrir carpetas desde la aplicación.");
            }
            Desktop.getDesktop().open(folder.toFile());
            return folder;
        }, "Abriendo carpeta de salida: " + folder,
                opened -> showStatus(step, "Carpeta de salida abierta: " + opened, false),
                "No se pudo abrir la carpeta de salida: " + folder);
    }

    /** Muestra el destino y los nombres que se utilizarán en las próximas operaciones. */
    private void updateOutputFolderLabel() {
        Path extractedPath = outputPath(false);
        Path filteredPath = outputPath(true);
        outputFilename.setText(extractedPath.getFileName().toString());
        outputFolderLabel.setText(extractedPath.toString());
        filterOutputFilename.setText(filteredPath.getFileName().toString());
        filterOutputFolderLabel.setText(filteredPath.toString());
    }

    /**
     * Calcula el nombre del resultado a partir del PDF original.
     *
     * @param filtered indica si se genera el resultado filtrado
     * @return ruta absoluta del TXT
     */
    private Path outputPath(boolean filtered) {
        String name = selectedPdf.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return outputFolder.resolve(base + (filtered ? "-filtrado.txt" : ".txt"));
    }

    /**
     * Solicita permiso para sobrescribir, sin dar por aceptado el cierre del diálogo.
     *
     * @param path archivo existente
     * @return true solo si se confirma el reemplazo
     */
    private static boolean confirmOverwrite(Path path) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "El archivo ya existe:\n" + path + "\n\n¿Quieres reemplazarlo?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Confirmar sobrescritura");
        alert.setHeaderText("Reemplazar archivo de salida");
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    /**
     * Comprueba el destino y pide confirmación antes de iniciar el procesamiento.
     *
     * @param path destino del resultado
     * @return destino autorizado, o null si se cancela
     */
    private OutputFile prepareOutput(Path path, int step) {
        boolean exists = Files.exists(path);
        if (exists && !confirmOverwrite.test(path)) {
            showStatus(step, "Operación cancelada. No se ha sobrescrito el archivo: " + path, false);
            return null;
        }
        return new OutputFile(path, exists);
    }

    /**
     * Guarda en UTF-8; no reemplaza archivos aparecidos después de comprobar el destino.
     *
     * @param output destino y permiso de sobrescritura
     * @param content resultado que se debe persistir
     * @throws IOException si el destino no permite escribir el archivo
     */
    private static void writeOutput(OutputFile output, String content) throws IOException {
        Files.writeString(output.path(), content, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                output.overwrite() ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE_NEW);
    }

    /** Extrae el PDF en un hilo auxiliar y presenta el resultado en el hilo de JavaFX. */
    private void extractText() {
        OutputFile output = prepareOutput(outputPath(false), 1);
        if (output == null) {
            return;
        }
        Path pdf = selectedPdf;
        text.clear();
        resetFilter();
        runTask(1, () -> {
            String result = StatementExtractor.extractText(pdf);
            if (!result.isBlank()) {
                writeOutput(output, result);
            }
            return result;
        }, "Extrayendo texto del PDF…\nArchivo de salida: " + output.path(), result -> {
            if (result.isBlank()) {
                showStatus(1, "El PDF no contiene texto extraíble. Si está escaneado, necesita OCR.", true);
                return;
            }
            text.setText(result);
            text.positionCaret(0);
            showStatus(1, "Texto extraído correctamente. Ya puedes continuar al paso 3: Filtrar texto."
                    + "\nArchivo generado: " + output.path(), false);
        }, "No se pudo extraer el texto. Comprueba que el PDF sea válido, legible "
                + "y no esté protegido con contraseña, y que puedas escribir en el destino: " + output.path());
    }

    /** Ejecuta el filtro existente sin modificar el texto original extraído. */
    private void filterText() {
        OutputFile output = prepareOutput(outputPath(true), 2);
        if (output == null) {
            return;
        }
        String original = text.getText();
        resetFilter();
        runTask(2, () -> {
            StatementFilter.Result result = StatementFilter.filter(original);
            writeOutput(output, result.text());
            return result;
        }, "Filtrando texto…\nArchivo de salida: " + output.path(), result -> {
            filteredText.setText(result.text());
            filteredText.positionCaret(0);
            filterCompleted = result.structureRecognized();
            showStatus(2, (filterCompleted ? "Texto filtrado correctamente."
                    : "No se ha reconocido la estructura del enunciado. Se conserva el texto original sin filtrar.")
                    + "\nArchivo generado: " + output.path(),
                    !filterCompleted);
        }, "No se pudo filtrar o guardar el texto. Comprueba el destino: " + output.path());
    }

    /** Invalida el resultado del filtro cuando cambia su entrada o se vuelve a ejecutar. */
    private void resetFilter() {
        filteredText.clear();
        statuses.get(2).setText("");
        filterCompleted = false;
    }

    /**
     * Ejecuta una operación en segundo plano y comunica su resultado en JavaFX.
     *
     * @param <T> tipo del resultado
     * @param step panel al que pertenece la operación y sus mensajes
     * @param operation operación local que puede fallar
     * @param message mensaje durante la ejecución
     * @param onSuccess presentación del resultado
     * @param failureMessage mensaje recuperable en caso de error
     */
    private <T> void runTask(int step, Callable<T> operation, String message, Consumer<T> onSuccess,
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
        showStatus(step, message, false);
        task.setOnSucceeded(event -> {
            onSuccess.accept(task.getValue());
            busy = false;
            updateState();
        });
        task.setOnFailed(event -> {
            showStatus(step, failureMessage, true);
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
        selectPDFButton.setDisable(busy);
        changeFolderButton.setDisable(busy);
        filterChangeFolderButton.setDisable(busy);
        openFolderButton.setDisable(busy);
        filterOpenFolderButton.setDisable(busy);
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
     * @param step panel al que pertenece el mensaje
     * @param error indica si debe resaltarse como error
     */
    private void showStatus(int step, String message, boolean error) {
        Label status = statuses.get(step);
        status.setText(message);
        status.pseudoClassStateChanged(ERROR, error);
    }

    /** Destino capturado antes del trabajo asíncrono y permiso explícito de reemplazo. */
    private record OutputFile(Path path, boolean overwrite) { }
}

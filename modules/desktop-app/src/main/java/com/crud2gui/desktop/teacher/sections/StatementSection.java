package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.ui.Section;
import com.crud2gui.desktop.ui.FileDialogs;
import com.crud2gui.desktop.ui.PdfPreview;
import com.crud2gui.common.utils.DotEnv;
import com.crud2gui.statement.extractor.StatementExtractor;
import com.crud2gui.statement.filter.StatementFilter;
import com.crud2gui.statement.filter.StatementFilterConfigJson;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.concurrent.Callable;

/**
 * Sección docente que permite seleccionar y previsualizar un PDF, extraer su texto
 * y filtrarlo mediante las reglas de un archivo JSON externo.
 *
 * <p>Organiza el flujo en tres paneles: selección (índice 0), extracción (índice 1)
 * y filtrado (índice 2). Cada panel conserva su propio mensaje de estado. La
 * selección de otro documento invalida los resultados anteriores y la extracción
 * vuelve a invalidar el filtrado.</p>
 *
 * <p>Los resultados se muestran en visores de solo lectura y se guardan en UTF-8
 * en la carpeta de salida elegida, previa confirmación si el destino ya existe.
 * El filtro carga {@code statement-filter-rules.json} en cada ejecución mediante
 * la ruta resuelta por {@link #filterConfigurationPath()}.</p>
 *
 * <p>La construcción y las operaciones de interfaz deben realizarse en el hilo
 * de JavaFX. La extracción, el filtrado, la escritura y la apertura del explorador
 * se ejecutan mediante tareas auxiliares; el visor PDF gestiona su propia carga.</p>
 */
public final class StatementSection extends Section {

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
            UiComponents.label("PDF", List.of("statement-file-badge", "statement-pdf-badge")), 12);
    private final Label filePath = UiComponents.label("Selecciona un documento para comenzar.",
            List.of("statement-file-path"));
    private final VBox fileCard = UiComponents.vBox(List.of("statement-file-card"), filename, filePath);
    private final PdfPreview pdfPreview = new PdfPreview();
    private final List<Label> statuses = List.of(
            UiComponents.statusLabel("statement-selection-status"),
            UiComponents.statusLabel("statement-extraction-status"),
            UiComponents.statusLabel("statement-filter-status"));
    private final ProgressIndicator progress =
            UiComponents.progressIndicator(false, List.of("statement-progress"), true);
    private final TextArea extractedText =
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

    /**
     * Construye los tres paneles con los diálogos de selección de PDF,
     * carpeta y confirmación de sobrescritura. Inicialmente solo está habilitado
     * el paso de selección y no se carga ningún documento.
     */
    public StatementSection() {
        this(owner -> FileDialogs.chooseFile(owner, "Seleccionar enunciado PDF",
                new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf", "*.PDF")));
    }

    /**
     * Crea la vista con un selector sustituible en pruebas de la interfaz.
     *
     * @param chooseFile selector que recibe la ventana propietaria, o {@code null}
     *                   si la sección aún no tiene ventana, y devuelve el PDF
     *                   seleccionado o {@code null} al cancelar
     */
    StatementSection(Function<Window, File> chooseFile) {
        this(chooseFile, (owner, current) -> FileDialogs.chooseDirectory(
                owner, current, "Carpeta de salida de los enunciados"), FileDialogs::confirmOverwrite);
    }

    /**
     * Construye la interfaz con selectores y confirmación sustituibles para poder
     * probar el flujo sin abrir diálogos interactivos.
     *
     * @param chooseFile función que recibe la ventana propietaria, posiblemente
     *                   {@code null}, y devuelve un PDF o {@code null} al cancelar
     * @param chooseFolder función que recibe la ventana propietaria y la carpeta
     *                     actual, y devuelve el nuevo destino o {@code null} al cancelar
     * @param confirmOverwrite predicado que recibe la ruta de un archivo existente
     *                         y devuelve {@code true} únicamente si se autoriza reemplazarlo
     */
    StatementSection(Function<Window, File> chooseFile, BiFunction<Window, Path, File> chooseFolder,
                     Predicate<Path> confirmOverwrite) {
        super("ENUNCIADO", "Enunciado",
                "Extracción de información del enunciado paso a paso: selecciona el PDF, extrae el texto y aplica el filtro para optimizar el texto obtenido.");
        this.chooseFolder = chooseFolder;
        this.confirmOverwrite = confirmOverwrite;
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
        extractedText.setId("statement-raw-text");
        filePath.setMaxWidth(Double.MAX_VALUE);



        VBox pdfPanel = UiComponents.surface(new VBox(14,
                UiComponents.panelHeading("Seleccionar PDF",
                        "En el primer paso, se selecciona el documento PDF que contiene el enunciado de la práctica.",
                        17, 8),
                selectPDFButton, fileCard, pdfPreview, statuses.get(0)));
        VBox extractionPanel = UiComponents.surface(new VBox(14,
                UiComponents.panelHeading("Extraer texto", "Una vez seleccionado el PDF, se extrae el contenido como texto plano."),
                UiComponents.panelHeading("Salida", "Se generará un fichero de texto plano con el contenido extraído en la carpeta de salida seleccionada."),
                UiComponents.outputSettings(outputFilename, outputFolderLabel, changeFolderButton),
                UiComponents.panelHeading("Iniciar el proceso", "El resultado se mostrará en el visor y se generará un archivo físico en la carpeta de salida."),
                extractButton, extractedText, openFolderButton, statuses.get(1)));
        VBox filterPanel = UiComponents.surface(new VBox(14,
                UiComponents.panelHeading("Filtrar texto", "Antes de trabajar con un LLM que analice el contenido del enunciado, se aplica un filtro para optimizar el texto obtenido."),
                UiComponents.panelHeading("Salida", "Se generará un fichero de texto plano con el contenido filtrado en la carpeta de salida."),
                UiComponents.outputSettings(filterOutputFilename, filterOutputFolderLabel, filterChangeFolderButton),
                UiComponents.panelHeading("Reglas de filtrado",
                        "Las reglas de filtrado aplicadas son las definidas en el archivo de configuración statement-filter-rules.json."),
                UiComponents.fileContentPane("statement-filter-config", "Ver statement-filter-rules.json",
                        StatementSection::filterConfigurationPath,
                        "No se pudo leer statement-filter-rules.json. Comprueba la ruta "
                                + "STATEMENT_FILTER_CONFIG_PATH y los permisos del archivo."),
                UiComponents.mutedText("El archivo de reglas se lee en cada filtrado. "
                        + "Si no se reconoce la estructura definida, se conserva el texto original."),
                UiComponents.panelHeading("Iniciar el proceso", "Tomando como referencia el fichero generado en el paso anterior, se aplicará el filtro al texto extraído. El resultado se mostrará en el visor y se generará un archivo físico en la carpeta de salida."),
                filterButton, filteredText, filterOpenFolderButton, statuses.get(2)));
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
        flow.setMaxWidth(Region.USE_PREF_SIZE);
        panelHost.getChildren().addAll(panels);
        getChildren().addAll(flow, panelHost, progress);
        showStep(0);
        updateState();
    }

    /**
     * Resuelve la ruta del JSON utilizado tanto por la consulta como por el filtrado.
     *
     * <p>Obtiene {@code STATEMENT_FILTER_CONFIG_PATH} mediante {@link DotEnv};
     * si no está configurada, usa la ruta del módulo extractor. Las rutas relativas
     * se buscan desde el directorio de ejecución y, después, desde sus antecesores.
     * Una ruta absoluta se devuelve normalizada sin comprobar aquí su existencia.</p>
     *
     * @return ruta absoluta y normalizada del archivo de reglas
     * @throws IOException si no se encuentra el archivo indicado por una ruta relativa
     * @throws InvalidPathException si el valor configurado no es una ruta válida
     */
    private static Path filterConfigurationPath() throws IOException {
        Path path = Path.of(DotEnv.getStringVar("STATEMENT_FILTER_CONFIG_PATH",
                "modules/statement-text-extractor/config/statement-filter-rules.json"));
        if (path.isAbsolute()) {
            return path.normalize();
        }
        Path directory = Path.of("").toAbsolutePath().normalize();
        while (directory != null) {
            Path candidate = directory.resolve(path).normalize();
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IOException("No se encuentra el fichero de configuración JSON: " + path);
    }

    /**
     * Muestra únicamente el panel del paso elegido sin ejecutar su acción.
     *
     * @param index índice del panel que se muestra: 0 para selección, 1 para
     *              extracción o 2 para filtrado; el botón llamante controla su habilitación
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
     * Conserva el PDF elegido, establece su carpeta como destino inicial y limpia
     * los resultados de extracción y filtrado del documento anterior.
     * Actualiza las tarjetas, vuelve al primer panel e inicia la previsualización.
     *
     * @param file PDF seleccionado, no nulo; su lectura se comprueba al previsualizarlo
     *             o extraerlo, no durante esta asignación
     */
    private void selectPdf(File file) {
        selectedPdf = file.toPath().toAbsolutePath().normalize();
        outputFolder = selectedPdf.getParent();
        updateOutputFolderLabel();
        filename.setText(file.getName());
        filePath.setText(file.getAbsolutePath());
        fileCard.pseudoClassStateChanged(SELECTED, true);
        resetExtract();
        resetFilter();
        updateState();
        showStep(0);
        pdfPreview.load(selectedPdf);
        showStatus(0, "PDF seleccionado. Puedes continuar con el paso 2: Extraer texto.", false);
    }

    /**
     * Solicita una carpeta de salida y actualiza los destinos previstos de ambos TXT.
     * Cancelar conserva la carpeta actual. Elegir otra carpeta no mueve los archivos
     * ya generados ni vuelve a ejecutar la extracción o el filtrado.
     */
    private void changeOutputFolder() {
        File folder = chooseFolder.apply(getScene() == null ? null : getScene().getWindow(), outputFolder);
        if (folder != null) {
            outputFolder = folder.toPath().toAbsolutePath().normalize();
            updateOutputFolderLabel();
            showStatus(currentStep, "Carpeta de salida actualizada. Los archivos ya generados no se han movido.", false);
        }
    }

    /**
     * Captura la carpeta actual y solicita abrirla en el explorador mediante una
     * tarea auxiliar. Comprueba que exista y que el sistema admita la apertura;
     * solo comunica el error, en el panel desde el que se solicitó.
     */
    private void openOutputFolder() {
        Path folder = outputFolder;
        int step = currentStep;
        busy = true;
        updateState();
        runTask(() -> {
            if (!Files.isDirectory(folder)) {
                throw new IOException("La carpeta de salida no existe: " + folder);
            }
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new IOException("El sistema no permite abrir carpetas desde la aplicación.");
            }
            Desktop.getDesktop().open(folder.toFile());
            return folder;
        }, opened -> { },
                failure -> showStatus(step, "No se pudo abrir la carpeta de salida: " + folder, true),
                () -> {
                    busy = false;
                    updateState();
                });
    }

    /**
     * Actualiza las etiquetas de nombre y ruta de las dos tarjetas de salida a partir
     * del PDF y la carpeta actuales. Describe destinos previstos, no garantiza que
     * los archivos existan ni modifica archivos en disco.
     */
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
     * <p>Requiere un PDF seleccionado y una carpeta de salida asignada.</p>
     *
     * @param filtered {@code true} para añadir {@code -filtrado.txt};
     *                 {@code false} para utilizar únicamente la extensión {@code .txt}
     * @return ruta absoluta del TXT en la carpeta de salida actual, sin crearlo
     */
    private Path outputPath(boolean filtered) {
        String name = selectedPdf.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return outputFolder.resolve(base + (filtered ? "-filtrado.txt" : ".txt"));
    }

    /**
     * Comprueba si el destino existe y, en ese caso, solicita autorización para
     * reemplazarlo. Si se rechaza, muestra la cancelación en el panel indicado.
     * No crea ni escribe el archivo: captura el destino y el permiso para la tarea.
     *
     * @param path ruta del archivo que se pretende generar
     * @param step índice del panel que recibe el mensaje de cancelación (0, 1 o 2)
     * @return destino y permiso de sobrescritura, o {@code null} si se rechaza el reemplazo
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
     * Escribe el resultado en UTF-8 sin crear directorios padre. Si se autorizó
     * sobrescribir, trunca el archivo existente; en caso contrario exige crear uno
     * nuevo, evitando reemplazar un archivo aparecido después de la comprobación.
     *
     * @param output ruta de destino y autorización de sobrescritura capturadas previamente
     * @param content texto que se debe guardar
     * @throws IOException si no puede escribirse el destino, por ejemplo por permisos,
     *                     ausencia de su carpeta o conflicto con el modo de creación
     */
    private static void writeOutput(OutputFile output, String content) throws IOException {
        Files.writeString(output.path(), content, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
                output.overwrite() ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE_NEW);
    }

    /**
     * Prepara el destino y extrae el PDF seleccionado en una tarea auxiliar.
     * Tras autorizar la operación, limpia la extracción y el filtrado anteriores.
     * Solo guarda un TXT si obtiene texto no vacío; presenta el resultado en JavaFX
     * o informa de la ausencia de texto extraíble, de errores de lectura o de escritura.
     */
    private void extractText() {
        OutputFile output = prepareOutput(outputPath(false), 1);
        if (output == null) {
            return;
        }
        Path pdf = selectedPdf;
        resetExtract();
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
            extractedText.setText(result);
            extractedText.positionCaret(0);
            showStatus(1, "Texto extraído correctamente. Puedes continuar con el paso 3: Filtrar texto."
                    + "\nArchivo generado: " + output.path(), false);
        }, "No se pudo extraer el texto. Comprueba que el PDF sea válido, legible "
                + "y no esté protegido con contraseña, y que puedas escribir en el destino: " + output.path());
    }

    /**
     * Filtra el texto del visor de extracción sin modificarlo y guarda el TXT resultante.
     * Carga y valida las reglas JSON en cada ejecución; si esa carga falla, la tarea
     * termina con error antes de escribir, sin recurrir a reglas predeterminadas.
     * Si no reconoce la estructura, guarda el texto original y muestra una advertencia,
     * sin marcar el paso como completado.
     */
    private void filterText() {
        OutputFile output = prepareOutput(outputPath(true), 2);
        if (output == null) {
            return;
        }
        String original = extractedText.getText();
        resetFilter();
        runTask(2, () -> {
            var config = StatementFilterConfigJson.read(filterConfigurationPath());
            StatementFilter.Result result = StatementFilter.filter(original, config);
            writeOutput(output, result.text());
            return result;
        }, "Filtrando texto…\nArchivo de salida: " + output.path(), result -> {
            filteredText.setText(result.text());
            filteredText.positionCaret(0);
            filterCompleted = result.structureRecognized();
            showStatus(2, (filterCompleted ? "Texto filtrado correctamente. Flujo completado."
                    : "No se ha reconocido la estructura del enunciado. Se conserva el texto original sin filtrar.")
                    + "\nArchivo generado: " + output.path(),
                    !filterCompleted);
        }, "No se pudo filtrar o guardar el texto. Comprueba statement-filter-rules.json "
                + "(ruta STATEMENT_FILTER_CONFIG_PATH, permisos y reglas válidas) y el destino: " + output.path());
    }

    /**
     * Limpia el visor de extracción y su mensaje. No elimina archivos ni reinicia
     * el filtrado; el llamante debe invocar {@link #resetFilter()} y actualizar el
     * estado de los controles cuando corresponda.
     */
    private void resetExtract() {
        extractedText.clear();
        statuses.get(1).setText("");
    }

    /**
     * Limpia el visor y el mensaje del filtro y retira su marca de finalización.
     * No modifica el texto extraído ni elimina el TXT generado anteriormente.
     * La actualización visual de botones y marcas corresponde a {@link #updateState()}.
     */
    private void resetFilter() {
        filteredText.clear();
        statuses.get(2).setText("");
        filterCompleted = false;
    }

    /**
     * Ejecuta una operación en segundo plano y comunica su resultado en JavaFX.
     * Bloquea los controles durante la ejecución y muestra el progreso en el panel
     * indicado. Utiliza un hilo daemon por operación. Los manejadores de éxito y
     * fallo se ejecutan en el hilo de JavaFX; los fallos de la tarea se registran.
     *
     * @param <T> tipo del resultado
     * @param step índice del panel al que pertenece la operación (0, 1 o 2)
     * @param operation trabajo ejecutado fuera de JavaFX; no debe modificar controles
     * @param message mensaje durante la ejecución
     * @param onSuccess presentación del resultado en JavaFX
     * @param failureMessage mensaje recuperable en caso de error
     */
    private <T> void runTask(int step, Callable<T> operation, String message, Consumer<T> onSuccess,
                            String failureMessage) {
        busy = true;
        updateState();
        showStatus(step, message, false);
        runTask(operation, onSuccess, failure -> showStatus(step, failureMessage, true), () -> {
            busy = false;
            updateState();
        });
    }

    /**
     * Sincroniza los controles con el estado actual: bloquea acciones durante una tarea,
     * exige un PDF para extraer y texto no vacío para filtrar, y muestra el progreso.
     * Actualiza las marcas de selección, extracción y filtrado completados a partir
     * del documento, el visor de extracción y el reconocimiento de la estructura.
     */
    private void updateState() {
        boolean hasPdf = selectedPdf != null;
        boolean hasText = !extractedText.getText().isBlank();
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
     * @param step índice del panel que recibe el mensaje (0, 1 o 2)
     * @param message texto que sustituye al mensaje anterior; vacío para ocultar la etiqueta
     * @param error {@code true} para activar el estilo de error; {@code false} para retirarlo
     */
    private void showStatus(int step, String message, boolean error) {
        showStatus(statuses.get(step), message, error);
    }

    /**
     * Destino inmutable capturado antes del trabajo asíncrono junto con el permiso
     * de reemplazo obtenido durante la preparación.
     *
     * @param path ruta del archivo que la tarea escribirá
     * @param overwrite {@code true} si el archivo existía y se autorizó reemplazarlo;
     *                  {@code false} si debe crearse sin sobrescribir
     */
    private record OutputFile(Path path, boolean overwrite) { }
}

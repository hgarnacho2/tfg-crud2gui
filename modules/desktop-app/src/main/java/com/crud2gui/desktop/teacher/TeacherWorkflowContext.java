package com.crud2gui.desktop.teacher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.crud2gui.desktop.workflow.WorkflowContext;
import com.crud2gui.desktop.workflow.ValidationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Estado persistente del flujo docente de una cuenta.
 *
 * <p>Conserva las rutas de los archivos seleccionados y generados, además de si algunas
 * entradas de validación fueron elegidas manualmente. El estado se carga al construir
 * la instancia y se escribe en un JSON versionado después de cada cambio. Los archivos
 * referenciados no se copian ni se eliminan al reiniciar el contexto.</p>
 */
public final class TeacherWorkflowContext implements WorkflowContext {

    /** Registrador de errores de lectura y escritura del estado persistido. */
    private static final Logger logger = LoggerFactory.getLogger(TeacherWorkflowContext.class);
    /** Serializador JSON compartido por todas las instancias del contexto. */
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** Versión del formato JSON persistido. */
    private static final int VERSION = 1;

    /** Archivo JSON donde se guarda el contexto de esta instancia. */
    private final Path stateFile;
    /** PDF de origen de la etapa de tratamiento del enunciado. */
    private Path statementPdf;
    /** Carpeta fuente del último análisis de código Java. */
    private Path codeSourceFolder;
    /** Ruta del prompt seleccionado para inferir el modelo desde el enunciado. */
    private Path selectedPromptFile;
    /** Texto extraído del PDF antes de aplicar las reglas de filtrado. */
    private Path extractedText;
    /** Texto filtrado que se sugiere como entrada para la inferencia del modelo. */
    private Path filteredText;
    /** Último modelo generado desde el enunciado, todavía no confirmado. */
    private Path generatedStatementModel;
    /** Último modelo del enunciado validado y confirmado por el usuario. */
    private Path confirmedStatementModel;
    /** Último modelo de clases extraído del código Java. */
    private Path codeModel;
    /** Último informe JSON de comparación de modelos. */
    private Path validationReport;
    /** Estado del último informe de validación disponible. */
    private ValidationStatus validationStatus = ValidationStatus.NOT_VALIDATED;
    /** Texto actualmente seleccionado como entrada para la inferencia. */
    private Path selectedStatementText;
    /** Modelo Java seleccionado actualmente para la comparación. */
    private Path selectedJavaModel;
    /** Modelo del enunciado seleccionado actualmente para la comparación. */
    private Path selectedInferredModel;
    /** ID del proveedor LLM seleccionado para inferir el modelo. */
    private String selectedModelProviderId;
    /** ID del modelo LLM seleccionado dentro del proveedor. */
    private String selectedModelId;
    /** Indica si el texto de entrada se eligió manualmente y no por sugerencia. */
    private boolean statementTextManual;
    /** Indica si el modelo Java de validación se eligió manualmente. */
    private boolean javaModelManual;
    /** Indica si el modelo del enunciado de validación se eligió manualmente. */
    private boolean inferredModelManual;

    /**
     * Crea el contexto genérico guardado en el directorio de usuario.
     *
     * <p>El panel docente usa el constructor por cuenta para mantener estados separados.</p>
     */
    public TeacherWorkflowContext() {
        this(Path.of(System.getProperty("user.home"), ".crud2gui", "teacher-workflow.json"));
    }

    /**
     * Crea el contexto persistente asociado a una cuenta.
     * El identificador se transforma en un hash para no incluirlo en claro en el nombre del archivo.
     *
     * @param userId identificador estable de la cuenta autenticada
     * @throws NullPointerException si {@code userId} es {@code null}
     */
    public TeacherWorkflowContext(String userId) {
        this(stateFileFor(userId));
    }

    /**
     * Crea un contexto que lee y escribe en el archivo indicado.
     *
     * @param stateFile ruta del JSON de estado
     * @throws NullPointerException si {@code stateFile} es {@code null}
     */
    public TeacherWorkflowContext(Path stateFile) {
        this.stateFile = Objects.requireNonNull(stateFile, "stateFile").toAbsolutePath().normalize();
        load();
    }

    /**
     * Indica si hay alguna ruta o selección de trabajo que merezca ofrecerse al iniciar.
     * No exige que los archivos referenciados sigan existiendo.
     *
     * @return {@code true} si hay al menos una referencia o selección guardada
     */
    public boolean hasSavedContext() {
        return statementPdf != null || codeSourceFolder != null || extractedText != null || filteredText != null
                || generatedStatementModel != null || confirmedStatementModel != null || codeModel != null
            || selectedPromptFile != null || selectedStatementText != null
                || selectedJavaModel != null || selectedInferredModel != null || validationReport != null
                || selectedModelProviderId != null || selectedModelId != null
                || statementTextManual || javaModelManual || inferredModelManual;
    }

    /**
     * Elimina las referencias persistidas y deja vacío el contexto de trabajo.
     * No borra los PDF, textos ni modelos existentes en el sistema de archivos.
     */
    public void reset() {
        statementPdf = null;
        codeSourceFolder = null;
        selectedPromptFile = null;
        extractedText = null;
        filteredText = null;
        generatedStatementModel = null;
        confirmedStatementModel = null;
        codeModel = null;
        clearValidationReport();
        selectedStatementText = null;
        selectedJavaModel = null;
        selectedInferredModel = null;
        selectedModelProviderId = null;
        selectedModelId = null;
        statementTextManual = false;
        javaModelManual = false;
        inferredModelManual = false;
        persist();
    }

    /**
     * Devuelve el PDF guardado si todavía es un archivo legible.
     *
     * @return ruta del PDF utilizable o {@code null} si no existe o no se puede leer
     */
    public Path getStatementPdf() {
        return usable(statementPdf);
    }

    /**
     * Devuelve la ruta del prompt seleccionado, incluso si el archivo ya no existe.
     *
     * @return ruta absoluta del prompt o {@code null} si no se ha seleccionado
     */
    public Path getSelectedPromptFile() {
        return selectedPromptFile;
    }

    /**
     * Guarda la ruta del prompt seleccionado. Si cambia, invalida el modelo inferido
     * que dependía del prompt anterior.
     *
     * @param path ruta del archivo o {@code null} para quitar la selección
     */
    public void selectPromptFile(Path path) {
        Path selected = normalize(path);
        if (!Objects.equals(selectedPromptFile, selected)) {
            selectedPromptFile = selected;
            invalidateStatementModelsForInputChange();
            persist();
        }
    }

    /**
     * Elige el texto filtrado como entrada sugerida y, si no está disponible,
     * usa el texto extraído.
     *
     * @return ruta legible del texto filtrado o extraído; {@code null} si ninguno está disponible
     */
    public Path getSuggestedStatementText() {
        Path preferred = usable(filteredText);
        return preferred != null ? preferred : usable(extractedText);
    }

    /** @return texto extraído del PDF si sigue disponible */
    public Path getExtractedText() {
        return usable(extractedText);
    }

    /** @return texto filtrado del enunciado si sigue disponible */
    public Path getFilteredText() {
        return usable(filteredText);
    }

    /**
     * Devuelve la selección manual del usuario o la entrada sugerida vigente.
     * Una selección manual puede devolverse aunque su archivo haya dejado de existir.
     *
     * @return ruta de entrada o {@code null} si no hay una entrada utilizable
     */
    public Path getSelectedStatementText() {
        return statementTextManual ? selectedStatementText
                : usable(selectedStatementText) != null ? usable(selectedStatementText) : getSuggestedStatementText();
    }

    /**
     * Indica si el texto actual se eligió manualmente.
     *
     * @return {@code true} si la selección manual tiene prioridad sobre la sugerencia
     */
    public boolean isStatementTextManual() {
        return statementTextManual;
    }

    /**
     * Devuelve el último modelo Java generado si continúa siendo legible.
     *
     * @return ruta sugerida o {@code null} si el archivo no está disponible
     */
    public Path getSuggestedJavaModel() {
        return usable(codeModel);
    }

    @Override
    public Path getGeneratedCodeModel() {
        return getSuggestedJavaModel();
    }

    /**
     * Devuelve el modelo Java elegido manualmente o, en su defecto, el último modelo generado.
     *
     * @return ruta seleccionada o sugerida; {@code null} si no hay un modelo utilizable
     */
    public Path getSelectedJavaModel() {
        return selected(javaModelManual, selectedJavaModel, getSuggestedJavaModel());
    }

    /**
     * Indica si el modelo Java de validación fue elegido manualmente.
     *
     * @return {@code true} si la selección manual tiene prioridad sobre el modelo generado
     */
    public boolean isJavaModelManual() {
        return javaModelManual;
    }

    /**
     * Devuelve el último modelo del enunciado confirmado si continúa siendo legible.
     * Los modelos generados pero no confirmados no se ofrecen para validar.
     *
     * @return ruta del modelo confirmado o {@code null} si no está disponible
     */
    public Path getSuggestedInferredModel() {
        return usable(confirmedStatementModel);
    }

    /** @return último JSON inferido, confirmado o pendiente de confirmación, si sigue disponible */
    public Path getGeneratedStatementModel() {
        return usable(generatedStatementModel);
    }

    /**
     * Devuelve el modelo del enunciado elegido manualmente o el último modelo confirmado.
     *
     * @return ruta seleccionada o sugerida; {@code null} si no hay un modelo utilizable
     */
    public Path getSelectedInferredModel() {
        return selected(inferredModelManual, selectedInferredModel, getSuggestedInferredModel());
    }

    /**
     * Indica si el modelo del enunciado de validación fue elegido manualmente.
     *
     * @return {@code true} si la selección manual tiene prioridad sobre el modelo confirmado
     */
    public boolean isInferredModelManual() {
        return inferredModelManual;
    }

    /**
     * Devuelve el identificador del proveedor LLM seleccionado anteriormente.
     *
     * @return ID estable del proveedor o {@code null} si no se ha seleccionado
     */
    public String getSelectedModelProviderId() {
        return selectedModelProviderId;
    }

    /**
     * Devuelve el identificador del modelo seleccionado anteriormente.
     *
     * @return ID del modelo o {@code null} si no se ha seleccionado
     */
    public String getSelectedModelId() {
        return selectedModelId;
    }

    /**
     * Registra el proveedor LLM. Cambiarlo limpia el modelo seleccionado y los resultados
     * inferidos que dependían del proveedor anterior.
     *
     * @param providerId ID estable del proveedor, o {@code null} para quitar la selección
     */
    public void selectModelProvider(String providerId) {
        String selected = normalizeId(providerId);
        if (!Objects.equals(selectedModelProviderId, selected)) {
            selectedModelProviderId = selected;
            selectedModelId = null;
            invalidateStatementModelsForInputChange();
            persist();
        }
    }

    /**
     * Registra el modelo seleccionado. Cambiarlo invalida resultados inferidos anteriores.
     *
     * @param modelId ID del modelo, o {@code null} para quitar la selección
     */
    public void selectModel(String modelId) {
        String selected = normalizeId(modelId);
        if (!Objects.equals(selectedModelId, selected)) {
            selectedModelId = selected;
            invalidateStatementModelsForInputChange();
            persist();
        }
    }

    /**
     * Registra el PDF de origen. Si cambia, invalida los textos y modelos dependientes.
     * La operación actualiza referencias, pero no modifica archivos existentes.
     *
     * @param path ruta del PDF, o {@code null} para quitar la selección
     */
    public void selectStatementPdf(Path path) {
        Path selected = normalize(path);
        if (!Objects.equals(statementPdf, selected)) {
            statementPdf = selected;
            extractedText = null;
            filteredText = null;
            selectedStatementText = null;
            statementTextManual = false;
            invalidateStatementModelsForInputChange();
            persist();
        }
    }

    /**
     * Registra la carpeta de código fuente. Si cambia, invalida el modelo Java sugerido
     * y cualquier selección manual que dependía de la carpeta anterior.
     *
     * @param path carpeta fuente o {@code null} para quitarla
     */
    public void selectCodeFolder(Path path) {
        Path selected = normalize(path);
        if (!Objects.equals(codeSourceFolder, selected)) {
            codeSourceFolder = selected;
            codeModel = null;
            selectedJavaModel = null;
            javaModelManual = false;
            clearValidationReport();
            persist();
        }
    }

    @Override
    public Path getSelectedCodeFolder() {
        return codeSourceFolder != null && Files.isDirectory(codeSourceFolder) && Files.isReadable(codeSourceFolder)
                ? codeSourceFolder : null;
    }

    /**
     * Registra el resultado de una extracción completada correctamente.
     * Lo propone como entrada si el usuario no seleccionó otro texto manualmente,
     * y descarta el filtrado y los modelos dependientes.
     *
     * @param path archivo de texto extraído
     */
    public void recordExtractedText(Path path) {
        extractedText = normalize(path);
        filteredText = null;
        if (!statementTextManual) {
            selectedStatementText = extractedText;
        }
        invalidateStatementModelsForInputChange();
        persist();
    }

    /**
     * Invalida los resultados de extracción y filtrado anteriores antes de reintentar.
     * No elimina los archivos físicos que se generaron en ejecuciones previas.
     */
    public void beginStatementExtraction() {
        extractedText = null;
        filteredText = null;
        if (!statementTextManual) {
            selectedStatementText = null;
        }
        invalidateStatementModelsForInputChange();
        persist();
    }

    /**
     * Invalida el filtrado anterior y vuelve a sugerir el texto extraído mientras
     * se procesa un nuevo resultado filtrado.
     */
    public void beginStatementFiltering() {
        filteredText = null;
        if (!statementTextManual) {
            selectedStatementText = usable(extractedText);
        }
        invalidateStatementModelsForInputChange();
        persist();
    }

    /**
     * Registra un filtrado completado y lo propone como entrada para la inferencia.
     * También invalida los modelos que dependían del texto anterior.
     *
     * @param path archivo de texto filtrado
     */
    public void recordFilteredText(Path path) {
        filteredText = normalize(path);
        if (!statementTextManual) {
            selectedStatementText = filteredText;
        }
        invalidateStatementModelsForInputChange();
        persist();
    }

    /**
     * Registra una entrada manual para la inferencia desde enunciado.
     * Si cambia, invalida los modelos inferidos que dependían de la entrada anterior.
     *
     * @param path archivo de texto seleccionado, o {@code null} para quitar la selección
     */
    public void selectStatementText(Path path) {
        Path selected = normalize(path);
        if (!Objects.equals(selectedStatementText, selected)) {
            invalidateStatementModelsForInputChange();
        }
        selectedStatementText = selected;
        statementTextManual = true;
        persist();
    }

    /**
     * Registra el modelo recién generado, todavía pendiente de confirmación.
     * Reemplaza el modelo confirmado como sugerencia, pero no borra archivos físicos.
     *
     * @param path archivo JSON generado
     */
    public void recordGeneratedStatementModel(Path path) {
        generatedStatementModel = normalize(path);
        clearValidationReport();
        confirmedStatementModel = null;
        if (!inferredModelManual) {
            selectedInferredModel = null;
        }
        persist();
    }

    /**
     * Invalida las referencias a modelos inferidos sin eliminar sus archivos.
     * Mantiene una selección manual de validación hasta que cambie una entrada del flujo.
     */
    public void clearStatementModel() {
        clearStatementModels();
        persist();
    }

    /**
     * Registra un modelo del enunciado confirmado y lo ofrece para la validación,
     * salvo que el usuario haya fijado otra selección manual.
     *
     * @param path archivo JSON validado
     */
    public void recordConfirmedStatementModel(Path path) {
        confirmedStatementModel = normalize(path);
        clearValidationReport();
        if (!inferredModelManual) {
            selectedInferredModel = confirmedStatementModel;
        }
        persist();
    }

    /**
     * Registra un modelo generado desde código y lo ofrece para la validación
     * si no hay una selección manual.
     *
     * @param path archivo JSON generado
     */
    public void recordCodeModel(Path path) {
        codeModel = normalize(path);
        clearValidationReport();
        if (!javaModelManual) {
            selectedJavaModel = codeModel;
        }
        persist();
    }

    /**
     * Registra la selección manual del modelo Java para validar.
     *
     * @param path archivo JSON seleccionado, o {@code null} para quitar la selección
     */
    public void selectJavaModel(Path path) {
        selectedJavaModel = normalize(path);
        javaModelManual = true;
        clearValidationReport();
        persist();
    }

    /**
     * Registra la selección manual del modelo del enunciado para validar.
     *
     * @param path archivo JSON seleccionado, o {@code null} para quitar la selección
     */
    public void selectInferredModel(Path path) {
        selectedInferredModel = normalize(path);
        inferredModelManual = true;
        clearValidationReport();
        persist();
    }

    @Override
    public void recordValidationReport(Path path) {
        Path normalized = normalize(path);
        recordValidationReport(normalized, readReportStatus(normalized));
    }

    @Override
    public void recordValidationReport(Path path, ValidationStatus status) {
        validationReport = normalize(path);
        validationStatus = validationReport == null ? ValidationStatus.NOT_VALIDATED
                : Objects.requireNonNull(status, "status");
        persist();
    }

    @Override
    public Path getValidationReport() {
        return usable(validationReport);
    }

    @Override
    public ValidationStatus getValidationStatus() {
        return getValidationReport() == null ? ValidationStatus.NOT_VALIDATED : validationStatus;
    }

    private void clearValidationReport() {
        validationReport = null;
        validationStatus = ValidationStatus.NOT_VALIDATED;
    }

    /** Limpia las rutas de modelos inferidos y conserva una selección manual vigente. */
    private void clearStatementModels() {
        generatedStatementModel = null;
        confirmedStatementModel = null;
        if (!inferredModelManual) {
            selectedInferredModel = null;
        }
    }

    /** Invalida modelos inferidos y su selección manual cuando cambia una entrada fuente. */
    private void invalidateStatementModelsForInputChange() {
        inferredModelManual = false;
        selectedInferredModel = null;
        clearStatementModels();
        clearValidationReport();
    }

    /**
     * Resuelve la selección manual o usa una ruta legible y, si falta, la sugerencia.
     *
     * @param manual indica si la selección manual tiene prioridad
     * @param selected ruta elegida previamente
     * @param suggestion ruta sugerida por la etapa productora
     * @return ruta que debe utilizar la etapa consumidora, o {@code null}
     */
    private static Path selected(boolean manual, Path selected, Path suggestion) {
        if (manual) {
            return selected;
        }
        Path current = usable(selected);
        return current != null ? current : suggestion;
    }

    /**
     * Comprueba que una ruta referencia un archivo regular y legible.
     *
     * @param path ruta que se comprobará
     * @return la misma ruta si es utilizable; {@code null} en otro caso
     */
    private static Path usable(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isReadable(path) ? path : null;
    }

    /**
     * Convierte una ruta en absoluta y normalizada, conservando {@code null}.
     *
     * @param path ruta original
     * @return ruta absoluta normalizada o {@code null}
     */
    private static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    /** Normaliza un ID de proveedor o modelo, convirtiendo valores vacíos en {@code null}. */
    private static String normalizeId(String id) {
        return id == null || id.isBlank() ? null : id.strip();
    }

    /**
     * Deriva la ruta privada de estado a partir del hash del identificador de cuenta.
     *
     * @param userId identificador estable de la cuenta
     * @return ruta del JSON de estado bajo el directorio de usuario
     * @throws NullPointerException si {@code userId} es {@code null}
     * @throws IllegalStateException si el entorno Java no dispone de SHA-256
     */
    private static Path stateFileFor(String userId) {
        Objects.requireNonNull(userId, "userId");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(userId.getBytes(StandardCharsets.UTF_8));
            return Path.of(System.getProperty("user.home"), ".crud2gui",
                    "teacher-workflow-" + java.util.HexFormat.of().formatHex(digest) + ".json");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    /** Lee el estado versionado si existe; un archivo inválido se ignora y se registra. */
    private void load() {
        if (!Files.isRegularFile(stateFile)) {
            return;
        }
        try {
            JsonNode root = MAPPER.readTree(stateFile.toFile());
            if (root == null || root.path("version").asInt(-1) != VERSION) {
                logger.warn("TEACHER_WORKFLOW_STATE_VERSION | Se ignora un estado de trabajo incompatible: {}",
                        stateFile);
                return;
            }
            statementPdf = readPath(root, "statementPdf");
            codeSourceFolder = readPath(root, "codeSourceFolder");
            selectedPromptFile = readPath(root, "selectedPromptFile");
            extractedText = readPath(root, "extractedText");
            filteredText = readPath(root, "filteredText");
            generatedStatementModel = readPath(root, "generatedStatementModel");
            confirmedStatementModel = readPath(root, "confirmedStatementModel");
            codeModel = readPath(root, "codeModel");
            validationReport = readPath(root, "validationReport");
            validationStatus = readValidationStatus(root);
            selectedStatementText = readPath(root, "selectedStatementText");
            selectedJavaModel = readPath(root, "selectedJavaModel");
            selectedInferredModel = readPath(root, "selectedInferredModel");
            selectedModelProviderId = normalizeId(root.path("selectedModelProviderId").asText(null));
            selectedModelId = normalizeId(root.path("selectedModelId").asText(null));
            statementTextManual = root.path("statementTextManual").asBoolean(false);
            javaModelManual = root.path("javaModelManual").asBoolean(false);
            inferredModelManual = root.path("inferredModelManual").asBoolean(false);
        } catch (IOException | RuntimeException exception) {
            logger.warn("TEACHER_WORKFLOW_STATE_READ_ERROR | No se pudo recuperar el trabajo docente: {}",
                    stateFile, exception);
        }
    }

    /**
     * Lee una propiedad de ruta JSON y la normaliza como ruta absoluta.
     *
     * @param root objeto JSON que contiene las propiedades del estado
     * @param field nombre de la propiedad que se leerá
     * @return ruta normalizada o {@code null} si la propiedad falta o está vacía
     */
    private static Path readPath(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        return value == null || value.isBlank() ? null : Path.of(value).toAbsolutePath().normalize();
    }

    private ValidationStatus readValidationStatus(JsonNode root) {
        String stored = root.path("validationStatus").asText(null);
        if (stored != null) {
            try {
                return ValidationStatus.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
                return ValidationStatus.UNKNOWN;
            }
        }
        return readReportStatus(validationReport);
    }

    private ValidationStatus readReportStatus(Path reportFile) {
        if (reportFile == null) {
            return ValidationStatus.NOT_VALIDATED;
        }
        try {
            JsonNode report = MAPPER.readTree(reportFile.toFile());
            if (report == null || !report.isObject() || !report.has("valid")) {
                return ValidationStatus.UNKNOWN;
            }
            return ValidationStatus.fromReport(report.path("valid").asBoolean(),
                    report.path("readyForCrud").asBoolean());
        } catch (IOException | RuntimeException exception) {
            return ValidationStatus.UNKNOWN;
        }
    }

    /** Persiste todas las rutas y opciones mediante reemplazo atómico cuando está disponible. */
    private void persist() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("version", VERSION);
        writePath(root, "statementPdf", statementPdf);
        writePath(root, "codeSourceFolder", codeSourceFolder);
        writePath(root, "selectedPromptFile", selectedPromptFile);
        writePath(root, "extractedText", extractedText);
        writePath(root, "filteredText", filteredText);
        writePath(root, "generatedStatementModel", generatedStatementModel);
        writePath(root, "confirmedStatementModel", confirmedStatementModel);
        writePath(root, "codeModel", codeModel);
        writePath(root, "validationReport", validationReport);
        root.put("validationStatus", validationStatus.name());
        writePath(root, "selectedStatementText", selectedStatementText);
        writePath(root, "selectedJavaModel", selectedJavaModel);
        writePath(root, "selectedInferredModel", selectedInferredModel);
        writeString(root, "selectedModelProviderId", selectedModelProviderId);
        writeString(root, "selectedModelId", selectedModelId);
        root.put("statementTextManual", statementTextManual);
        root.put("javaModelManual", javaModelManual);
        root.put("inferredModelManual", inferredModelManual);
        Path temporary = stateFile.resolveSibling(stateFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(stateFile.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), root);
            try {
                Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            logger.warn("TEACHER_WORKFLOW_STATE_WRITE_ERROR | No se pudo guardar el trabajo docente: {}",
                    stateFile, exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                logger.debug("No se pudo eliminar el fichero temporal {}", temporary, cleanupException);
            }
        }
    }

    /**
     * Escribe una ruta como cadena JSON o como valor JSON nulo.
     *
     * @param root objeto JSON que recibe la propiedad
     * @param field nombre de la propiedad
     * @param path ruta que se escribirá
     */
    private static void writePath(ObjectNode root, String field, Path path) {
        if (path == null) {
            root.putNull(field);
        } else {
            root.put(field, path.toString());
        }
    }

    /** Escribe una cadena como propiedad JSON o como valor nulo. */
    private static void writeString(ObjectNode root, String field, String value) {
        if (value == null) {
            root.putNull(field);
        } else {
            root.put(field, value);
        }
    }
}
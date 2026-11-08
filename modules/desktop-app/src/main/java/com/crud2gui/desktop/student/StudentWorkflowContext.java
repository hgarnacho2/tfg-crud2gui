package com.crud2gui.desktop.student;

import com.crud2gui.desktop.workflow.WorkflowContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.HexFormat;
import java.util.Objects;

/** Estado persistente del flujo de modelos de una cuenta de alumno. */
public final class StudentWorkflowContext implements WorkflowContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentWorkflowContext.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int VERSION = 1;

    private final Path stateFile;
    private Path codeFolder;
    private Path codeModel;
    private Path selectedJavaModel;
    private Path selectedInferredModel;
    private Path validationReport;
    private boolean javaModelManual;
    private boolean inferredModelManual;

    public StudentWorkflowContext(String userId) {
        this(stateFileFor(userId));
    }

    StudentWorkflowContext(Path stateFile) {
        this.stateFile = Objects.requireNonNull(stateFile, "stateFile").toAbsolutePath().normalize();
        load();
    }

    public boolean hasSavedContext() {
        return codeFolder != null || codeModel != null || selectedJavaModel != null
                || selectedInferredModel != null || validationReport != null
                || javaModelManual || inferredModelManual;
    }

    public void reset() {
        codeFolder = null;
        codeModel = null;
        selectedJavaModel = null;
        selectedInferredModel = null;
        validationReport = null;
        javaModelManual = false;
        inferredModelManual = false;
        persist();
    }

    @Override
    public void selectCodeFolder(Path path) {
        Path selected = normalize(path);
        if (!Objects.equals(codeFolder, selected)) {
            codeFolder = selected;
            codeModel = null;
            if (!javaModelManual) {
                selectedJavaModel = null;
            }
            selectedInferredModel = null;
            inferredModelManual = false;
            validationReport = null;
            persist();
        }
    }

    @Override
    public Path getSelectedCodeFolder() {
        return codeFolder != null && Files.isDirectory(codeFolder) && Files.isReadable(codeFolder)
                ? codeFolder : null;
    }

    @Override
    public void recordCodeModel(Path path) {
        codeModel = normalize(path);
        validationReport = null;
        if (!javaModelManual) {
            selectedJavaModel = codeModel;
        }
        selectedInferredModel = null;
        inferredModelManual = false;
        persist();
    }

    @Override
    public Path getSelectedJavaModel() {
        return usable(selectedJavaModel);
    }

    @Override
    public boolean isJavaModelManual() {
        return javaModelManual;
    }

    @Override
    public void selectJavaModel(Path path) {
        selectedJavaModel = normalize(path);
        javaModelManual = true;
        validationReport = null;
        persist();
    }

    @Override
    public Path getSelectedInferredModel() {
        return usable(selectedInferredModel);
    }

    @Override
    public boolean isInferredModelManual() {
        return inferredModelManual;
    }

    @Override
    public void selectInferredModel(Path path) {
        selectedInferredModel = normalize(path);
        inferredModelManual = true;
        validationReport = null;
        persist();
    }

    @Override
    public void recordValidationReport(Path path) {
        validationReport = normalize(path);
        persist();
    }

    @Override
    public Path getValidationReport() {
        return usable(validationReport);
    }

    private void load() {
        if (!Files.isRegularFile(stateFile)) {
            return;
        }
        try {
            JsonNode root = MAPPER.readTree(stateFile.toFile());
            if (root.path("version").asInt(-1) != VERSION) {
                LOGGER.warn("STUDENT_CONTEXT_VERSION | Se ignora un contexto de alumno incompatible.");
                return;
            }
            codeFolder = readPath(root, "codeFolder");
            codeModel = readPath(root, "codeModel");
            selectedJavaModel = readPath(root, "selectedJavaModel");
            selectedInferredModel = readPath(root, "selectedInferredModel");
            validationReport = readPath(root, "validationReport");
            javaModelManual = root.path("javaModelManual").asBoolean(false);
            inferredModelManual = root.path("inferredModelManual").asBoolean(false);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("STUDENT_CONTEXT_READ_ERROR | No se pudo cargar el contexto del alumno.", exception);
        }
    }

    private void persist() {
        Path parent = stateFile.getParent();
        Path temporary = null;
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ObjectNode root = MAPPER.createObjectNode();
            root.put("version", VERSION);
            writePath(root, "codeFolder", codeFolder);
            writePath(root, "codeModel", codeModel);
            writePath(root, "selectedJavaModel", selectedJavaModel);
            writePath(root, "selectedInferredModel", selectedInferredModel);
            writePath(root, "validationReport", validationReport);
            root.put("javaModelManual", javaModelManual);
            root.put("inferredModelManual", inferredModelManual);
            temporary = Files.createTempFile(parent, stateFile.getFileName().toString(), ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), root);
            try {
                Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, stateFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("STUDENT_CONTEXT_WRITE_ERROR | No se pudo guardar el contexto del alumno.", exception);
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupFailure) {
                    LOGGER.debug("STUDENT_CONTEXT_CLEANUP_ERROR | No se pudo eliminar el archivo temporal.",
                            cleanupFailure);
                }
            }
        }
    }

    private static Path stateFileFor(String userId) {
        Objects.requireNonNull(userId, "userId");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(userId.getBytes(StandardCharsets.UTF_8));
            return Path.of(System.getProperty("user.home"), ".crud2gui", "student-workflows",
                    HexFormat.of().formatHex(digest) + ".json");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private static Path readPath(JsonNode root, String field) {
        return root.hasNonNull(field) ? Path.of(root.get(field).asText()).toAbsolutePath().normalize() : null;
    }

    private static void writePath(ObjectNode root, String field, Path path) {
        if (path == null) {
            root.putNull(field);
        } else {
            root.put(field, path.toString());
        }
    }

    private static Path normalize(Path path) {
        return path == null ? null : path.toAbsolutePath().normalize();
    }

    private static Path usable(Path path) {
        return path != null && Files.isRegularFile(path) && Files.isReadable(path) ? path : null;
    }
}

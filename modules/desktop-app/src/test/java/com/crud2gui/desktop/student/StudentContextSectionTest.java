package com.crud2gui.desktop.student;

import com.crud2gui.desktop.student.sections.StudentContextSection;
import com.crud2gui.desktop.student.sections.ModelValidationSection;
import com.crud2gui.desktop.workflow.ValidationStatus;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentContextSectionTest {

    @TempDir
    Path directory;

    @BeforeAll
    static void startJavaFx() throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        try {
            Platform.startup(() -> started.complete(null));
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(() -> started.complete(null));
        }
        started.get(10, TimeUnit.SECONDS);
        Platform.setImplicitExit(false);
    }

    @Test
    void rendersCodeFlowAndReferenceModelFromStudentWorkflow() throws Exception {
        Path codeFolder = Files.createDirectory(directory.resolve("src"));
        Path codeModel = Files.writeString(directory.resolve("modelo-codigo.json"), "{}");
        Path validModel = Files.writeString(directory.resolve("modelo-valido.json"), "{}");
        Path validationReport = Files.writeString(directory.resolve("validacion.json"),
            "{\"valid\":true,\"readyForCrud\":true}");
        StudentWorkflowContext workflow = new StudentWorkflowContext(directory.resolve("workflow.json"));
        workflow.selectCodeFolder(codeFolder);
        workflow.recordCodeModel(codeModel);
        workflow.selectInferredModel(validModel);
        workflow.recordValidationReport(validationReport, ValidationStatus.READY_FOR_CRUD);

        onFx(() -> {
            StudentContextSection section = new StudentContextSection(workflow);
            StackPane root = new StackPane(section);
            Scene scene = new Scene(root, 1200, 900);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/styles/app.css")).toExternalForm());
            root.resize(1200, 900);
            root.applyCss();
            root.layout();

            Label codeTitle = label(section, "student-context-code-lane-title");
            assertEquals("CÓDIGO FUENTE", codeTitle.getText());
            assertEquals(20, codeTitle.getFont().getSize(), 0.01);
            assertEquals(15, label(section, "student-context-code-lane-description").getFont().getSize(), 0.01);
            assertEquals(15, label(section, "student-context-reference-lane-description").getFont().getSize(), 0.01);
            assertEquals("src", label(section, "student-context-stage-code-folder-file").getText());
            assertEquals(14, label(section, "student-context-stage-code-folder-path").getFont().getSize(), 0.01);
                assertEquals(codeFolder.toAbsolutePath().normalize().toString(),
                    label(section, "student-context-stage-code-folder-path").getText());
            assertEquals("modelo-codigo.json", label(section, "student-context-stage-code-model-file").getText());
                assertEquals(codeModel.toAbsolutePath().normalize().getParent().toString(),
                    label(section, "student-context-stage-code-model-path").getText());
            assertEquals("validacion.json", label(section, "student-context-stage-validation-report-file").getText());
                assertEquals(validationReport.toAbsolutePath().normalize().getParent().toString(),
                    label(section, "student-context-stage-validation-report-path").getText());
            assertEquals("Listo para CRUD", label(section, "student-context-validation-status").getText());
                assertEquals("✓", label(section, "student-context-validation-status-icon").getText());
            HBox statusBadge = (HBox) section.lookup("#student-context-validation-status-badge");
            assertTrue(statusBadge.getStyleClass().contains("workflow-validation-status-ready-for-crud"));
            assertEquals(Color.TRANSPARENT, statusBadge.getBackground().getFills().get(0).getFill());
            assertEquals(Insets.EMPTY, statusBadge.getPadding());
            assertEquals("modelo-valido.json", label(section, "student-context-stage-valid-model-file").getText());
            Button revealModel = (Button) section.lookup("#student-context-stage-code-model-reveal");
            assertNotNull(revealModel);
            assertTrue(revealModel.isVisible());
            assertFalse(revealModel.isDisabled());
            assertEquals("Mostrar en la carpeta contenedora", revealModel.getTooltip().getText());
            assertEquals(28, revealModel.getWidth(), 0.01);
            assertFalse(((Button) section.lookup("#student-context-stage-code-folder-reveal")).isDisabled());
            assertFalse(((Button) section.lookup("#student-context-validation-input-code-reveal")).isDisabled());
            Button pending = (Button) section.lookup("#student-context-stage-crud-reveal");
            assertFalse(pending.isVisible());
            assertFalse(pending.isManaged());
            assertEquals("CODIGO", label(section, "student-context-validation-input-code-badge").getText());
                assertEquals(11, label(section, "student-context-validation-input-code-badge").getFont().getSize(), 0.01);
            assertEquals("modelo-codigo.json", label(section, "student-context-validation-input-code-file").getText());
                assertEquals(13, label(section, "student-context-validation-input-code-file").getFont().getSize(), 0.01);
            assertEquals("ENUNCIADO", label(section, "student-context-validation-input-statement-badge").getText());
                assertEquals(11, label(section, "student-context-validation-input-statement-badge").getFont().getSize(), 0.01);
            assertEquals("modelo-valido.json",
                    label(section, "student-context-validation-input-statement-file").getText());
                assertEquals(13, label(section, "student-context-validation-input-statement-file").getFont().getSize(), 0.01);
            assertNotNull(section.lookup("#student-context-validation-arrow"));
                    assertNotNull(section.lookup("#student-context-reference-arrow"));
                    assertNotNull(section.lookup("#student-context-crud-arrow"));
                    VBox codeFolderStage = (VBox) section.lookup("#student-context-stage-code-folder");
                VBox validation = (VBox) section.lookup("#student-context-stage-validation-report");
                VBox crud = (VBox) section.lookup("#student-context-stage-crud");
                    assertEquals(codeFolderStage.getWidth(),
                    ((VBox) section.lookup("#student-context-stage-valid-model")).getWidth(), 1);
                    assertTrue(validation.getWidth() > codeFolderStage.getWidth() * 1.8);
                    assertEquals(validation.getWidth(), crud.getWidth(), 1);
            return null;
        });
    }

    @Test
    void studentValidationSectionOffersOutputFolderButton() throws Exception {
        StudentWorkflowContext workflow = new StudentWorkflowContext(directory.resolve("workflow.json"));

        onFx(() -> {
            ModelValidationSection section = new ModelValidationSection(workflow);
            StackPane root = new StackPane(section);
            Scene scene = new Scene(root, 1200, 900);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/styles/app.css")).toExternalForm());
            root.resize(1200, 900);
            root.applyCss();

            Button openFolder = (Button) section.lookup("#model-validation-open-output-folder");
            assertNotNull(openFolder);
            assertEquals("Abrir carpeta de salida", openFolder.getText());
            return null;
        });
    }

    private static Label label(StudentContextSection section, String id) {
        return (Label) section.lookup("#" + id);
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
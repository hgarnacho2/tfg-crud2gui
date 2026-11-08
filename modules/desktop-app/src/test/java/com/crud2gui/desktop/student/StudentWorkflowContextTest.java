package com.crud2gui.desktop.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentWorkflowContextTest {

    @TempDir
    Path directory;

    @Test
    void restoresSelectedModelsAndResetKeepsModelFiles() throws Exception {
        Path stateFile = directory.resolve("student-workflow.json");
        Path codeFolder = Files.createDirectory(directory.resolve("src"));
        Path javaModel = Files.writeString(directory.resolve("java-model.json"), "{}");
        Path expectedModel = Files.writeString(directory.resolve("expected-model.json"), "{}");
        Path validationReport = Files.writeString(directory.resolve("validation-report.json"), "{}");
        StudentWorkflowContext context = new StudentWorkflowContext(stateFile);

        context.selectCodeFolder(codeFolder);
        context.recordCodeModel(javaModel);
        context.selectInferredModel(expectedModel);
        context.recordValidationReport(validationReport);

        StudentWorkflowContext restored = new StudentWorkflowContext(stateFile);
        assertTrue(restored.hasSavedContext());
        assertEquals(codeFolder, restored.getSelectedCodeFolder());
        assertEquals(javaModel, restored.getSelectedJavaModel());
        assertEquals(expectedModel, restored.getSelectedInferredModel());
        assertEquals(validationReport, restored.getValidationReport());
        assertFalse(restored.isJavaModelManual());
        assertTrue(restored.isInferredModelManual());

        restored.reset();

        StudentWorkflowContext reset = new StudentWorkflowContext(stateFile);
        assertFalse(reset.hasSavedContext());
        assertNull(reset.getSelectedJavaModel());
        assertNull(reset.getSelectedInferredModel());
        assertTrue(Files.exists(javaModel));
        assertTrue(Files.exists(expectedModel));
    }
}

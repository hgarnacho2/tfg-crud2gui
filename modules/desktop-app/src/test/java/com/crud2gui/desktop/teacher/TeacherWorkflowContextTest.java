package com.crud2gui.desktop.teacher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.crud2gui.desktop.workflow.ValidationStatus;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherWorkflowContextTest {

    @TempDir
    Path directory;

    @Test
    void persistsSuggestionsAndManualValidationSelections() throws Exception {
        Path stateFile = directory.resolve("workflow.json");
        Path extracted = Files.writeString(directory.resolve("statement.txt"), "statement");
        Path filtered = Files.writeString(directory.resolve("statement-filtered.txt"), "filtered");
        Path inferred = Files.writeString(directory.resolve("inferred-validated.json"), "{}");
        Path generated = Files.writeString(directory.resolve("inferred-generated.json"), "{}");
        Path javaModel = Files.writeString(directory.resolve("java.json"), "{}");
        Path manualJavaModel = Files.writeString(directory.resolve("manual-java.json"), "{}");
        Path validationReport = Files.writeString(directory.resolve("validation-report.json"),
            "{\"valid\":true,\"readyForCrud\":true}");
        Path prompt = Files.writeString(directory.resolve("prompt.txt"), "instructions");

        TeacherWorkflowContext context = new TeacherWorkflowContext(stateFile);
        context.recordExtractedText(extracted);
        assertEquals(extracted, context.getSuggestedStatementText());
        context.recordFilteredText(filtered);
        context.selectPromptFile(prompt);
        context.selectModelProvider("fake-provider");
        context.selectModel("fake-model");
        context.recordGeneratedStatementModel(generated);
        context.recordConfirmedStatementModel(inferred);
        context.recordCodeModel(javaModel);
        context.selectJavaModel(manualJavaModel);
        context.recordValidationReport(validationReport);

        TeacherWorkflowContext restored = new TeacherWorkflowContext(stateFile);

        assertEquals(filtered, restored.getSelectedStatementText());
        assertEquals(extracted, restored.getExtractedText());
        assertEquals(filtered, restored.getFilteredText());
        assertEquals(generated, restored.getGeneratedStatementModel());
        assertEquals(inferred, restored.getSelectedInferredModel());
        assertEquals(validationReport, restored.getValidationReport());
        assertEquals(ValidationStatus.READY_FOR_CRUD, restored.getValidationStatus());
        assertEquals(prompt, restored.getSelectedPromptFile());
        assertEquals("fake-provider", restored.getSelectedModelProviderId());
        assertEquals("fake-model", restored.getSelectedModelId());
        assertEquals(manualJavaModel, restored.getSelectedJavaModel());
        assertEquals(javaModel, restored.getSuggestedJavaModel());
    }

    @Test
    void persistsAndRestoresSelectedCodeFolder() throws Exception {
        Path stateFile = directory.resolve("workflow.json");
        Path codeFolder = Files.createDirectory(directory.resolve("src"));
        TeacherWorkflowContext context = new TeacherWorkflowContext(stateFile);

        context.selectCodeFolder(codeFolder);

        TeacherWorkflowContext restored = new TeacherWorkflowContext(stateFile);
        assertEquals(codeFolder, restored.getSelectedCodeFolder());
    }

    @Test
    void onlySuggestsConfirmedInferredModelsAndFallsBackToExtractedText() throws Exception {
        Path stateFile = directory.resolve("workflow.json");
        Path extracted = Files.writeString(directory.resolve("statement.txt"), "statement");
        Path generated = Files.writeString(directory.resolve("generated.json"), "{}");
        TeacherWorkflowContext context = new TeacherWorkflowContext(stateFile);

        context.recordExtractedText(extracted);
        context.recordGeneratedStatementModel(generated);

        assertEquals(extracted, context.getSuggestedStatementText());
        assertNull(context.getSuggestedInferredModel());
        assertNull(context.getSelectedInferredModel());
    }

    @Test
    void changingStatementInputClearsAnOldManualExpectedModel() throws Exception {
        Path stateFile = directory.resolve("workflow.json");
        Path firstInput = Files.writeString(directory.resolve("first.txt"), "first");
        Path secondInput = Files.writeString(directory.resolve("second.txt"), "second");
        Path oldModel = Files.writeString(directory.resolve("old-validated.json"), "{}");
        TeacherWorkflowContext context = new TeacherWorkflowContext(stateFile);

        context.selectStatementText(firstInput);
        context.selectInferredModel(oldModel);
        context.selectStatementText(secondInput);

        assertNull(context.getSelectedInferredModel());
        assertNull(context.getSuggestedInferredModel());
        assertEquals(secondInput, context.getSelectedStatementText());
    }

    @Test
    void changingValidatedModelsClearsValidationStatus() throws Exception {
        Path stateFile = directory.resolve("workflow.json");
        Path javaModel = Files.writeString(directory.resolve("java.json"), "{}");
        Path report = Files.writeString(directory.resolve("report.json"),
                "{\"valid\":true,\"readyForCrud\":true}");
        TeacherWorkflowContext context = new TeacherWorkflowContext(stateFile);
        context.recordValidationReport(report, ValidationStatus.READY_FOR_CRUD);

        context.selectJavaModel(javaModel);

        assertNull(context.getValidationReport());
        assertEquals(ValidationStatus.NOT_VALIDATED, context.getValidationStatus());
    }

    @Test
    void changingPromptClearsAnOldManualExpectedModel() throws Exception {
        Path stateFile = directory.resolve("workflow.json");
        Path statement = Files.writeString(directory.resolve("statement.txt"), "statement");
        Path oldModel = Files.writeString(directory.resolve("old-validated.json"), "{}");
        Path prompt = Files.writeString(directory.resolve("prompt.txt"), "new prompt");
        TeacherWorkflowContext context = new TeacherWorkflowContext(stateFile);

        context.selectStatementText(statement);
        context.selectInferredModel(oldModel);
        context.selectPromptFile(prompt);

        assertNull(context.getSelectedInferredModel());
        assertNull(context.getSuggestedInferredModel());
    }

    @Test
    void resetClearsPersistedContextWithoutDeletingGeneratedFiles() throws Exception {
        Path stateFile = directory.resolve("workflow.json");
        Path pdf = Files.writeString(directory.resolve("statement.pdf"), "not deleted");
        Path prompt = Files.writeString(directory.resolve("prompt.txt"), "instructions");
        TeacherWorkflowContext context = new TeacherWorkflowContext(stateFile);
        context.selectStatementPdf(pdf);
        context.selectPromptFile(prompt);
        context.selectModelProvider("groq");
        context.selectModel("qwen/qwen3.8-27b");

        assertTrue(context.hasSavedContext());
        context.reset();

        TeacherWorkflowContext restored = new TeacherWorkflowContext(stateFile);
        assertFalse(restored.hasSavedContext());
        assertNull(restored.getStatementPdf());
        assertNull(restored.getSelectedPromptFile());
        assertNull(restored.getSelectedModelProviderId());
        assertNull(restored.getSelectedModelId());
        assertTrue(Files.exists(pdf));
    }
}
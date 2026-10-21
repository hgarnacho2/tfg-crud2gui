package com.crud2gui.modelvalidator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.crud2gui.model.Analysis;
import com.crud2gui.model.ManagerCandidate;
import com.crud2gui.model.ModelIO;
import com.crud2gui.model.ProjectModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.crud2gui.modelvalidator.ModelValidator.Severity.ERROR;
import static com.crud2gui.modelvalidator.ModelValidator.Severity.INFO;
import static com.crud2gui.modelvalidator.ModelValidator.Severity.WARNING;

/** Compara el modelo esperado del enunciado con el extraido del codigo Java. */
public final class ModelComparisonMain {

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private ModelComparisonMain() { }

    /**
    * @param args modelos de entrada, informe opcional y --config=fichero.json
     * @throws Exception si los ficheros no se pueden leer
     */
    public static void main(String[] args) throws Exception {
        List<String> positional = new ArrayList<>();
        Path configFile = null;
        Path reportOption = null;
        for (String arg : args) {
            if (arg.startsWith("--config=")) configFile = Path.of(arg.substring("--config=".length()));
            else if (arg.startsWith("--report=")) reportOption = Path.of(arg.substring("--report=".length()));
            else positional.add(arg);
        }
        if (positional.size() < 2 || positional.size() > 3
                || (positional.size() == 3 && reportOption != null)) {
            System.err.println("Uso: ModelComparisonMain <modelo-esperado.json> <modelo-extraido.json> "
                    + "[informe.json] [--config=fichero.json] [--report=informe.json]");
            System.exit(2);
        }

        Path expectedFile = Path.of(positional.get(0));
        Path actualFile = Path.of(positional.get(1));
        Path reportFile = reportOption != null ? reportOption
                : positional.size() == 3 ? Path.of(positional.get(2)) : defaultReportPath(expectedFile);
        Path effectiveConfigFile = configFile == null ? defaultValidationConfigPath() : configFile;
        if (!Files.isRegularFile(effectiveConfigFile)) {
            throw new IllegalArgumentException("No existe la configuracion de validacion: " + effectiveConfigFile);
        }
        ValidationOptions options = ValidationOptions.fromJson(JSON.readTree(effectiveConfigFile.toFile()));
        ProjectModel expected = ModelIO.read(expectedFile);
        ProjectModel actual = ModelIO.read(actualFile);
        ModelValidator.Result result = ModelValidator.validate(expected, actual, options);
        writeReport(reportFile, expectedFile, actualFile, expected, actual, result, options);

        System.out.println("Resultado: " + (result.isValid() ? "VALID" : "INVALID"));
        for (ModelValidator.Finding finding : result.findings()) {
            System.out.printf("[%s] %s (%s): %s%n", finding.severity(), finding.path(),
                    finding.code(), finding.message());
        }
        System.out.println("Informe JSON: " + reportFile.toAbsolutePath().normalize());
        if (!result.isValid()) {
            System.exit(1);
        }
    }

    static Path defaultValidationConfigPath() throws Exception {
        Path codeLocation = Path.of(ModelComparisonMain.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        Path moduleRoot = codeLocation.getParent().getParent();
        return moduleRoot.resolve("conf").resolve("validation-options.json");
    }

    static Path defaultReportPath(Path expectedFile) {
        String modelName = expectedFile.getFileName().toString();
        int extension = modelName.lastIndexOf('.');
        if (extension > 0) {
            modelName = modelName.substring(0, extension);
        }
        modelName = modelName.replaceAll("[^A-Za-z0-9_-]+", "_").replaceAll("^_+|_+$", "");
        if (modelName.isEmpty()) {
            modelName = "model";
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT));
        String reportName = "validation_" + modelName + "_" + timestamp;
        Path reportFile = Path.of("output", reportName + ".json");
        int suffix = 1;
        while (Files.exists(reportFile)) {
            reportFile = Path.of("output", reportName + "_" + suffix + ".json");
            suffix++;
        }
        return reportFile;
    }

    static void writeReport(Path reportFile, Path expectedFile, Path actualFile,
                            ModelValidator.Result result) throws IOException {
        writeReport(reportFile, expectedFile, actualFile, null, null, result);
    }

    static void writeReport(Path reportFile, Path expectedFile, Path actualFile,
                            ProjectModel expected, ProjectModel actual,
                            ModelValidator.Result result) throws IOException {
        writeReport(reportFile, expectedFile, actualFile, expected, actual, result,
            ValidationOptions.defaults());
        }

        static void writeReport(Path reportFile, Path expectedFile, Path actualFile,
                    ProjectModel expected, ProjectModel actual,
                    ModelValidator.Result result, ValidationOptions options) throws IOException {
        Path parent = reportFile.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent);
        }

        var report = JSON.createObjectNode();
        report.put("valid", result.isValid());
        report.put("expectedModel", expectedFile.toString());
        report.put("actualModel", actualFile.toString());
        report.set("validationOptions", JSON.valueToTree(options));
        ObjectNode manager = managerSelection(expected, actual);
        report.set("managerSelection", manager);
        var confirmations = crudConfirmations(result, manager);
        report.put("readyForCrud", result.isValid() && confirmations.isEmpty());
        report.set("crudConfirmations", confirmations);
        report.set("relationshipDecisions", JSON.valueToTree(result.relationshipDecisions()));

        var summary = report.putObject("summary");
        summary.put("errors", count(result.findings(), ERROR));
        summary.put("warnings", count(result.findings(), WARNING));
        summary.put("info", count(result.findings(), INFO));
        report.set("findings", JSON.valueToTree(result.findings()));

        JSON.writeValue(reportFile.toFile(), report);
    }

    static com.fasterxml.jackson.databind.node.ArrayNode crudConfirmations(
            ModelValidator.Result result, ObjectNode manager) {
        var confirmations = JSON.createArrayNode();
        if (manager.path("requiresConfirmation").asBoolean()) {
            ObjectNode confirmation = JSON.createObjectNode();
            confirmation.put("type", "MANAGER_SELECTION");
            confirmation.put("status", manager.path("status").asText());
            confirmation.put("stage", "BEFORE_CRUD_GENERATION");
            confirmation.put("requiredForCrud", true);
            confirmation.set("expectedManagerId", manager.path("expectedManagerId").deepCopy());
            confirmation.set("selectedManager", manager.path("selectedManager").deepCopy());
            confirmations.add(confirmation);
        }
        for (ModelValidator.RelationshipDecision decision : result.relationshipDecisions()) {
            ObjectNode confirmation = JSON.valueToTree(decision);
            confirmation.put("type", "RELATIONSHIP_KIND");
            confirmation.put("stage", "CRUD_GENERATION");
            confirmation.put("requiredForCrud", true);
            confirmations.add(confirmation);
        }
        return confirmations;
    }

    static ObjectNode managerSelection(ProjectModel expected, ProjectModel actual) {
        String expectedManagerId = expected == null ? null : expected.expectedManagerId();
        Analysis analysis = actual == null ? null : actual.analysis();
        ManagerCandidate selected = analysis == null ? null : analysis.managerClass();
        boolean manualOverride = analysis != null && analysis.manualOverride();
        List<String> managerAmbiguities = expectedManagerAmbiguities(expected);

        String status;
        if (expectedManagerId == null || expectedManagerId.isBlank()) {
            status = managerAmbiguities.isEmpty() ? "NOT_SPECIFIED" : "AMBIGUOUS_EXPECTATION";
        } else if (!containsType(expected, expectedManagerId)) {
            status = "EXPECTED_TYPE_MISSING";
        } else if (selected == null) {
            status = "NOT_DETECTED";
        } else if (!containsType(actual, selected.id())) {
            status = "DETECTED_TYPE_MISSING";
        } else if (simpleTypeName(expectedManagerId).equals(simpleTypeName(selected.id()))) {
            status = "MATCH";
        } else {
            status = "MISMATCH";
        }

        boolean confidenceLow = selected == null || selected.confidence() == com.crud2gui.model.Confidence.LOW;
        boolean statusNeedsReview = status.equals("MISMATCH") || status.equals("NOT_DETECTED")
            || status.equals("AMBIGUOUS_EXPECTATION") || status.endsWith("_TYPE_MISSING");
        boolean requiresConfirmation = statusNeedsReview && !(manualOverride
            && status.equals("AMBIGUOUS_EXPECTATION")) || (!manualOverride && confidenceLow);

        ObjectNode manager = JSON.createObjectNode();
        if (expectedManagerId == null) manager.putNull("expectedManagerId");
        else manager.put("expectedManagerId", expectedManagerId);
        if (selected == null) manager.putNull("selectedManager");
        else manager.set("selectedManager", JSON.valueToTree(selected));
        manager.set("alternatives", JSON.valueToTree(analysis == null ? List.of() : analysis.alternatives()));
        manager.set("expectedManagerAmbiguities", JSON.valueToTree(managerAmbiguities));
        manager.put("status", status);
        manager.put("manualOverride", manualOverride);
        manager.put("requiresConfirmation", requiresConfirmation);
        return manager;
    }

    private static List<String> expectedManagerAmbiguities(ProjectModel expected) {
        if (expected == null || expected.source() == null || expected.source().warnings() == null) {
            return List.of();
        }
        return expected.source().warnings().stream()
                .filter(warning -> warning.startsWith("AMBIGUO:")
                        && warning.toLowerCase(java.util.Locale.ROOT).contains("gestora"))
                .toList();
    }

    private static boolean containsType(ProjectModel model, String id) {
        return model != null && model.types().stream()
                .anyMatch(type -> simpleTypeName(type.id()).equals(simpleTypeName(id)));
    }

    private static String simpleTypeName(String id) {
        if (id == null) return "<null>";
        int separator = Math.max(id.lastIndexOf('.'), id.lastIndexOf('$'));
        return id.substring(separator + 1);
    }

    private static long count(List<ModelValidator.Finding> findings, ModelValidator.Severity severity) {
        return findings.stream().filter(finding -> finding.severity() == severity).count();
    }
}
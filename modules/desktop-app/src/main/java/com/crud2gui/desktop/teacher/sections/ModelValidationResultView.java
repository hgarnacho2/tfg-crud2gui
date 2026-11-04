package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.UiComponents;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.List;

/** Vista gráfica del informe JSON generado por model-validator. */
final class ModelValidationResultView extends VBox {

    ModelValidationResultView(JsonNode report, Path reportFile) {
        super(14);
        setId("model-validation-result-view");
        getStyleClass().add("model-validation-result");

        boolean valid = report.path("valid").asBoolean();
        boolean readyForCrud = report.path("readyForCrud").asBoolean();
        getChildren().addAll(outcome(valid, readyForCrud), summary(report.path("summary")),
                manager(report.path("managerSelection")), relationships(report.path("relationshipDecisions")),
                confirmations(report.path("crudConfirmations")), findings(report.path("findings")));

        Label reportLocation = UiComponents.statusLabel("model-validation-report-path");
        reportLocation.setText("Validación completada. Informe guardado en "
                + reportFile.toAbsolutePath().normalize());
        getChildren().add(reportLocation);
    }

    private static HBox outcome(boolean valid, boolean readyForCrud) {
        Label icon = UiComponents.label(valid ? "✓" : "!", List.of("model-validation-outcome-icon"));
        Label title = UiComponents.label(valid ? "Estructura válida" : "Hay errores estructurales",
                List.of("model-validation-outcome-title"));
        title.setId("model-validation-outcome-title");
        Label subtitle = UiComponents.label(valid
                ? "El modelo Java cumple los requisitos estructurales configurados."
                : "Revisa los errores antes de continuar con la generación del CRUD.",
                List.of("model-validation-outcome-subtitle"));
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(Double.MAX_VALUE);
        VBox details = new VBox(4, title, subtitle);
        HBox.setHgrow(details, Priority.ALWAYS);
        Label readiness = UiComponents.label(readyForCrud ? "Listo para CRUD" : "Pendiente de confirmación",
                List.of("model-validation-readiness", readyForCrud
                        ? "model-validation-readiness-ready" : "model-validation-readiness-pending"));
        readiness.setId("model-validation-readiness");
        HBox banner = UiComponents.hBox(List.of("model-validation-outcome",
                valid ? "model-validation-outcome-valid" : "model-validation-outcome-invalid"),
                icon, details, readiness);
        banner.setId("model-validation-outcome");
        return banner;
    }

    private static HBox summary(JsonNode summary) {
        return UiComponents.hBox(List.of("model-validation-summary"),
                metric("Errores", summary.path("errors").asInt(), "model-validation-metric-errors"),
                metric("Avisos", summary.path("warnings").asInt(), "model-validation-metric-warnings"),
                metric("Información", summary.path("info").asInt(), "model-validation-metric-info"));
    }

    private static VBox metric(String title, int value, String styleClass) {
        Label count = UiComponents.label(Integer.toString(value), List.of("model-validation-metric-count"));
        Label description = UiComponents.label(title, List.of("model-validation-metric-title"));
        VBox metric = UiComponents.vBox(List.of("model-validation-metric", styleClass), count, description);
        metric.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(metric, Priority.ALWAYS);
        return metric;
    }

    private static VBox manager(JsonNode manager) {
        JsonNode selected = manager.path("selectedManager");
        String selectedId = selected.path("id").asText("No detectada");
        String expectedId = manager.path("expectedManagerId").asText("No especificada");
        String status = manager.path("status").asText("Sin datos");
        String confidence = selected.path("confidence").asText("-");
        Label title = UiComponents.label("Clase gestora", List.of("model-validation-block-title"));
        Label comparison = UiComponents.label("Esperada: " + expectedId + "    Detectada: " + selectedId,
                List.of("model-validation-manager-comparison"));
        comparison.setWrapText(true);
        Label state = UiComponents.label("Estado: " + status + "    Confianza: " + confidence,
                List.of("model-validation-manager-state"));
        VBox content = UiComponents.vBox(List.of("model-validation-manager"), title, comparison, state);

        JsonNode reasons = selected.path("reasons");
        if (reasons.isArray() && !reasons.isEmpty()) {
            content.getChildren().add(UiComponents.label("Motivos de la selección",
                    List.of("model-validation-subheading")));
            for (JsonNode reason : reasons) {
                Label item = UiComponents.label("· " + reason.asText(), List.of("model-validation-detail"));
                item.setWrapText(true);
                content.getChildren().add(item);
            }
        }

        JsonNode alternatives = manager.path("alternatives");
        if (alternatives.isArray() && !alternatives.isEmpty()) {
            content.getChildren().add(UiComponents.label("Otras candidatas",
                    List.of("model-validation-subheading")));
            for (JsonNode alternative : alternatives) {
                content.getChildren().add(UiComponents.label(alternative.path("id").asText()
                        + " · " + alternative.path("confidence").asText("-")
                        + " · puntuación " + alternative.path("score").asText("-"),
                        List.of("model-validation-detail")));
            }
        }
        content.setId("model-validation-manager");
        return content;
    }

    private static TitledPane relationships(JsonNode decisions) {
        VBox rows = new VBox(8);
        if (!decisions.isArray() || decisions.isEmpty()) {
            rows.getChildren().add(UiComponents.mutedText("No hay decisiones de relaciones pendientes."));
        } else {
            for (JsonNode decision : decisions) {
                Label relation = UiComponents.label(decision.path("from").asText() + " → "
                        + decision.path("to").asText(), List.of("model-validation-relation-title"));
                Label property = UiComponents.label("Atributo: " + decision.path("via").asText("-"),
                        List.of("model-validation-detail"));
                Label kinds = UiComponents.label(decision.path("expectedKind").asText() + " → "
                        + decision.path("inferredKind").asText(), List.of("model-validation-relation-kinds"));
                Label state = UiComponents.label(decision.path("status").asText(),
                        List.of("model-validation-pending-badge"));
                rows.getChildren().add(UiComponents.hBox(List.of("model-validation-relation"),
                        UiComponents.vBox(List.of("model-validation-relation-details"), relation, property, kinds),
                        state));
            }
        }
        TitledPane pane = detailsPane("Decisiones de relaciones (" + arraySize(decisions) + ")", rows,
                decisions.isArray() && !decisions.isEmpty());
        pane.setId("model-validation-relationships");
        return pane;
    }

    private static TitledPane confirmations(JsonNode confirmations) {
        VBox rows = new VBox(8);
        for (JsonNode confirmation : confirmations) {
            String description = confirmation.path("type").asText("Confirmación") + " · "
                    + confirmation.path("stage").asText("") + " · "
                    + confirmation.path("status").asText("Pendiente");
            Label item = UiComponents.label(description, List.of("model-validation-detail"));
            item.setWrapText(true);
            rows.getChildren().add(item);
        }
        TitledPane pane = detailsPane("Confirmaciones pendientes (" + arraySize(confirmations) + ")",
                rows, true);
        pane.setId("model-validation-confirmations");
        return pane;
    }

    private static TitledPane findings(JsonNode findings) {
        VBox rows = new VBox(8);
        if (!findings.isArray() || findings.isEmpty()) {
            rows.getChildren().add(UiComponents.mutedText("No se encontraron hallazgos."));
        } else {
            for (JsonNode finding : findings) {
                String severity = finding.path("severity").asText("INFO").toLowerCase();
                Label badge = UiComponents.label(finding.path("severity").asText(),
                        List.of("model-validation-severity", "model-validation-severity-" + severity));
                Label path = UiComponents.label(finding.path("path").asText(),
                        List.of("model-validation-finding-path"));
                Label code = UiComponents.label(finding.path("code").asText(),
                        List.of("model-validation-finding-code"));
                Label message = UiComponents.label(finding.path("message").asText(),
                        List.of("model-validation-finding-message"));
                path.setWrapText(true);
                message.setWrapText(true);
                VBox detail = UiComponents.vBox(List.of("model-validation-finding-detail"), path, code, message);
                HBox.setHgrow(detail, Priority.ALWAYS);
                rows.getChildren().add(UiComponents.hBox(List.of("model-validation-finding"), badge, detail));
            }
        }
        TitledPane pane = detailsPane("Hallazgos (" + arraySize(findings) + ")", rows,
                findings.isArray() && !findings.isEmpty());
        pane.setId("model-validation-findings");
        return pane;
    }

    private static TitledPane detailsPane(String title, Node content, boolean expanded) {
        TitledPane pane = new TitledPane(title, content);
        pane.getStyleClass().add("model-validation-details");
        pane.setExpanded(expanded);
        return pane;
    }

    private static int arraySize(JsonNode node) {
        return node.isArray() ? node.size() : 0;
    }
}
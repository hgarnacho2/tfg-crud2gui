package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.UiComponents;
import com.crud2gui.desktop.workflow.ValidationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
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
        ValidationStatus status = report.has("valid")
                ? ValidationStatus.fromReport(valid, readyForCrud) : ValidationStatus.UNKNOWN;
        getChildren().addAll(outcome(valid, status), summary(report.path("summary")),
                manager(report.path("managerSelection")), relationships(report.path("relationshipDecisions")),
                confirmations(report.path("crudConfirmations")), findings(report.path("findings")));

        Label reportLocation = UiComponents.statusLabel("model-validation-report-path");
        reportLocation.setText("Validación completada. Informe guardado en "
                + reportFile.toAbsolutePath().normalize());
        getChildren().add(reportLocation);
    }

        private static HBox outcome(boolean valid, ValidationStatus status) {
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
        HBox readiness = UiComponents.validationStatusBadge(status, "model-validation-readiness");
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
        double score = selected.path("score").asDouble(-1.0);
        
        Label title = UiComponents.label("Clase gestora", List.of("model-validation-block-title"));
        
        // Comparison badges row
        HBox comparisonRow = new HBox(12);
        comparisonRow.getStyleClass().add("model-validation-manager-comparison-row");
        
        // Expected/Inferred from statement
        VBox expectedBox = new VBox(4);
        expectedBox.getStyleClass().add("model-validation-manager-class-box");
        Label expectedLabel = UiComponents.label("Inferida desde el enunciado", 
                List.of("model-validation-manager-class-label"));
        Label expectedValue = UiComponents.label(expectedId, 
                List.of("model-validation-manager-class-value"));
        expectedBox.getChildren().addAll(expectedLabel, expectedValue);
        
        // Selected/Inferred from code
        VBox selectedBox = new VBox(4);
        selectedBox.getStyleClass().add("model-validation-manager-class-box");
        Label selectedLabel = UiComponents.label("Inferida desde el código", 
                List.of("model-validation-manager-class-label"));
        Label selectedValue = UiComponents.label(selectedId, 
                List.of("model-validation-manager-class-value"));
        selectedBox.getChildren().addAll(selectedLabel, selectedValue);
        
        comparisonRow.getChildren().addAll(expectedBox, selectedBox);
        
        // Badges row (status, confidence, score)
        HBox badgesRow = new HBox(8);
        badgesRow.getStyleClass().add("model-validation-manager-badges");
        
        // Status badge
        Label statusBadge = UiComponents.label(statusDisplayName(status), 
                List.of("model-validation-status-badge", "model-validation-status-" + status.toLowerCase()));
        badgesRow.getChildren().add(statusBadge);
        
        // Confidence badge
        if (!"-".equals(confidence)) {
            String confidenceIcon = confidenceIcon(confidence);
            String confidenceLevel = confidence.equals("HIGH") ? "ALTO" : confidence.equals("MEDIUM") ? "MEDIO" : "BAJO";
            String confidenceDisplay = confidenceIcon + " Nivel de certeza: " + confidenceLevel;
            Label confidenceBadge = UiComponents.label(confidenceDisplay, 
                    List.of("model-validation-confidence-badge", "model-validation-confidence-" + confidence.toLowerCase()));
            badgesRow.getChildren().add(confidenceBadge);
        }
        
        // Score badge (if available)
        if (score >= 0) {
            String scoreDisplay = "Puntuación: " + String.format("%.2f", score);
            Label scoreLabel = UiComponents.label(scoreDisplay, 
                    List.of("model-validation-score-value"));
            badgesRow.getChildren().add(scoreLabel);
        }
        
        VBox content = UiComponents.vBox(List.of("model-validation-manager"), title, comparisonRow, badgesRow);

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
            VBox alternativesContainer = new VBox(8);
            alternativesContainer.getStyleClass().add("model-validation-alternatives-container");
            
            for (JsonNode alternative : alternatives) {
                String altId = alternative.path("id").asText("No identificada");
                String altConfidence = alternative.path("confidence").asText("-");
                double altScore = alternative.path("score").asDouble(-1.0);
                
                HBox alternativeRow = new HBox(8);
                alternativeRow.getStyleClass().add("model-validation-alternative-row");
                
                // Class name badge
                Label classNameBadge = UiComponents.label(altId, 
                        List.of("model-validation-alternative-class-badge"));
                alternativeRow.getChildren().add(classNameBadge);
                
                // Confidence badge with score
                if (!"-".equals(altConfidence)) {
                    String confidenceIcon = confidenceIcon(altConfidence);
                    String confidenceLevel = altConfidence.equals("HIGH") ? "ALTO" : altConfidence.equals("MEDIUM") ? "MEDIO" : "BAJO";
                    String confidenceDisplay = confidenceIcon + " " + confidenceLevel + " (" + String.format("%.2f", altScore) + ")";
                    Label confidenceBadge = UiComponents.label(confidenceDisplay, 
                            List.of("model-validation-alternative-confidence-badge", "model-validation-confidence-" + altConfidence.toLowerCase()));
                    alternativeRow.getChildren().add(confidenceBadge);
                }
                
                alternativesContainer.getChildren().add(alternativeRow);
            }
            content.getChildren().add(alternativesContainer);
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
                Label propertyBadge = UiComponents.label("ATRIBUTO",
                        List.of("model-validation-relation-attribute-badge"));
                Label propertyValue = UiComponents.label(decision.path("via").asText("-"),
                        List.of("model-validation-detail"));
                HBox property = UiComponents.hBox(List.of("model-validation-relation-property"),
                        propertyBadge, propertyValue);
                FlowPane kinds = new FlowPane(12, 6);
                kinds.getStyleClass().add("model-validation-relation-kinds");
                kinds.getChildren().addAll(
                        relationKindGroup("ENUNCIADO", relationKindName(decision.path("expectedKind").asText()),
                                "enunciado", "statement"),
                        relationKindGroup("CÓDIGO", relationKindName(decision.path("inferredKind").asText()),
                                "code", "source"));
                Label state = UiComponents.label(relationStatus(decision.path("status").asText()),
                        List.of("model-validation-pending-badge"));
                state.setWrapText(true);
                state.setMaxWidth(250);
                rows.getChildren().add(UiComponents.hBox(List.of("model-validation-relation"),
                        UiComponents.vBox(List.of("model-validation-relation-details"), relation, property, kinds),
                        state));
            }
        }
        TitledPane pane = detailsPane("Avisos de relaciones entre clases (" + arraySize(decisions) + ")", rows,
                true);
        pane.setId("model-validation-relationships");
        return pane;
    }

        private static HBox relationKindGroup(String origin, String kind, String originStyle, String kindStyle) {
                Label badge = UiComponents.label(origin,
                                List.of("model-validation-relation-origin-badge", "model-validation-relation-origin-" + originStyle));
                Label value = UiComponents.label(kind,
                                List.of("model-validation-relation-kind-value", "model-validation-relation-kind-" + kindStyle));
                return UiComponents.hBox(List.of("model-validation-relation-kind-group"), badge, value);
        }

        private static String relationKindName(String kind) {
                return switch (kind) {
                        case "ASSOCIATION" -> "Asociación";
                        case "COMPOSITION" -> "Composición";
                        case "AGGREGATION" -> "Agregación";
                        case "INHERITANCE" -> "Herencia";
                        case "REALIZATION" -> "Realización";
                        default -> kind;
                };
        }

        private static String relationStatus(String status) {
                return switch (status) {
                        case "PENDING_CONFIRMATION" -> "Se confirmará al generar el CRUD";
                        default -> status;
                };
        }

        private static String statusDisplayName(String status) {
                return switch (status) {
                        case "MATCH" -> "✓ Las clases gestoras coinciden";
                        case "MISMATCH" -> "⚠ Mismatch: detectada diferente a la esperada";
                        case "NOT_DETECTED" -> "✗ Clase gestora no detectada";
                        case "NOT_SPECIFIED" -> "? Clase gestora no especificada en el enunciado";
                        case "AMBIGUOUS_EXPECTATION" -> "⚡ Especificación ambigua del enunciado";
                        case "EXPECTED_TYPE_MISSING" -> "✗ Clase esperada falta en el enunciado";
                        case "DETECTED_TYPE_MISSING" -> "✗ Clase detectada falta en el código";
                        default -> status;
                };
        }

        private static String confidenceDisplayName(String confidence) {
                return switch (confidence) {
                        case "HIGH" -> "🎯 ALTO";
                        case "MEDIUM" -> "⚠ MEDIO";
                        case "LOW" -> "❌ BAJO";
                        default -> confidence;
                };
        }

        private static String confidenceIcon(String confidence) {
                return switch (confidence) {
                        case "HIGH" -> "🎯";
                        case "MEDIUM" -> "⚠";
                        case "LOW" -> "❌";
                        default -> "";
                };
        }

    private static TitledPane confirmations(JsonNode confirmations) {
        VBox rows = new VBox(8);
                if (!confirmations.isArray() || confirmations.isEmpty()) {
                        rows.getChildren().add(UiComponents.mutedText("No hay confirmaciones pendientes."));
                } else {
                        for (JsonNode confirmation : confirmations) {
                                String description = confirmation.path("type").asText("Confirmación") + " · "
                                                + confirmation.path("stage").asText("") + " · "
                                                + confirmation.path("status").asText("Pendiente");
                                Label item = UiComponents.label(description, List.of("model-validation-detail"));
                                item.setWrapText(true);
                                rows.getChildren().add(item);
                        }
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
        TitledPane pane = detailsPane("Informe de la validación (" + arraySize(findings) + ")", rows,
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
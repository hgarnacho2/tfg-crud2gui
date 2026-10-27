package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.desktop.ui.PdfPreview;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StatementSectionTest {

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
    void appliesJsonRulesReloadsChangesAndRejectsInvalidOrMissingConfiguration() throws Exception {
        Path pdf = createPdf("custom-rules.pdf", "Antes\nInicio\nContenido\nFin\nDespues", false);
        Path config = directory.resolve("statement-filter-rules.json");
        String rules = """
                {
                  "sectionRules": [
                    {"heading": "Inicio", "action": "START"},
                    {"heading": "Fin", "action": "STOP"}
                  ],
                  "maxConsecutiveBlankLines": 2,
                  "removePageNumbers": true
                }
                """;
        Files.writeString(config, rules);
        String previous = System.getProperty("STATEMENT_FILTER_CONFIG_PATH");
        System.setProperty("STATEMENT_FILTER_CONFIG_PATH", config.toString());
        try {
            Controls controls = onFx(() -> controls(new AtomicReference<>(pdf.toFile())));
            onFx(() -> {
                controls.select().fire();
                return null;
            });
            extract(controls);
            onFx(() -> {
                controls.step(3).fire();
                return null;
            });
            runOperation(controls, controls.filter());
            Path output = directory.resolve("custom-rules-filtrado.txt");
            assertEquals("Inicio\nContenido\n", Files.readString(output));
            onFx(() -> {
                assertTrue(controls.step(3).getPseudoClassStates()
                        .contains(PseudoClass.getPseudoClass("completed")));
                return null;
            });
            Files.writeString(config, rules.replace("\"Inicio\"", "\"Contenido\""));
            runOperation(controls, controls.filter());
            String lastResult = Files.readString(output);
            assertEquals("Contenido\n", lastResult);

            for (String invalid : List.of("{invalid", rules.replace("\"START\"", "\"IGNORE\""))) {
                Files.writeString(config, invalid);
                runOperation(controls, controls.filter());
                assertEquals(lastResult, Files.readString(output));
                onFx(() -> {
                    assertEquals("", controls.filtered().getText());
                    assertTrue(controls.status().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
                    assertTrue(controls.status().getText().contains("statement-filter-rules.json"));
                    assertFalse(controls.step(3).getPseudoClassStates()
                            .contains(PseudoClass.getPseudoClass("completed")));
                    return null;
                });
            }
            Files.delete(config);
            Files.delete(output);
            runOperation(controls, controls.filter());
            assertFalse(Files.exists(output));
            onFx(() -> {
                assertTrue(controls.status().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
                assertFalse(controls.filter().isDisabled());
                return null;
            });
        } finally {
            if (previous == null) {
                System.clearProperty("STATEMENT_FILTER_CONFIG_PATH");
            } else {
                System.setProperty("STATEMENT_FILTER_CONFIG_PATH", previous);
            }
        }
    }

    @Test
    void expandsConfigurationAsReadOnlyJsonAndReloadsAfterChanges() throws Exception {
        Path config = directory.resolve("statement-filter-rules.json");
        String original = "{\n  \"removePageNumbers\": true\n}";
        Files.writeString(config, original);
        onFx(() -> {
            String previous = System.getProperty("STATEMENT_FILTER_CONFIG_PATH");
            System.setProperty("STATEMENT_FILTER_CONFIG_PATH", config.toString());
            try {
                Controls controls = controls(new AtomicReference<>());
                TitledPane pane = (TitledPane) controls.section().lookup("#statement-filter-config");
                assertFalse(pane.isExpanded());
                TextArea content = (TextArea) pane.getContent().lookup("#statement-filter-config-content");
                assertEquals("", content.getText());
                assertFalse(content.isEditable());
                assertFalse(content.isWrapText());
                pane.setExpanded(true);
                assertEquals(original, content.getText());
                assertEquals(config.toString(),
                        ((Label) pane.getContent().lookup("#statement-filter-config-path")).getText());
                assertFalse(pane.getContent().lookup("#statement-filter-config-error").isManaged());
                pane.setExpanded(false);
                String updated = "{\n  \"removePageNumbers\": false\n}";
                Files.writeString(config, updated);
                pane.setExpanded(true);
                assertEquals(updated, content.getText());
                assertTrue(content.isManaged());
            } finally {
                if (previous == null) {
                    System.clearProperty("STATEMENT_FILTER_CONFIG_PATH");
                } else {
                    System.setProperty("STATEMENT_FILTER_CONFIG_PATH", previous);
                }
            }
            return null;
        });
    }

    @Test
    void reportsMissingConfigurationAndRecoversOnNextExpansion() throws Exception {
        Path config = directory.resolve("missing.json");
        onFx(() -> {
            String previous = System.getProperty("STATEMENT_FILTER_CONFIG_PATH");
            System.setProperty("STATEMENT_FILTER_CONFIG_PATH", config.toString());
            try {
                Controls controls = controls(new AtomicReference<>());
                TitledPane pane = (TitledPane) controls.section().lookup("#statement-filter-config");
                pane.setExpanded(true);
                Label error = (Label) pane.getContent().lookup("#statement-filter-config-error");
                TextArea content = (TextArea) pane.getContent().lookup("#statement-filter-config-content");
                assertTrue(error.isManaged());
                assertTrue(error.getText().contains("No se pudo leer"));
                assertFalse(content.isManaged());
                assertEquals("", content.getText());
                pane.setExpanded(false);
                Files.writeString(config, "{}");
                pane.setExpanded(true);
                assertEquals("{}", content.getText());
                assertTrue(content.isManaged());
                assertFalse(error.isManaged());
            } finally {
                if (previous == null) {
                    System.clearProperty("STATEMENT_FILTER_CONFIG_PATH");
                } else {
                    System.setProperty("STATEMENT_FILTER_CONFIG_PATH", previous);
                }
            }
            return null;
        });
    }

    @Test
    void rendersPanelSubtitlesAndTxtDestinationCards() throws Exception {
        Path pdf = createPdf("destination-card.pdf", "Texto del enunciado.", false);
        onFx(() -> {
            Controls controls = controls(new AtomicReference<>(pdf.toFile()));
            controls.select().fire();
            controls.section().applyCss();
            var subtitles = controls.section().lookupAll(".panel-subtitle");
            assertEquals(3, subtitles.size());
            for (var node : subtitles) {
                Label subtitle = (Label) node;
                assertEquals(15, subtitle.getFont().getSize(), 0.01);
                assertEquals(8, ((VBox) subtitle.getParent()).getSpacing(), 0.01);
            }
            var settings = controls.section().lookupAll(".statement-output-settings");
            assertEquals(2, settings.size());
            for (var node : settings) {
                VBox card = (VBox) node.lookup(".statement-file-card");
                Label name = (Label) card.lookup(".statement-file-name");
                Label badge = (Label) name.getGraphic();
                Label path = (Label) card.lookup(".statement-output-folder");
                assertEquals("TXT", badge.getText());
                assertTrue(name.getText().equals("destination-card.txt")
                        || name.getText().equals("destination-card-filtrado.txt"));
                assertEquals(directory.resolve(name.getText()).toString(), path.getText());
                assertTrue(card.getPseudoClassStates().contains(PseudoClass.getPseudoClass("selected")));
                assertEquals(javafx.scene.paint.Color.web("#e4f3ec"),
                        card.getBackground().getFills().get(0).getFill());
            }
            return null;
        });
    }

    @Test
    void keepsResultsInsideTheirPanelsAndClearsInvalidatedMessages() throws Exception {
        Path pdf = createPdf("panel-status.pdf", "Introducción\nContenido del enunciado.", false);
        AtomicReference<File> selection = new AtomicReference<>(pdf.toFile());
        Controls controls = onFx(() -> controls(selection));
        onFx(() -> {
            Label selectionStatus = (Label) controls.section().lookup("#statement-selection-status");
            Label extractionStatus = (Label) controls.section().lookup("#statement-extraction-status");
            Label filterStatus = (Label) controls.section().lookup("#statement-filter-status");
            assertFalse(selectionStatus.isManaged());
            assertFalse(extractionStatus.isManaged());
            assertFalse(filterStatus.isManaged());
            assertNotSame(selectionStatus.getParent(), extractionStatus.getParent());
            assertNotSame(extractionStatus.getParent(), filterStatus.getParent());
            assertNotSame(controls.section(), selectionStatus.getParent());
            var openButtons = controls.section().lookupAll(".statement-open-folder");
            assertEquals(2, openButtons.size());
            for (var button : openButtons) {
                assertTrue(button.getParent() == extractionStatus.getParent()
                        || button.getParent() == filterStatus.getParent());
                var siblings = button.getParent().getChildrenUnmodifiable();
                assertTrue(siblings.indexOf(button) < siblings.indexOf(
                        button.getParent() == extractionStatus.getParent() ? extractionStatus : filterStatus));
            }
            for (Label label : List.of(selectionStatus, extractionStatus, filterStatus)) {
                var siblings = label.getParent().getChildrenUnmodifiable();
                assertSame(label, siblings.get(siblings.size() - 1));
            }
            controls.select().fire();
            assertTrue(selectionStatus.getText().startsWith("PDF seleccionado"));
            assertTrue(selectionStatus.getText().contains("Puedes continuar con el paso 2: Extraer texto."));
            assertTrue(selectionStatus.isManaged());
            assertEquals("", extractionStatus.getText());
            return null;
        });
        extract(controls);
        onFx(() -> {
            Label extractionStatus = (Label) controls.section().lookup("#statement-extraction-status");
            assertTrue(extractionStatus.getText().contains("Archivo generado:"));
            assertTrue(extractionStatus.getText().contains("Puedes continuar con el paso 3: Filtrar texto."));
            assertTrue(extractionStatus.getText().contains("panel-status.txt"));
            controls.step(1).fire();
            assertTrue(controls.status().getText().startsWith("PDF seleccionado"));
            controls.step(2).fire();
            assertSame(extractionStatus, controls.status());
            controls.step(3).fire();
            assertEquals("", controls.status().getText());
            return null;
        });
        runOperation(controls, controls.filter());
        onFx(() -> {
            assertTrue(controls.status().getText().contains("panel-status-filtrado.txt"));
            controls.step(2).fire();
            assertTrue(controls.status().getText().startsWith("Texto extraído"));
            return null;
        });
        extract(controls);
        onFx(() -> {
            assertEquals("", ((Label) controls.section().lookup("#statement-filter-status")).getText());
            selection.set(createPdf("new-panel-status.pdf", "Otro documento.", false).toFile());
            controls.step(1).fire();
            controls.select().fire();
            assertEquals("", ((Label) controls.section().lookup("#statement-extraction-status")).getText());
            assertEquals("", ((Label) controls.section().lookup("#statement-filter-status")).getText());
            return null;
        });
    }

    @Test
    void rendersPdfBadgeAsNeutralDocumentLabel() throws Exception {
        onFx(() -> {
            Controls controls = controls(new AtomicReference<>());
            Label badge = (Label) controls.section().lookup(".statement-pdf-badge");
            assertEquals("PDF", badge.getText());
            assertEquals(javafx.scene.paint.Color.web("#f8faf9"),
                    badge.getBackground().getFills().get(0).getFill());
            assertEquals(javafx.scene.paint.Color.web("#52675d"), badge.getTextFill());
            assertEquals(javafx.scene.paint.Color.web("#b7c9c2"),
                    badge.getBorder().getStrokes().get(0).getTopStroke());
            assertEquals(new javafx.geometry.Insets(4, 6, 4, 6), badge.getPadding());
            assertNotEquals(controls.select().getBackground().getFills().get(0).getFill(),
                    badge.getBackground().getFills().get(0).getFill());
            return null;
        });
    }

    @Test
    void startsWithOnlyFirstStepEnabledAndCancellationKeepsState() throws Exception {
        onFx(() -> {
            Controls controls = controls(new AtomicReference<>());
            assertNull(controls.section().lookup(".tab-pane"));
            assertFalse(controls.step(1).isDisabled());
            assertTrue(controls.step(2).isDisabled());
            assertTrue(controls.step(3).isDisabled());
            assertTrue(controls.extract().isDisabled());
            assertTrue(controls.filter().isDisabled());
            controls.step(2).fire();
            assertSelected(controls, 1);
            controls.select().fire();
            var settings = controls.section().lookup(".statement-output-settings");
            assertFalse(isEffectivelyVisible(settings));
            assertFalse(isEffectivelyVisible(controls.section().lookup(".statement-open-folder")));
            assertTrue(controls.step(2).isDisabled());
            assertSelected(controls, 1);
            return null;
        });
    }

    @Test
    void extractsRealPdfAndResetsResultOnlyWhenAnotherFileIsSelected() throws Exception {
        Path pdf = createPdf("first.pdf", "Enunciado de prueba: crear una clase Persona.", false);
        AtomicReference<File> selection = new AtomicReference<>(pdf.toFile());
        Controls controls = onFx(() -> controls(selection));
        onFx(() -> {
            controls.select().fire();
            assertFalse(controls.step(2).isDisabled());
            controls.section().applyCss();
            Label filename = (Label) controls.section().lookup(".statement-file-name");
            assertEquals("first.pdf", filename.getText());
            assertEquals(16, filename.getFont().getSize(), 0.01);
            assertTrue(filename.getFont().getStyle().contains("Bold"));
            assertTrue(filename.isWrapText());
            assertEquals(pdf.toFile().getAbsolutePath(),
                    ((Label) controls.section().lookup(".statement-file-path")).getText());
            assertTrue(controls.section().lookup(".statement-file-card").getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("selected")));
            assertTrue(controls.step(3).isDisabled());
            assertSelected(controls, 1);
            return null;
        });
        extract(controls);
        onFx(() -> {
            assertEquals("Enunciado de prueba: crear una clase Persona.", controls.text().getText().strip());
            assertFalse(controls.text().isEditable());
            controls.section().applyCss();
            assertEquals(15, controls.status().getFont().getSize(), 0.01);
            assertTrue(controls.status().getFont().getStyle().contains("Bold"));
            assertEquals(javafx.scene.paint.Color.web("#135c48"), controls.status().getTextFill());
            assertNotNull(controls.status().getBackground());
            assertFalse(controls.step(3).isDisabled());
            assertSelected(controls, 2);
            controls.step(1).fire();
            selection.set(null);
            controls.select().fire();
            assertFalse(controls.text().getText().isBlank());
            assertSelected(controls, 1);
            return null;
        });
        Path second = createPdf("second.pdf", "Segundo enunciado.", false);
        selection.set(second.toFile());
        onFx(() -> {
            controls.select().fire();
            assertEquals("", controls.text().getText());
            assertTrue(controls.step(3).isDisabled());
            assertSelected(controls, 1);
            return null;
        });
        extract(controls);
        onFx(() -> {
            assertEquals("Segundo enunciado.", controls.text().getText().strip());
            return null;
        });
        assertEquals("Enunciado de prueba: crear una clase Persona.",
                Files.readString(directory.resolve("first.txt")).strip());
        assertEquals("Segundo enunciado.", Files.readString(directory.resolve("second.txt")).strip());
    }

    @Test
    void reportsInvalidProtectedAndMissingPdfsAndAllowsRecovery() throws Exception {
        Path corrupt = directory.resolve("corrupt.pdf");
        Files.writeString(corrupt, "Not a PDF");
        Path protectedPdf = createPdf("protected.pdf", "Contenido protegido.", true);
        AtomicReference<File> selection = new AtomicReference<>();
        Controls controls = onFx(() -> controls(selection));
        for (Path pdf : new Path[]{corrupt, protectedPdf, directory.resolve("missing.pdf")}) {
            selection.set(pdf.toFile());
            onFx(() -> {
                controls.select().fire();
                return null;
            });
            extract(controls);
            onFx(() -> {
                assertTrue(controls.status().getText().startsWith("No se pudo extraer"));
                assertTrue(controls.status().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
                assertTrue(controls.step(3).isDisabled());
                assertFalse(controls.select().isDisabled());
                assertFalse(controls.extract().isDisabled());
                return null;
            });
        }
        selection.set(createPdf("recovered.pdf", "Lectura recuperada.", false).toFile());
        onFx(() -> {
            controls.select().fire();
            return null;
        });
        extract(controls);
        onFx(() -> {
            assertEquals("Lectura recuperada.", controls.text().getText().strip());
            assertFalse(controls.status().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            return null;
        });
    }

    @Test
    void reportsPdfWithoutExtractableText() throws Exception {
        AtomicReference<File> selection = new AtomicReference<>(
                createPdf("empty.pdf", "", false).toFile());
        Controls controls = onFx(() -> controls(selection));
        onFx(() -> {
            controls.select().fire();
            return null;
        });
        extract(controls);
        onFx(() -> {
            assertTrue(controls.status().getText().contains("OCR"));
            assertTrue(controls.step(3).isDisabled());
            assertEquals("", controls.text().getText());
            return null;
        });
        assertFalse(Files.exists(directory.resolve("empty.txt")));
    }

    @Test
    void filtersInOrderAndInvalidatesDownstreamResultsOnNewSelectionAndExtraction() throws Exception {
        String content = "Portada\nIntroducción\nCrear una clase Persona.\n"
                + "Requisitos previos\nContenido omitido.\n"
                + "2. Modelo\nAtributos del modelo.\nFechas y normas de entrega\nFin.";
        Path pdf = createPdf("flow.pdf", content, false);
        AtomicReference<File> selection = new AtomicReference<>(pdf.toFile());
        Controls controls = onFx(() -> controls(selection));
        onFx(() -> {
            controls.select().fire();
            controls.step(3).fire();
            assertSelected(controls, 1);
            return null;
        });
        extract(controls);
        onFx(() -> {
            controls.step(3).fire();
            assertSelected(controls, 3);
            assertTrue(controls.filtered().getText().isEmpty());
            assertTrue(controls.filtered().isVisible());
            assertFalse(controls.filtered().isEditable());
            assertFalse(controls.text().getParent().getParent().isVisible());
            return null;
        });
        runOperation(controls, controls.filter());
        assertTrue(Files.readString(directory.resolve("flow.txt")).contains("Contenido omitido."));
        assertFalse(Files.readString(directory.resolve("flow-filtrado.txt")).contains("Contenido omitido."));
        onFx(() -> {
            String filtered = controls.filtered().getText();
            assertEquals(filtered, Files.readString(directory.resolve("flow-filtrado.txt")));
            assertTrue(filtered.contains("Crear una clase Persona."));
            assertTrue(filtered.contains("Atributos del modelo."));
            assertFalse(filtered.contains("Portada"));
            assertFalse(filtered.contains("Contenido omitido."));
            assertFalse(filtered.contains("Fechas y normas de entrega"));
            assertTrue(controls.text().getText().contains("Contenido omitido."));
            assertTrue(controls.step(3).getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
            controls.step(1).fire();
            controls.step(3).fire();
            assertEquals(filtered, controls.filtered().getText());
            controls.step(2).fire();
            return null;
        });
        extract(controls);
        onFx(() -> {
            assertEquals("", controls.filtered().getText());
            assertFalse(controls.step(3).getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
            return null;
        });
        selection.set(createPdf("replacement.pdf", "Otro enunciado.", false).toFile());
        onFx(() -> {
            controls.step(1).fire();
            controls.select().fire();
            assertTrue(controls.step(3).isDisabled());
            assertTrue(controls.filter().isDisabled());
            assertEquals("", controls.text().getText());
            assertEquals("", controls.filtered().getText());
            return null;
        });
    }

    @Test
    void warnsWhenFilterDoesNotRecognizeStructureWithoutClaimingCompletion() throws Exception {
        AtomicReference<File> selection = new AtomicReference<>(
                createPdf("unrecognized.pdf", "Texto sin estructura conocida.", false).toFile());
        Controls controls = onFx(() -> controls(selection));
        onFx(() -> {
            controls.select().fire();
            return null;
        });
        extract(controls);
        onFx(() -> {
            controls.step(3).fire();
            return null;
        });
        runOperation(controls, controls.filter());
        onFx(() -> {
            assertEquals(controls.text().getText(), controls.filtered().getText());
            assertTrue(controls.status().getText().contains("sin filtrar"));
            assertTrue(controls.status().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            assertFalse(controls.step(3).getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
            assertFalse(controls.filter().isDisabled());
            return null;
        });
    }

    @Test
    void changesOutputFolderConfirmsOverwriteAndReportsWriteFailures() throws Exception {
        Path pdf = createPdf("output.pdf", "Texto para guardar.", false);
        Path destination = Files.createDirectory(directory.resolve("results"));
        AtomicReference<File> folder = new AtomicReference<>(destination.toFile());
        java.util.concurrent.atomic.AtomicBoolean overwrite = new java.util.concurrent.atomic.AtomicBoolean(false);
        StatementSection section = onFx(() -> new StatementSection(owner -> pdf.toFile(),
                (owner, current) -> folder.get(), path -> overwrite.get()));
        Controls controls = onFx(() -> {
            Scene scene = new Scene(section, 960, 680);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/styles/app.css")).toExternalForm());
            section.applyCss();
            return new Controls(section, (Button) section.lookup(".statement-select"),
                    (Button) section.lookup(".statement-extract"), (Button) section.lookup(".statement-filter"),
                    (TextArea) section.lookup("#statement-raw-text"), (Label) section.lookup(".status-message"),
                    (TextArea) section.lookup(".statement-filtered-text"));
        });
        onFx(() -> {
            controls.select().fire();
            assertTrue(((Label) section.lookup(".statement-output-folder")).getText().contains(directory.toString()));
            assertFalse(isEffectivelyVisible(section.lookup(".statement-output-settings")));
            controls.step(2).fire();
            assertTrue(isEffectivelyVisible(section.lookup(".statement-output-settings")));
            VBox extractionContent = (VBox) controls.text().getParent();
            assertSame(section.lookup(".statement-output-settings"), extractionContent.getChildren().get(1));
            Button changeFolder = (Button) section.lookup(".statement-change-folder");
            Button openFolder = (Button) section.lookup(".statement-open-folder");
            assertTrue(changeFolder.getStyleClass().contains("secondary-button"));
            assertTrue(openFolder.getStyleClass().contains("secondary-button"));
            assertTrue(openFolder.isVisible());
            assertTrue(openFolder.isManaged());
            ((Button) section.lookup(".statement-change-folder")).fire();
            assertTrue(((Label) section.lookup(".statement-output-folder")).getText().contains(destination.toString()));
            folder.set(null);
            ((Button) section.lookup(".statement-change-folder")).fire();
            assertTrue(((Label) section.lookup(".statement-output-folder")).getText().contains(destination.toString()));
            return null;
        });
        Path output = destination.resolve("output.txt");
        Files.writeString(output, "No reemplazar");
        onFx(() -> {
            controls.step(2).fire();
            controls.extract().fire();
            assertFalse(controls.extract().isDisabled());
            assertTrue(controls.status().getText().contains("cancelada"));
            return null;
        });
        assertEquals("No reemplazar", Files.readString(output));
        overwrite.set(true);
        extract(controls);
        assertEquals("Texto para guardar.", Files.readString(output).strip());
        onFx(() -> {
            controls.step(3).fire();
            return null;
        });
        Path filteredOutput = destination.resolve("output-filtrado.txt");
        Files.writeString(filteredOutput, "Filtro previo");
        overwrite.set(false);
        onFx(() -> {
            controls.filter().fire();
            assertTrue(controls.status().getText().contains("cancelada"));
            assertFalse(controls.filter().isDisabled());
            return null;
        });
        assertEquals("Filtro previo", Files.readString(filteredOutput));
        overwrite.set(true);
        runOperation(controls, controls.filter());
        assertEquals(Files.readString(output).replace("\r\n", "\n"), Files.readString(filteredOutput));
        onFx(() -> {
            assertTrue(controls.status().getText().contains(filteredOutput.toString()));
            assertTrue(section.lookupAll(".statement-output-file").isEmpty());
            assertTrue(isEffectivelyVisible(controls.filtered().getParent().lookup(".statement-output-settings")));
            controls.step(1).fire();
            for (var settings : section.lookupAll(".statement-output-settings")) {
                assertFalse(isEffectivelyVisible(settings));
            }
            for (var button : section.lookupAll(".statement-open-folder")) {
                assertFalse(isEffectivelyVisible(button));
            }
            controls.step(3).fire();
            VBox filterContent = (VBox) controls.filtered().getParent();
            var filterSettings = filterContent.lookup(".statement-output-settings");
            assertTrue(isEffectivelyVisible(filterSettings));
            assertSame(filterSettings, filterContent.getChildren().get(1));
            assertTrue(isEffectivelyVisible(filterContent.lookup(".statement-open-folder")));
            return null;
        });
        Path invalidDestination = directory.resolve("not-a-folder");
        Files.writeString(invalidDestination, "Un archivo no es una carpeta");
        folder.set(invalidDestination.toFile());
        onFx(() -> {
            ((Button) section.lookup(".statement-change-folder")).fire();
            return null;
        });
        extract(controls);
        runOperation(controls, onFx(() -> (Button) section.lookup(".statement-open-folder")));
        onFx(() -> {
            assertTrue(controls.status().getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            assertTrue(controls.status().getText().contains(invalidDestination.toString()));
            assertEquals("", controls.text().getText());
            assertTrue(controls.step(3).isDisabled());
            assertTrue(section.lookupAll(".statement-output-file").isEmpty());
            return null;
        });
        assertEquals("Texto para guardar.", Files.readString(output).strip());
    }

    @Test
    void previewsPagesWithZoomAndResetsOnReplacement() throws Exception {
        Path original = createPdf("preview-source.pdf", "Primera pagina.", false);
        Path pdf = directory.resolve("preview.pdf");
        try (PDDocument document = org.apache.pdfbox.Loader.loadPDF(original.toFile())) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }
        PdfPreview preview = onFx(() -> {
            PdfPreview viewer = new PdfPreview();
            Scene scene = new Scene(viewer, 960, 680);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/styles/app.css")).toExternalForm());
            viewer.applyCss();
            return viewer;
        });
        awaitPreview(preview, () -> preview.load(pdf));
        onFx(() -> {
            assertEquals("Página 1 de 2 · 100 %",
                    ((Label) preview.lookup(".pdf-preview-page")).getText());
            assertTrue(((Button) preview.lookup(".preview-previous")).isDisabled());
            assertFalse(((Button) preview.lookup(".preview-next")).isDisabled());
            var image = (javafx.scene.image.ImageView) preview.lookup(".pdf-preview-image");
            assertEquals(900, image.getImage().getWidth(), 1);
            assertEquals(900, image.getFitWidth(), 0.1);
            assertFullPageHeight(preview);
            return null;
        });
        awaitPreview(preview, () -> ((Button) preview.lookup(".preview-next")).fire());
        onFx(() -> {
            assertEquals("Página 2 de 2 · 100 %",
                    ((Label) preview.lookup(".pdf-preview-page")).getText());
            assertTrue(((Button) preview.lookup(".preview-next")).isDisabled());
            return null;
        });
        awaitPreview(preview, () -> ((Button) preview.lookup(".preview-zoom-in")).fire());
        onFx(() -> {
            assertEquals("Página 2 de 2 · 125 %",
                    ((Label) preview.lookup(".pdf-preview-page")).getText());
            assertEquals(1125, ((javafx.scene.image.ImageView) preview.lookup(".pdf-preview-image"))
                    .getFitWidth(), 0.1);
            assertFullPageHeight(preview);
            return null;
        });
        Path replacement = createPdf("preview-replacement.pdf", "Nuevo PDF.", false);
        awaitPreview(preview, () -> preview.load(replacement));
        onFx(() -> {
            assertEquals("Página 1 de 1 · 100 %",
                    ((Label) preview.lookup(".pdf-preview-page")).getText());
            assertTrue(((Button) preview.lookup(".preview-previous")).isDisabled());
            assertTrue(((Button) preview.lookup(".preview-next")).isDisabled());
            return null;
        });
        // El visor no conserva abierto ninguno de los documentos.
        Files.delete(pdf);
        Files.delete(replacement);
    }

    private static void awaitPreview(PdfPreview preview, Runnable action) throws Exception {
        CompletableFuture<Void> loaded = onFx(() -> {
            CompletableFuture<Void> result = new CompletableFuture<>();
            var image = (javafx.scene.image.ImageView) preview.lookup(".pdf-preview-image");
            javafx.beans.value.ChangeListener<javafx.scene.image.Image> listener =
                    new javafx.beans.value.ChangeListener<>() {
                        @Override
                        public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.image.Image> value,
                                            javafx.scene.image.Image oldImage, javafx.scene.image.Image newImage) {
                            if (newImage != null) {
                                image.imageProperty().removeListener(this);
                                result.complete(null);
                            }
                        }
                    };
            image.imageProperty().addListener(listener);
            action.run();
            assertTrue(((Button) preview.lookup(".preview-next")).isDisabled());
            return result;
        });
        loaded.get(15, TimeUnit.SECONDS);
    }

    private static void assertFullPageHeight(PdfPreview preview) {
        preview.applyCss();
        preview.resize(960, preview.prefHeight(960));
        preview.layout();
        var viewport = (javafx.scene.control.ScrollPane) preview.lookup(".pdf-preview-viewport");
        var image = (javafx.scene.image.ImageView) preview.lookup(".pdf-preview-image");
        double pageHeight = image.getBoundsInLocal().getHeight();
        assertTrue(pageHeight > 460);
        assertTrue(viewport.getPrefViewportHeight() >= pageHeight + 36);
        assertTrue(viewport.getViewportBounds().getHeight() >= pageHeight + 36);
    }

    @Test
    void reportsPreviewFailureAndDiscardsPreviousDocumentWhenSelectionChangesQuickly() throws Exception {
        PdfPreview preview = onFx(() -> {
            PdfPreview viewer = new PdfPreview();
            new Scene(viewer);
            viewer.applyCss();
            return viewer;
        });
        CompletableFuture<Void> failed = onFx(() -> {
            CompletableFuture<Void> result = new CompletableFuture<>();
            Label status = (Label) preview.lookup(".pdf-preview-status");
            status.textProperty().addListener((observable, previous, current) -> {
                if (current.startsWith("No se pudo previsualizar")) {
                    result.complete(null);
                }
            });
            preview.load(directory.resolve("missing-preview.pdf"));
            return result;
        });
        failed.get(15, TimeUnit.SECONDS);
        onFx(() -> {
            assertNull(((javafx.scene.image.ImageView) preview.lookup(".pdf-preview-image")).getImage());
            assertTrue(((Button) preview.lookup(".preview-next")).isDisabled());
            assertTrue(((Button) preview.lookup(".preview-zoom-in")).isDisabled());
            return null;
        });
        Path first = createPdf("first-preview.pdf", "Primer documento.", false);
        Path second = createPdf("second-preview.pdf", "Documento definitivo.", false);
        awaitPreview(preview, () -> {
            preview.load(first);
            preview.load(second);
        });
        onFx(() -> {
            assertEquals("", ((Label) preview.lookup(".pdf-preview-status")).getText());
            assertEquals("Página 1 de 1 · 100 %",
                    ((Label) preview.lookup(".pdf-preview-page")).getText());
            return null;
        });
    }

    @Test
    void rendersConnectedStepCardsInARealWindow() throws Exception {
        onFx(() -> {
            Controls controls = controls(new AtomicReference<>());
            Stage stage = new Stage();
            try {
                stage.setScene(controls.section().getScene());
                stage.show();
                controls.section().applyCss();
                var subtitles = controls.section().lookupAll(".panel-subtitle");
                assertEquals(3, subtitles.size());
                for (var node : subtitles) {
                    Label subtitle = assertInstanceOf(Label.class, node);
                    assertFalse(subtitle.getText().isBlank());
                    assertEquals(15, subtitle.getFont().getSize(), 0.01);
                    assertEquals(8, ((VBox) subtitle.getParent()).getSpacing(), 0.01);
                }
                for (int width : new int[]{760, 960, 1800}) {
                    controls.section().resize(width, 680);
                    controls.section().applyCss();
                    controls.section().layout();
                    assertTrue(controls.select().getBoundsInParent().getWidth() > 0);
                    assertSelected(controls, 1);
                    for (int i = 1; i <= 3; i++) {
                        Button step = controls.step(i);
                        assertTrue(step.getWidth() > 180);
                        assertTrue(step.getWidth() <= 280);
                        assertEquals(76, step.getHeight(), 0.1);
                        assertEquals(controls.step(1).getWidth(), step.getWidth(), 1);
                        assertEquals(controls.step(1).getLayoutY(), step.getLayoutY(), 0.1);
                        assertTrue(step.getGraphic().getBoundsInParent().getHeight() <= 50);
                        Label number = (Label) step.lookup(".statement-step-number");
                        assertEquals(34, number.getWidth(), 0.1);
                        assertEquals(34, number.getHeight(), 0.1);
                    }
                    assertTrue(controls.section().lookup(".statement-flow")
                            .getLayoutBounds().getWidth() <= 860);
                    for (var connector : controls.section().lookupAll(".statement-connector")) {
                        assertEquals(20, connector.getLayoutBounds().getWidth(), 0.1);
                        var arrow = assertInstanceOf(javafx.scene.shape.SVGPath.class,
                                connector.lookup(".statement-connector-arrow"));
                        assertEquals(javafx.scene.paint.Color.web("#087e65"), arrow.getStroke());
                        assertEquals(javafx.scene.shape.StrokeLineCap.ROUND, arrow.getStrokeLineCap());
                        assertTrue(arrow.getBoundsInParent().getWidth() <= 20);
                    }
                }
                assertNotNull(controls.section().snapshot(null, null));
                assertEquals(2, controls.section().lookupAll(".statement-connector").size());
            } finally {
                stage.close();
            }
            return null;
        });
    }

    private Path createPdf(String name, String content, boolean encrypted) throws Exception {
        Path path = directory.resolve(name);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (!content.isEmpty()) {
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(40, 700);
                    stream.setLeading(16);
                    for (String line : content.split("\n")) {
                        stream.showText(line);
                        stream.newLine();
                    }
                    stream.endText();
                }
            }
            if (encrypted) {
                document.protect(new StandardProtectionPolicy("owner-test", "reader-test", new AccessPermission()));
            }
            document.save(path.toFile());
        }
        return path;
    }

    private static Controls controls(AtomicReference<File> selection) {
        StatementSection section = new StatementSection(owner -> selection.get(),
                (owner, current) -> null, path -> true);
        Scene scene = new Scene(section, 960, 680);
        scene.getStylesheets().add(Objects.requireNonNull(
                StatementSectionTest.class.getResource("/styles/app.css")).toExternalForm());
        section.applyCss();
        return new Controls(section, (Button) section.lookup(".statement-select"),
                (Button) section.lookup(".statement-extract"), (Button) section.lookup(".statement-filter"),
                (TextArea) section.lookup("#statement-raw-text"), (Label) section.lookup(".status-message"),
                (TextArea) section.lookup(".statement-filtered-text"));
    }

    private static void extract(Controls controls) throws Exception {
        onFx(() -> {
            controls.step(2).fire();
            return null;
        });
        runOperation(controls, controls.extract());
    }

    private static void runOperation(Controls controls, Button action) throws Exception {
        CompletableFuture<Void> finished = onFx(() -> {
            CompletableFuture<Void> result = new CompletableFuture<>();
            action.disabledProperty().addListener((observable, previous, disabled) -> {
                if (!disabled) {
                    result.complete(null);
                }
            });
            action.fire();
            assertTrue(controls.select().isDisabled());
            assertTrue(action.isDisabled());
            for (int i = 1; i <= 3; i++) {
                assertTrue(controls.step(i).isDisabled());
            }
            return result;
        });
        finished.get(15, TimeUnit.SECONDS);
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

    private static boolean isEffectivelyVisible(javafx.scene.Node node) {
        for (javafx.scene.Node current = node; current != null; current = current.getParent()) {
            if (!current.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static void assertSelected(Controls controls, int step) {
        for (int i = 1; i <= 3; i++) {
            assertEquals(i == step, controls.step(i).getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("selected")));
        }
        assertTrue(controls.step(step).isVisible());
    }

    private record Controls(StatementSection section, Button select, Button extract,
                            Button filter, TextArea text, Label status, TextArea filtered) {
        public Label status() {
            String[] ids = {"statement-selection-status", "statement-extraction-status", "statement-filter-status"};
            for (int i = 0; i < ids.length; i++) {
                if (step(i + 1).getPseudoClassStates().contains(PseudoClass.getPseudoClass("selected"))) {
                    return (Label) section.lookup("#" + ids[i]);
                }
            }
            throw new IllegalStateException("No hay ningún paso seleccionado");
        }

        Button step(int number) {
            return (Button) section.lookup("#statement-step-" + number);
        }
    }
}

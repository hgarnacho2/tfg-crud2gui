package com.crud2gui.desktop.teacher.sections;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
            assertFalse(settings.isVisible());
            assertFalse(settings.isManaged());
            assertFalse(controls.section().lookup(".statement-open-folder").isVisible());
            assertFalse(controls.section().lookup(".statement-open-folder").isManaged());
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
                    (TextArea) section.lookup("#statement-raw-text"), (Label) section.lookup(".statement-status"),
                    (TextArea) section.lookup(".statement-filtered-text"));
        });
        onFx(() -> {
            controls.select().fire();
            assertTrue(((Label) section.lookup(".statement-output-folder")).getText().contains(directory.toString()));
            assertFalse(section.lookup(".statement-output-settings").isVisible());
            assertFalse(section.lookup(".statement-output-settings").isManaged());
            controls.step(2).fire();
            assertTrue(section.lookup(".statement-output-settings").isVisible());
            assertTrue(section.lookup(".statement-output-settings").isManaged());
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
            assertTrue(section.lookup(".statement-output-settings").isVisible());
            controls.step(1).fire();
            assertFalse(section.lookup(".statement-output-settings").isVisible());
            assertFalse(section.lookup(".statement-output-settings").isManaged());
            assertFalse(section.lookup(".statement-open-folder").isVisible());
            controls.step(3).fire();
            assertTrue(section.lookup(".statement-output-settings").isVisible());
            VBox filterContent = (VBox) controls.filtered().getParent();
            assertSame(section.lookup(".statement-output-settings"), filterContent.getChildren().get(1));
            assertTrue(section.lookup(".statement-open-folder").isVisible());
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
                    ((Label) preview.lookup(".statement-preview-page")).getText());
            assertTrue(((Button) preview.lookup(".preview-previous")).isDisabled());
            assertFalse(((Button) preview.lookup(".preview-next")).isDisabled());
            var image = (javafx.scene.image.ImageView) preview.lookup(".statement-preview-image");
            assertEquals(900, image.getImage().getWidth(), 1);
            assertEquals(900, image.getFitWidth(), 0.1);
            assertFullPageHeight(preview);
            return null;
        });
        awaitPreview(preview, () -> ((Button) preview.lookup(".preview-next")).fire());
        onFx(() -> {
            assertEquals("Página 2 de 2 · 100 %",
                    ((Label) preview.lookup(".statement-preview-page")).getText());
            assertTrue(((Button) preview.lookup(".preview-next")).isDisabled());
            return null;
        });
        awaitPreview(preview, () -> ((Button) preview.lookup(".preview-zoom-in")).fire());
        onFx(() -> {
            assertEquals("Página 2 de 2 · 125 %",
                    ((Label) preview.lookup(".statement-preview-page")).getText());
            assertEquals(1125, ((javafx.scene.image.ImageView) preview.lookup(".statement-preview-image"))
                    .getFitWidth(), 0.1);
            assertFullPageHeight(preview);
            return null;
        });
        Path replacement = createPdf("preview-replacement.pdf", "Nuevo PDF.", false);
        awaitPreview(preview, () -> preview.load(replacement));
        onFx(() -> {
            assertEquals("Página 1 de 1 · 100 %",
                    ((Label) preview.lookup(".statement-preview-page")).getText());
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
            var image = (javafx.scene.image.ImageView) preview.lookup(".statement-preview-image");
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
        var viewport = (javafx.scene.control.ScrollPane) preview.lookup(".statement-preview-viewport");
        var image = (javafx.scene.image.ImageView) preview.lookup(".statement-preview-image");
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
            Label status = (Label) preview.lookup(".statement-preview-status");
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
            assertNull(((javafx.scene.image.ImageView) preview.lookup(".statement-preview-image")).getImage());
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
            assertEquals("", ((Label) preview.lookup(".statement-preview-status")).getText());
            assertEquals("Página 1 de 1 · 100 %",
                    ((Label) preview.lookup(".statement-preview-page")).getText());
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
                Label panelSubtitle = controls.section().lookupAll(".label").stream()
                        .filter(Label.class::isInstance).map(Label.class::cast)
                        .filter(label -> "El documento de partida".equals(label.getText()))
                        .findFirst().orElseThrow();
                Label explanation = controls.section().lookupAll(".label").stream()
                        .filter(Label.class::isInstance).map(Label.class::cast)
                        .filter(label -> label.getText().startsWith("Elige un PDF local."))
                        .findFirst().orElseThrow();
                assertEquals(14, panelSubtitle.getFont().getSize(), 0.01);
                assertEquals(explanation.getFont().getSize(), panelSubtitle.getFont().getSize());
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
                (TextArea) section.lookup("#statement-raw-text"), (Label) section.lookup(".statement-status"),
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

    private static void assertSelected(Controls controls, int step) {
        for (int i = 1; i <= 3; i++) {
            assertEquals(i == step, controls.step(i).getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("selected")));
        }
        assertTrue(controls.step(step).isVisible());
    }

    private record Controls(StatementSection section, Button select, Button extract,
                            Button filter, TextArea text, Label status, TextArea filtered) {
        Button step(int number) {
            return (Button) section.lookup("#statement-step-" + number);
        }
    }
}

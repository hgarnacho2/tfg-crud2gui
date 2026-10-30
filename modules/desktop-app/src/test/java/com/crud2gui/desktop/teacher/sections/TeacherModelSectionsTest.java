package com.crud2gui.desktop.teacher.sections;

import com.crud2gui.authenticator.AuthenticatedUser;
import com.crud2gui.desktop.teacher.TeacherDashboardView;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeacherModelSectionsTest {

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
    void selectsBothModelsAndKeepsValidationResultDisabled() throws Exception {
        Path source = Files.writeString(directory.resolve("source.json"), "{}");
        Path inferred = Files.writeString(directory.resolve("inferred.json"), "{}");
        AtomicReference<File> selection = new AtomicReference<>(source.toFile());
        onFx(() -> {
            ModelValidationSection section = new ModelValidationSection(owner -> selection.get());
            attach(section);
            Button first = button(section, "model-validation-step-java");
            Button second = button(section, "model-validation-step-statement");
            Button result = button(section, "model-validation-step-result");
                assertEquals("Modelo extra\u00eddo del c\u00f3digo",
                    ((Label) first.lookup(".statement-step-description")).getText());
                assertEquals("RESULTADO DE LA VALIDACI\u00d3N",
                    ((Label) result.lookup(".statement-step-title")).getText());
                assertEquals("JSON extra\u00eddo de los fuentes Java.",
                    ((Label) section.lookup("#model-validation-java").lookup(".panel-subtitle")).getText());
                assertEquals("Ning\u00fan archivo seleccionado.",
                    ((Label) section.lookup("#model-validation-java-filename")).getText());
            assertTrue(second.isDisabled());
            assertTrue(result.isDisabled());
            button(section, "model-validation-java-select").fire();
            assertFalse(second.isDisabled());
            assertTrue(first.getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
            second.fire();
            assertFalse(section.lookup("#model-validation-java").getParent().isVisible());
            assertTrue(section.lookup("#model-validation-statement").getParent().isVisible());
            selection.set(inferred.toFile());
            button(section, "model-validation-statement-select").fire();
            assertEquals(source, ((ModelJsonSelection) section.lookup("#model-validation-java")).getSelectedFile());
            assertEquals(inferred,
                    ((ModelJsonSelection) section.lookup("#model-validation-statement")).getSelectedFile());
            assertTrue(second.getPseudoClassStates().contains(PseudoClass.getPseudoClass("completed")));
            assertTrue(result.isDisabled());
            first.fire();
            assertTrue(section.lookup("#model-validation-java").getParent().isVisible());
            assertEquals(source.toString(), ((Label) section.lookup("#model-validation-java-path")).getText());
            return null;
        });
    }

    @Test
    void crudSelectionSurvivesCancellationAndInvalidFiles() throws Exception {
        Path model = Files.writeString(directory.resolve("validated.JSON"), "{}");
        Path invalid = Files.writeString(directory.resolve("model.txt"), "{}");
        AtomicReference<File> selection = new AtomicReference<>();
        onFx(() -> {
            CrudSection section = new CrudSection(owner -> selection.get());
            attach(section);
            ModelJsonSelection selector = (ModelJsonSelection) section.lookup("#crud-model");
            Button select = button(section, "crud-model-select");
                assertEquals("JSON validado extra\u00eddo de los fuentes Java.",
                    ((Label) selector.lookup(".panel-subtitle")).getText());
                assertEquals("Ning\u00fan archivo seleccionado.",
                    ((Label) section.lookup("#crud-model-filename")).getText());
            select.fire();
            assertNull(selector.getSelectedFile());
            selection.set(invalid.toFile());
            select.fire();
            assertNull(selector.getSelectedFile());
            Label status = (Label) section.lookup("#crud-model-status");
            assertTrue(status.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            selection.set(model.toFile());
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            assertEquals("validated.JSON", ((Label) section.lookup("#crud-model-filename")).getText());
            assertEquals("", status.getText());
            assertFalse(status.getPseudoClassStates().contains(PseudoClass.getPseudoClass("error")));
            assertTrue(button(section, "crud-step-model").getPseudoClassStates()
                    .contains(PseudoClass.getPseudoClass("completed")));
            selection.set(null);
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            selection.set(directory.resolve("missing.json").toFile());
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            selection.set(directory.toFile());
            select.fire();
            assertEquals(model, selector.getSelectedFile());
            return null;
        });
    }

    @Test
    void teacherMenuOpensAndCachesBothSections() throws Exception {
        onFx(() -> {
            AuthenticatedUser user = new AuthenticatedUser("teacher", "teacher", "Docente", null,
                    Set.of("docente"), null);
            TeacherDashboardView dashboard = new TeacherDashboardView(user, () -> { });
            attach(dashboard);
            Button validation = navigation(dashboard, "Validar modelos");
            Button crud = navigation(dashboard, "CRUD");
            validation.fire();
            ScrollPane content = (ScrollPane) dashboard.getCenter();
            Node validationPage = content.getContent();
            assertTrue(validationPage instanceof ModelValidationSection);
            crud.fire();
            Node crudPage = content.getContent();
            assertTrue(crudPage instanceof CrudSection);
            assertNotNull(crudPage.lookup("#crud-model-select"));
            validation.fire();
            assertSame(validationPage, content.getContent());
            crud.fire();
            assertSame(crudPage, content.getContent());
            return null;
        });
    }

    private static void attach(Node section) {
        StackPane root = new StackPane(section);
        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(TeacherModelSectionsTest.class.getResource("/styles/app.css").toExternalForm());
        root.applyCss();
        root.layout();
    }

    private static Button button(Node root, String id) {
        return (Button) root.lookup("#" + id);
    }

    private static Button navigation(Node root, String text) {
        return root.lookupAll(".sidebar-navigation-button").stream()
                .map(Button.class::cast).filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.call());
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
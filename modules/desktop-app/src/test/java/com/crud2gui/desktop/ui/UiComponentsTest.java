package com.crud2gui.desktop.ui;

import com.crud2gui.desktop.login.LoginView;
import com.crud2gui.desktop.login.LogoutView;
import com.crud2gui.desktop.student.StudentDashboardView;
import com.crud2gui.desktop.teacher.TeacherDashboardView;
import com.crud2gui.authenticator.AuthenticatedUser;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.InvalidPathException;
import java.util.Objects;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UiComponentsTest {

    @Test
    void fileContentPaneResolvesEachOpeningAndRecoversWithoutStatementStyles(@TempDir Path directory)
            throws Exception {
        Path first = directory.resolve("first.txt");
        Path second = directory.resolve("second.txt");
        Files.writeString(first, "Texto UTF-8: información");
        Files.writeString(second, "Segundo archivo");
        AtomicInteger mode = new AtomicInteger();
        AtomicInteger resolutions = new AtomicInteger();
        onJavaFxThread(() -> {
            TitledPane pane = UiComponents.fileContentPane("file-viewer", "Consultar archivo", () -> {
                resolutions.incrementAndGet();
                return switch (mode.get()) {
                    case 1 -> second;
                    case 2 -> directory.resolve("missing.txt");
                    case 4 -> throw new InvalidPathException("invalid", "Ruta de prueba");
                    default -> first;
                };
            }, "No se pudo consultar el archivo.");
            styledScene(new VBox(pane));
            pane.applyCss();
            assertFalse(pane.isExpanded());
            assertEquals(0, resolutions.get());
            TextArea content = (TextArea) pane.lookup("#file-viewer-content");
            Label source = (Label) pane.lookup("#file-viewer-path");
            Label error = (Label) pane.lookup("#file-viewer-error");
            assertFalse(content.isEditable());
            assertFalse(content.isWrapText());
            assertTrue(pane.getStyleClass().contains("file-content"));
            assertTrue(content.getStyleClass().contains("file-content-text"));
            assertTrue(source.getStyleClass().contains("file-content-path"));
            assertTrue(error.getStyleClass().contains("status-message"));
            assertTrue(pane.lookupAll("*").stream().flatMap(node -> node.getStyleClass().stream())
                    .noneMatch(style -> style.startsWith("statement-")));
            for (int scenario = 0; scenario < 6; scenario++) {
                mode.set(scenario);
                pane.setExpanded(true);
                assertEquals(scenario + 1, resolutions.get());
                boolean failed = scenario == 2 || scenario == 4;
                assertEquals(!failed, content.isVisible());
                assertEquals(!failed, content.isManaged());
                assertEquals(failed, error.isManaged());
                if (failed) {
                    assertEquals("", content.getText());
                    assertEquals("", source.getText());
                    assertEquals("No se pudo consultar el archivo.", error.getText());
                    assertTrue(error.getPseudoClassStates().contains(Section.ERROR));
                } else {
                    assertEquals(scenario == 1 ? "Segundo archivo" : "Texto UTF-8: información",
                            content.getText());
                    assertEquals((scenario == 1 ? second : first).toString(), source.getText());
                    assertEquals(0, content.getCaretPosition());
                    assertEquals("", error.getText());
                }
                pane.setExpanded(false);
                assertEquals(scenario + 1, resolutions.get());
            }
        });
    }

    @Test
    void createsSectionHeadersWithAndWithoutActionButton() throws Exception {
        onJavaFxThread(() -> {
            VBox plain = UiComponents.sectionHeader("Resumen", "Mi espacio", "Descripcion");
            AtomicInteger actions = new AtomicInteger();
            Button button = UiComponents.actionButton(
                    "Nueva práctica", List.of("section-header-action"), actions::incrementAndGet);
            var withAction = UiComponents.sectionHeader("DOCENCIA", "Practicas", "Descripcion", button);
            VBox root = new VBox(plain, withAction);
            styledScene(root);
            root.applyCss();
            assertEquals(3, plain.getChildren().size());
            assertNull(plain.lookup(".button"));
            assertEquals("CRUD2GUI  /  Resumen", ((Label) plain.getChildren().get(0)).getText());
            assertSame(button, withAction.getChildren().get(2));
            assertInstanceOf(VBox.class, withAction.getChildren().get(0));
            assertEquals(Pos.CENTER_LEFT, withAction.getAlignment());
            assertEquals(20, withAction.getSpacing());
            assertEquals(new Insets(10, 16, 10, 16), button.getPadding());
            assertEquals(0, actions.get());
            button.fire();
            assertEquals(1, actions.get());
            assertThrows(NullPointerException.class,
                    () -> UiComponents.sectionHeader("Resumen", "Titulo", "Descripcion", null));
        });
    }

    @Test
    void updatesBreadcrumbWhenStudentSectionChanges() throws Exception {
        onJavaFxThread(() -> {
            var user = new AuthenticatedUser("student", "alumno@example.test", "Alumno",
                    "tenant", java.util.Set.of("alumno"), null);
            var dashboard = new StudentDashboardView(user, () -> {});
            styledScene(new VBox(dashboard));
            dashboard.applyCss();
            var sidebar = (VBox) ((javafx.scene.control.ScrollPane) dashboard.getLeft()).getContent();
            for (var child : sidebar.getChildren()) {
                if (child instanceof Button button) {
                    button.fire();
                    assertEquals("CRUD2GUI  /  " + button.getText(),
                            ((Label) dashboard.lookup(".section-breadcrumb")).getText());
                }
            }
        });
    }

    @Test
    void cachesEveryDashboardPageAndKeepsSidebarSelectionInSync() throws Exception {
        onJavaFxThread(() -> {
            var user = new AuthenticatedUser("user", "user@example.test", "Usuario",
                    "tenant", java.util.Set.of("alumno", "docente"), null);
            for (BorderPane dashboard : List.of(
                    new StudentDashboardView(user, () -> {}),
                    new TeacherDashboardView(user, () -> {}))) {
                styledScene(new VBox(dashboard));
                dashboard.applyCss();
                VBox sidebar = (VBox) ((ScrollPane) dashboard.getLeft()).getContent();
                assertNotNull(sidebar.lookup(".sidebar-uned-logo"));
                assertNotNull(sidebar.lookup(".sidebar-branding"));
                assertEquals(new Insets(26, 18, 20, 18), sidebar.getPadding());
                assertEquals(10, sidebar.getSpacing());
                assertTrue(sidebar.getStyle().isEmpty());
                var buttons = sidebar.getChildren().stream().filter(Button.class::isInstance)
                        .map(Button.class::cast).toList();
                for (Button button : buttons) {
                    assertEquals(15, button.getFont().getSize(), 0.01);
                    assertTrue(button.getFont().getStyle().contains("Bold"));
                }
                assertEquals(dashboard instanceof TeacherDashboardView ? 6 : 4, buttons.size());
                if (dashboard instanceof TeacherDashboardView) {
                    assertEquals("Enunciado", buttons.get(buttons.size() - 1).getText());
                }
                ScrollPane content = (ScrollPane) dashboard.getCenter();
                java.util.Map<Button, Node> visited = new java.util.LinkedHashMap<>();
                for (Button button : buttons) {
                    button.fire();
                    dashboard.applyCss();
                    Node page = content.getContent();
                    assertNotNull(page.lookup(".section-breadcrumb"));
                    assertInstanceOf(Section.class, page);
                    assertEquals(20, ((Section) page).getSpacing(), 0.01);
                    assertNotNull(((Section) page).getChildren().get(0).lookup(".section-title"));
                    assertEquals(new Insets(32, 38, 36, 38), ((VBox) page).getPadding());
                    assertTrue(button.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("selected")));
                    assertEquals(Color.web("#1b5b50"), button.getBackground().getFills().get(0).getFill());
                    assertEquals(dashboard instanceof TeacherDashboardView
                                    && !"Enunciado".equals(button.getText())
                                    && !"Modelo desde enunciado".equals(button.getText()) ? 1 : 0,
                            page.lookupAll(".section-header-action").size());
                    visited.put(button, page);
                }
                for (var entry : visited.entrySet()) {
                    entry.getKey().fire();
                    assertSame(entry.getValue(), content.getContent());
                }
                buttons.get(0).fire();
                if (dashboard instanceof StudentDashboardView) {
                    Button shortcut = content.getContent().lookupAll(".button").stream()
                            .filter(node -> node instanceof Button button
                                    && "Abrir mis prácticas".equals(button.getText()))
                            .map(Button.class::cast).findFirst().orElseThrow();
                    shortcut.fire();
                    assertSame(visited.get(buttons.get(1)), content.getContent());
                    assertTrue(buttons.get(1).getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("selected")));
                    assertFalse(buttons.get(0).getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("selected")));
                }
            }
        });
    }

    @Test
    void createsSectionsWithCommonLayoutAndOptionalHeaderAction() throws Exception {
        onJavaFxThread(() -> {
            AtomicInteger clicks = new AtomicInteger();
            Button action = UiComponents.actionButton("Acción", List.of(), clicks::incrementAndGet);
            for (Section section : List.of(
                    new Section("PRUEBA", "Título", "Descripción") {},
                    new Section("PRUEBA", "Título", "Descripción", action) {})) {
                Label content = new Label("Contenido");
                assertEquals(section.getClass().getName(), section.logger.getName());
                section.getChildren().add(content);
                styledScene(section);
                section.applyCss();
                assertEquals(20, section.getSpacing(), 0.01);
                assertEquals(new Insets(32, 38, 36, 38), section.getPadding());
                assertEquals(2, section.getChildren().size());
                assertSame(content, section.getChildren().get(1));
                assertEquals("Título", ((Label) section.lookup(".section-title")).getText());
                assertEquals("Descripción", ((Label) section.lookup(".section-subtitle")).getText());
                assertEquals("CRUD2GUI  /  PRUEBA",
                        ((Label) section.lookup(".section-breadcrumb")).getText());
            }
            action.fire();
            assertEquals(1, clicks.get());
        });
    }

    @Test
    void sectionTasksDeliverResultsAndAlwaysFinishOnJavaFxThread() throws Exception {
        for (int scenario = 0; scenario < 3; scenario++) {
            int mode = scenario;
            CompletableFuture<Void> finished = new CompletableFuture<>();
            AtomicInteger successes = new AtomicInteger();
            AtomicInteger failures = new AtomicInteger();
            AtomicInteger cleanups = new AtomicInteger();
            onJavaFxThread(() -> {
                Section section = new Section("PRUEBA", "Tareas", "Ejecución") {};
                section.runTask(() -> {
                    assertFalse(Platform.isFxApplicationThread());
                    if (mode == 1) {
                        throw new java.io.IOException("Fallo de prueba");
                    }
                    return "Resultado";
                }, result -> {
                    assertTrue(Platform.isFxApplicationThread());
                    assertEquals("Resultado", result);
                    successes.incrementAndGet();
                    if (mode == 2) {
                        throw new IllegalStateException("Fallo al presentar");
                    }
                }, failure -> {
                    assertTrue(Platform.isFxApplicationThread());
                    if (mode == 1) {
                        assertInstanceOf(java.io.IOException.class, failure);
                    } else {
                        assertInstanceOf(IllegalStateException.class, failure);
                    }
                    failures.incrementAndGet();
                }, () -> {
                    cleanups.incrementAndGet();
                    if (Platform.isFxApplicationThread()) {
                        finished.complete(null);
                    } else {
                        finished.completeExceptionally(new AssertionError("Limpieza fuera de JavaFX"));
                    }
                });
            });
            finished.get(10, TimeUnit.SECONDS);
            assertEquals(mode == 1 ? 0 : 1, successes.get());
            assertEquals(mode == 0 ? 0 : 1, failures.get());
            assertEquals(1, cleanups.get());
        }
    }

    @Test
    void sectionStatusUpdatesTextAndClearsErrorState() throws Exception {
        onJavaFxThread(() -> {
            Section section = new Section("PRUEBA", "Estado", "Mensajes") {};
            Label status = UiComponents.statusLabel("test-status");
            section.showStatus(status, "Error de prueba", true);
            assertEquals("Error de prueba", status.getText());
            assertTrue(status.getPseudoClassStates().contains(Section.ERROR));
            section.showStatus(status, "Correcto", false);
            assertFalse(status.getPseudoClassStates().contains(Section.ERROR));
            assertEquals("Correcto", status.getText());
            section.showStatus(status, "", false);
            assertFalse(status.isManaged());
        });
    }

    @Test
    void buildsSidebarLinksAndNotifiesSelectionWithoutCreatingPages() throws Exception {
        onJavaFxThread(() -> {
            var selected = new javafx.beans.property.SimpleObjectProperty<>("Resumen");
            ScrollPane scroll = UiComponents.dashboardSidebar("Panel de prueba", new VBox(),
                    List.of("Resumen", "Practicas"), name -> name, selected);
            VBox sidebar = (VBox) scroll.getContent();
            Button overview = (Button) sidebar.getChildren().get(1);
            Button practices = (Button) sidebar.getChildren().get(2);
            var active = javafx.css.PseudoClass.getPseudoClass("selected");
            assertEquals("Practicas", practices.getText());
            assertTrue(overview.getPseudoClassStates().contains(active));
            practices.fire();
            assertEquals("Practicas", selected.get());
            assertTrue(practices.getPseudoClassStates().contains(active));
            selected.set("Resumen");
            assertTrue(overview.getPseudoClassStates().contains(active));
            assertFalse(practices.getPseudoClassStates().contains(active));
        });
    }

    @Test
    void rendersBrowserAgainAfterRepeatedLogoutCycles() throws Exception {
        Stage[] window = new Stage[1];
        onJavaFxThread(() -> {
            Platform.setImplicitExit(false);
            window[0] = new Stage();
            window[0].setScene(styledScene(new LoginView()));
            window[0].show();
        });
        try {
            for (int cycle = 0; cycle < 3; cycle++) {
                LoginView[] login = new LoginView[1];
                CompletableFuture<Void> pageLoaded = new CompletableFuture<>();
                onJavaFxThread(() -> {
                    login[0] = new LoginView();
                    window[0].getScene().setRoot(login[0]);
                    login[0].showEmbeddedBrowser(Objects.requireNonNull(
                            UiComponentsTest.class.getResource("/templates/auth_success_page.html")));
                });
                onJavaFxThread(() -> {
                    WebView browser = assertInstanceOf(WebView.class, login[0].getChildren().get(0));
                    var worker = browser.getEngine().getLoadWorker();
                    worker.stateProperty().addListener((observable, previous, state) -> {
                        if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                            pageLoaded.complete(null);
                        } else if (state == javafx.concurrent.Worker.State.FAILED) {
                            pageLoaded.completeExceptionally(worker.getException());
                        }
                    });
                    if (worker.getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
                        pageLoaded.complete(null);
                    } else if (worker.getState() == javafx.concurrent.Worker.State.FAILED) {
                        pageLoaded.completeExceptionally(worker.getException());
                    }
                });
                pageLoaded.get(10, TimeUnit.SECONDS);
                onJavaFxThread(() -> {
                    assertTrue(window[0].getScene().snapshot(null).getWidth() > 0);
                });
                CompletableFuture<Void> logout = new CompletableFuture<>();
                onJavaFxThread(() -> {
                    var view = new LogoutView(URI.create("about:blank"), URI.create("http://localhost"), logout);
                    window[0].getScene().setRoot(view);
                    ((WebView) view.getChildren().get(1)).getEngine().load("http://localhost/");
                });
                logout.get(10, TimeUnit.SECONDS);
                onJavaFxThread(() -> {
                    window[0].getScene().setRoot(new LoginView());
                    assertTrue(window[0].getScene().snapshot(null).getWidth() > 0);
                });
            }
        } finally {
            onJavaFxThread(() -> window[0].close());
        }
    }

    @Test
    void wiresTeacherSignOutAction() throws Exception {
        onJavaFxThread(() -> {
            var user = new AuthenticatedUser("teacher", "docente@example.test", "Docente UNED",
                    "test-tenant", java.util.Set.of("docente"), null);
            AtomicInteger signOuts = new AtomicInteger();
            var dashboard = new TeacherDashboardView(user, signOuts::incrementAndGet);
            styledScene(new VBox(dashboard));
            dashboard.applyCss();
            var logo = (javafx.scene.image.ImageView) dashboard.lookup(".sidebar-uned-logo");
            assertNotNull(logo);
            assertFalse(logo.getImage().isError());
            assertEquals(36, logo.getFitWidth());
            assertEquals(36, logo.getFitHeight());
            assertTrue(logo.isPreserveRatio());
            var header = (javafx.scene.layout.HBox) dashboard.lookup(".sidebar-branding");
            var frame = (javafx.scene.layout.StackPane) header.getChildren().get(0);
            assertSame(logo, frame.getChildren().get(0));
            assertEquals(44, frame.getPrefWidth());
            assertEquals(44, frame.getPrefHeight());
            assertEquals(Color.web("#6b9e8f"), frame.getBorder().getStrokes().get(0).getTopStroke());
            var accountCard = (VBox) dashboard.lookup(".current-user-card");
            assertEquals(accountCard.getBackground(), header.getBackground());
            assertEquals(accountCard.getBorder(), header.getBorder());
            assertEquals(accountCard.getPadding(), header.getPadding());
            ((Button) dashboard.lookup(".sign-out-button")).fire();
            assertEquals(1, signOuts.get());
        });
    }

    @Test
    void showsMicrosoftLogoutWithoutReturnButton() throws Exception {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        onJavaFxThread(() -> {
            var logout = new LogoutView(URI.create("about:blank"), URI.create("http://localhost"), completion);
            styledScene(logout);
            assertFalse(completion.isDone());
            assertNull(logout.lookup(".button"));
            assertEquals(2, logout.getChildren().size());
            assertInstanceOf(WebView.class, logout.getChildren().get(1));
            ((WebView) logout.getChildren().get(1)).getEngine().load("http://localhost/");
        });
        completion.get(10, TimeUnit.SECONDS);
    }

    @Test
    void completesMicrosoftLogoutOnlyOnRegisteredReturn() throws Exception {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        onJavaFxThread(() -> {
            var logout = new LogoutView(URI.create("about:blank"), URI.create("http://localhost"), completion);
            styledScene(logout);
            assertFalse(completion.isDone());
            ((javafx.scene.web.WebView) logout.getChildren().get(1))
                    .getEngine().load("http://localhost/");
        });
        completion.get(10, TimeUnit.SECONDS);
    }

    @Test
    void showsCurrentUserCardWithoutRedundantHeading() throws Exception {
        onJavaFxThread(() -> {
            var user = new AuthenticatedUser("test-user", "alumno@example.test", "Alumno UNED",
                    "test-tenant", java.util.Set.of("alumno"), null);
            AtomicInteger signOuts = new AtomicInteger();
            var dashboard = new StudentDashboardView(user, signOuts::incrementAndGet);
            VBox root = new VBox(dashboard);
            styledScene(root);
            root.applyCss();
            assertNull(dashboard.lookup(".current-user-heading"));
            var icon = dashboard.lookup(".current-user-icon");
            assertInstanceOf(javafx.scene.shape.SVGPath.class, icon);
            assertEquals(Color.web("#d8eee4"), ((javafx.scene.shape.SVGPath) icon).getFill());
            Label name = (Label) dashboard.lookup(".current-user-name");
            assertEquals("Alumno UNED", name.getText());
            assertTrue(name.isWrapText());
            assertEquals("Perfil alumno", ((Label) dashboard.lookup(".current-user-card")
                    .lookup(".current-user-role")).getText());
            assertNotNull(dashboard.lookup(".current-user-card"));
            Button signOut = (Button) dashboard.lookup(".sign-out-button");
            assertEquals("Cerrar sesión", signOut.getText());
            signOut.fire();
            assertEquals(1, signOuts.get());
            assertEquals(36, ((javafx.scene.layout.StackPane)
                    dashboard.lookup(".current-user-avatar")).getMaxWidth());
        });
    }

    @Test
    void keepsSharedAccountCardAccessibleInShortAndTallWindows() throws Exception {
        onJavaFxThread(() -> {
            var user = new AuthenticatedUser("user", "cuenta@example.test", "Nombre de usuario",
                    "tenant", java.util.Set.of("alumno", "docente"), null);
            for (var dashboard : List.of(
                    new StudentDashboardView(user, () -> {}),
                    new TeacherDashboardView(user, () -> {}))) {
                Scene scene = new Scene(dashboard, 760, 640);
                scene.getStylesheets().add(Objects.requireNonNull(
                        getClass().getResource("/styles/app.css")).toExternalForm());
                dashboard.applyCss();
                for (int height : new int[] {760, 640, 300}) {
                    dashboard.resize(760, height);
                    dashboard.layout();
                    var scroll = (javafx.scene.control.ScrollPane) dashboard.getLeft();
                    VBox sidebar = (VBox) scroll.getContent();
                    VBox card = (VBox) sidebar.lookup(".current-user-card");
                    assertNotNull(card);
                    assertNull(sidebar.lookup(".current-user-heading"));
                    Label role = (Label) card.lookup(".current-user-role");
                    assertEquals(dashboard instanceof StudentDashboardView ? "Perfil alumno" : "Perfil docente",
                            role.getText());
                    assertEquals(Color.WHITE, role.getTextFill());
                    assertEquals(13, role.getFont().getSize());
                    Button signOut = (Button) card.lookup(".sign-out-button");
                    assertEquals(Color.WHITE, signOut.getTextFill());
                    assertTrue(contrast(Color.WHITE, Color.web("#255c4e")) >= 4.5);
                    assertTrue(contrast(Color.WHITE, Color.web("#19483f")) >= 4.5);
                    assertTrue(contrast(Color.WHITE, Color.web("#2c6557")) >= 4.5);
                    for (int index = 1; index < sidebar.getChildren().size(); index++) {
                        var previous = sidebar.getChildren().get(index - 1).getBoundsInParent();
                        var current = sidebar.getChildren().get(index).getBoundsInParent();
                        assertTrue(previous.getMaxY() <= current.getMinY() + 1);
                    }
                    if (height == 300) {
                        assertTrue(sidebar.getHeight() > scroll.getViewportBounds().getHeight());
                    } else {
                        assertEquals(scroll.getViewportBounds().getHeight(), sidebar.getHeight(), 1);
                    }
                    scroll.setVvalue(1);
                    dashboard.layout();
                    var viewport = scroll.lookup(".viewport");
                    var visible = viewport.localToScene(viewport.getBoundsInLocal());
                    var accountBounds = card.localToScene(card.getBoundsInLocal());
                    assertTrue(accountBounds.getMaxY() <= visible.getMaxY() + 1);
                    assertTrue(accountBounds.getMinY() >= visible.getMinY() - 1);
                }
            }
        });
    }

    private static double contrast(Color foreground, Color background) {
        double first = luminance(foreground);
        double second = luminance(background);
        return (Math.max(first, second) + 0.05) / (Math.min(first, second) + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen())
                + 0.0722 * linear(color.getBlue());
    }

    private static double linear(double component) {
        return component <= 0.04045 ? component / 12.92 : Math.pow((component + 0.055) / 1.055, 2.4);
    }

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
    void appliesLoginViewAndEmbeddedBrowserStyles() throws Exception {
        onJavaFxThread(() -> {
            LoginView login = new LoginView();
            styledScene(login);
            login.applyCss();
            assertEquals(Pos.CENTER, login.getAlignment());
            assertEquals(new Insets(28), login.getPadding());
            assertEquals(Color.web("#f1f6f4"), login.getBackground().getFills().get(0).getFill());
            login.getStyleClass().add("embedded-browser");
            login.applyCss();
            assertEquals(Insets.EMPTY, login.getPadding());
            login.getStyleClass().remove("embedded-browser");
            login.applyCss();
            assertEquals(new Insets(28), login.getPadding());
        });
    }

    @Test
    void createsVerticalContainerWithCssLayout() throws Exception {
        onJavaFxThread(() -> {
            Label first = UiComponents.label("Primero", List.of());
            Label second = UiComponents.label("Segundo", List.of());
            VBox container = UiComponents.vBox(List.of("login-container"), first, second);
            assertEquals(List.of(first, second), container.getChildren());
            VBox root = new VBox(container);
            styledScene(root);
            root.applyCss();
            assertEquals(18, container.getSpacing());
            assertEquals(Pos.TOP_LEFT, container.getAlignment());
            assertEquals(new Insets(34), container.getPadding());
            assertEquals(430, container.getMaxWidth());
            assertEquals(Color.WHITE, container.getBackground().getFills().get(0).getFill());
            assertNotNull(container.getEffect());
            assertTrue(UiComponents.vBox(List.of()).getChildren().isEmpty());
        });
    }

    @Test
    void createsHorizontalRowWithChildrenAndCssLayout() throws Exception {
        onJavaFxThread(() -> {
            Label first = UiComponents.label("Primero", List.of());
            Label second = UiComponents.label("Segundo", List.of());
            var row = UiComponents.hBox(List.of("login-status-row"), first, second);
            assertEquals(List.of(first, second), row.getChildren());
            VBox root = new VBox(row);
            styledScene(root);
            root.applyCss();
            assertEquals(10, row.getSpacing());
            assertEquals(Pos.CENTER_LEFT, row.getAlignment());
            assertTrue(UiComponents.hBox(List.of()).getChildren().isEmpty());
        });
    }

    @Test
    void createsParameterizedProgressIndicators() throws Exception {
        onJavaFxThread(() -> {
            var hidden = UiComponents.progressIndicator(false, List.of("login-progress"));
            assertFalse(hidden.isVisible());
            assertFalse(hidden.isManaged());
            assertTrue(hidden.isIndeterminate());
            assertTrue(hidden.getStyleClass().containsAll(List.of("progress-indicator", "login-progress")));

            var visible = UiComponents.progressIndicator(true, List.of("login-progress"));
            VBox root = new VBox(visible);
            styledScene(root);
            root.applyCss();
            assertEquals(22, visible.getMaxWidth());
            assertEquals(22, visible.getMaxHeight());
            assertTrue(visible.isVisible());
            assertTrue(visible.isManaged());
        });
    }

    @Test
    void hidesEmptyLabelsOnlyWhenRequested() throws Exception {
        onJavaFxThread(() -> {
            Label label = UiComponents.label("", List.of("custom-label"), true);
            assertTrue(label.getStyleClass().contains("custom-label"));
            assertTrue(label.visibleProperty().isBound());
            assertTrue(label.managedProperty().isBound());
            assertFalse(label.isVisible());
            assertFalse(label.isManaged());
            label.setText("Resultado");
            assertTrue(label.isVisible());
            assertTrue(label.isManaged());
            label.setText("");
            assertFalse(label.isVisible());
            assertFalse(label.isManaged());
            label.setText(null);
            assertFalse(label.isVisible());
            assertFalse(label.isManaged());

            Label populated = UiComponents.label("Inicial", List.of(), true);
            assertTrue(populated.isVisible());
            assertTrue(populated.isManaged());
            for (Label plain : List.of(
                    UiComponents.label("", List.of()),
                    UiComponents.label("", List.of(), false))) {
                assertTrue(plain.isVisible());
                assertTrue(plain.isManaged());
                assertFalse(plain.visibleProperty().isBound());
                assertFalse(plain.managedProperty().isBound());
                plain.setVisible(false);
                assertTrue(plain.isManaged());
            }
        });
    }

    @Test
    void createsLabelsWithTextAndAdditionalClasses() throws Exception {
        onJavaFxThread(() -> {
            Label label = UiComponents.label("Acceso", List.of("login-heading", "custom-label"));
            assertEquals("Acceso", label.getText());
            assertTrue(label.getStyleClass().containsAll(
                    List.of("label", "login-heading", "custom-label")));
            Label plain = UiComponents.label("Sin estilo", List.of());
            assertEquals("Sin estilo", plain.getText());
            assertEquals(List.of("label"), plain.getStyleClass());
        });
    }

    @Test
    void addsExtraClassesWithoutReplacingDefaults() throws Exception {
        onJavaFxThread(() -> {
            AtomicInteger clicks = new AtomicInteger();
            Button button = UiComponents.actionButton(
                    "Abrir", List.of("login-button", "custom-button"), clicks::incrementAndGet);
            assertTrue(button.getStyleClass().containsAll(
                    List.of("button", "action-button", "login-button", "custom-button")));
            button.fire();
            assertEquals(1, clicks.get());
        });
    }

    @Test
    void appliesStylesAndKeepsButtonAction() throws Exception {
        onJavaFxThread(() -> {
            var text = UiComponents.mutedText("Descripcion");
            AtomicInteger clicks = new AtomicInteger();
            Button button = UiComponents.actionButton("Abrir", List.of(), clicks::incrementAndGet);
            VBox root = new VBox(text, button);
            styledScene(root);
            root.applyCss();

            assertTrue(text.isWrapText());
            assertEquals(Color.web("#64736f"), text.getTextFill());
            assertEquals(14, text.getFont().getSize(), 0.01);
            assertEquals(Color.WHITE, button.getTextFill());
            assertEquals(Color.web("#087e65"), button.getBackground().getFills().get(0).getFill());
            assertEquals(15, button.getFont().getSize(), 0.01);
            assertEquals(new Insets(10, 14, 10, 14), button.getPadding());
            assertEquals(Cursor.HAND, button.getCursor());
            button.fire();
            assertEquals(1, clicks.get());

            button.setDisable(true);
            root.applyCss();
            assertEquals(Cursor.DEFAULT, button.getCursor());
            button.fire();
            assertEquals(1, clicks.get());

            button.setDisable(false);
            root.setDisable(true);
            root.applyCss();
            assertEquals(Cursor.DEFAULT, button.getCursor());
            root.setDisable(false);
            root.applyCss();
            assertEquals(Cursor.HAND, button.getCursor());
        });
    }

    @Test
    void providesSecondaryExitButtonOnLogin() throws Exception {
        onJavaFxThread(() -> {
            LoginView login = new LoginView();
            styledScene(login);
            login.applyCss();
            Button exit = (Button) login.lookup(".exit-button");
            assertNotNull(exit);
            assertEquals("Salir", exit.getText());
            assertNotNull(exit.getOnAction());
            assertFalse(exit.isDefaultButton());
            assertFalse(exit.isDisabled());
            assertEquals(Double.MAX_VALUE, exit.getMaxWidth());
            assertEquals(Color.web("#087e65"), exit.getTextFill());
            assertEquals(Cursor.HAND, exit.getCursor());
        });
    }

    @Test
    void keepsLoginVariantAndStylesAfterChangingRoot() throws Exception {
        onJavaFxThread(() -> {
            VBox root = new VBox();
            Scene scene = styledScene(root);
            LoginView login = new LoginView();
            scene.setRoot(login);
            login.applyCss();
            Button button = (Button) login.lookup(".login-button");
            Label brand = (Label) login.lookup(".login-brand");
            Label heading = (Label) login.lookup(".login-heading");
            Label description = (Label) login.lookup(".login-description");
            Label status = (Label) login.lookup(".login-status");
            Label role = (Label) login.lookup(".login-role");
            assertNotNull(role);
            assertEquals(Color.web("#087e65"), role.getTextFill());
            assertEquals(16, role.getFont().getSize(), 0.01);
            assertTrue(role.getFont().getStyle().contains("Bold"));
            assertTrue(role.isWrapText());
            assertNotNull(status);
            assertEquals("No has iniciado sesión.", status.getText());
            assertEquals(Color.web("#17212b"), status.getTextFill());
            assertEquals(14, status.getFont().getSize(), 0.01);
            assertTrue(status.isWrapText());
            assertNotNull(brand);
            assertNotNull(heading);
            assertNotNull(description);
            assertEquals(14, brand.getFont().getSize(), 0.01);
            assertEquals(Color.web("#087e65"), brand.getTextFill());
            assertEquals(28, heading.getFont().getSize(), 0.01);
            assertEquals(Color.web("#17212b"), heading.getTextFill());
            assertEquals(14, description.getFont().getSize(), 0.01);
            assertEquals(Color.web("#52616b"), description.getTextFill());
            assertTrue(description.isWrapText());
            assertNotNull(button);
            assertEquals(new Insets(12, 16, 12, 16), button.getPadding());
            assertTrue(button.isDefaultButton());
            assertEquals(Double.MAX_VALUE, button.getMaxWidth());
            assertEquals(Cursor.HAND, button.getCursor());
        });
    }

    private static Scene styledScene(VBox root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(
                UiComponentsTest.class.getResource("/styles/app.css")).toExternalForm());
        return scene;
    }

    private static void onJavaFxThread(Runnable assertions) throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                assertions.run();
                result.complete(null);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        result.get(10, TimeUnit.SECONDS);
    }
}

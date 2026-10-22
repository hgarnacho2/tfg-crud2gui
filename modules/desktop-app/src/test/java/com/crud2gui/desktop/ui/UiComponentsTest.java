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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UiComponentsTest {

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
            assertEquals("Perfil alumno", ((Label) dashboard.lookup(".current-user-role")).getText());
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
                    assertEquals(12, role.getFont().getSize());
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
        Platform.startup(() -> started.complete(null));
        started.get(10, TimeUnit.SECONDS);
    }

    @AfterAll
    static void stopJavaFx() {
        Platform.exit();
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
            assertEquals(12, text.getFont().getSize(), 0.01);
            assertEquals(Color.WHITE, button.getTextFill());
            assertEquals(Color.web("#087e65"), button.getBackground().getFills().get(0).getFill());
            assertEquals(14, button.getFont().getSize(), 0.01);
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

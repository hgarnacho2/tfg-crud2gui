package com.crud2gui.desktop.ui;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonEditorViewerTest {

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
    void showsVerticalScrollbarWhenExpandedJsonExceedsViewerHeight(@TempDir Path directory) throws Exception {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < 40; i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("\"field").append(i).append("\":\"value\"");
        }
        json.append('}');
        Path file = Files.writeString(directory.resolve("model.json"), json);
        JsonEditorViewer viewer = new JsonEditorViewer(file, false);

        onJavaFxThread(() -> {
            StackPane root = new StackPane(viewer.getView());
            new Scene(root, 800, 300);
            root.resize(800, 300);
            root.applyCss();
            root.layout();

            TreeView<?> tree = (TreeView<?>) root.lookup(".tree-view");
            ScrollBar verticalBar = tree.lookupAll(".scroll-bar").stream()
                    .map(ScrollBar.class::cast)
                    .filter(scrollBar -> scrollBar.getOrientation() == Orientation.VERTICAL)
                    .findFirst()
                    .orElseThrow();
            assertTrue(verticalBar.isVisible());
            assertTrue(verticalBar.getWidth() >= 12);
            return null;
        });
    }

    private static <T> T onJavaFxThread(java.util.concurrent.Callable<T> action) throws Exception {
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

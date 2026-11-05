package tetris.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsScreenTest {
    @BeforeAll
    static void initializeJavaFx() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        Runnable initialize = () -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        };
        try {
            Platform.startup(initialize);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(initialize);
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS), "JavaFX 초기화 시간 초과");
    }

    @Test
    void buildsSettingsScreenAndRunsBackAction(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.properties");
        runOnFxThread(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(new BorderPane(), 960, 540);
            stage.setScene(scene);
            int[] backCount = {0};
            SettingsScreen screen = new SettingsScreen();

            screen.makeSS(stage, scene, () -> backCount[0]++, new SettingsStore(settingsFile));
            BorderPane root = screen.showSS();
            HBox bottom = (HBox) root.getBottom();
            Button back = (Button) bottom.getChildren().get(0);
            HBox keyActions = (HBox) bottom.getChildren().get(2);

            TabPane settingsTabs = (TabPane) root.getCenter();
            VBox display = (VBox) ((ScrollPane) settingsTabs.getTabs().get(0).getContent()).getContent();
            VBox multi = (VBox) ((ScrollPane) settingsTabs.getTabs().get(2).getContent()).getContent();
            List<String> multiLabels = multi.lookupAll(".label").stream()
                    .map(node -> ((Label) node).getText())
                    .toList();

            assertEquals(null, root.getTop());
            assertEquals(3, settingsTabs.getTabs().size());
            assertEquals("화면", settingsTabs.getTabs().get(0).getText());
            assertEquals("키설정", settingsTabs.getTabs().get(1).getText());
            assertEquals("멀티 설정", settingsTabs.getTabs().get(2).getText());
            assertEquals("스코어보드 초기화", ((Button) display.getChildren().get(6)).getText());
            assertTrue(multiLabels.contains("공통 키 설정"));
            assertTrue(multiLabels.contains("Player 1"));
            assertTrue(multiLabels.contains("Player 2"));
            assertTrue(multiLabels.contains("일시정지"));
            assertTrue(multiLabels.contains("왼쪽 이동"));
            assertFalse(keyActions.isVisible());
            settingsTabs.getSelectionModel().select(1);
            assertTrue(keyActions.isVisible());
            settingsTabs.getSelectionModel().select(2);
            assertTrue(keyActions.isVisible());
            settingsTabs.getSelectionModel().select(0);
            assertFalse(keyActions.isVisible());
            assertEquals("뒤로가기", back.getText());
            assertEquals(180, back.getPrefWidth());
            assertEquals(50, back.getPrefHeight());

            screen.activate();
            back.fire();

            assertEquals(1, backCount[0]);
            assertFalse(Files.exists(settingsFile));
        });
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}

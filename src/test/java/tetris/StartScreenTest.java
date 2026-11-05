package tetris;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StartScreenTest {
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
    void exposesStageAndSceneProvidedAtConstruction() {
        StartScreen screen = new StartScreen(null, null);

        assertNull(screen.getStage());
        assertNull(screen.getScene());
    }

    @Test
    void selectsMenuWithKeysMouseAndCallbacks() throws Exception {
        runOnFxThread(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(new BorderPane(), 960, 540);
            StartScreen screen = new StartScreen(stage, scene);
            int[] selected = {-1};

            screen.makeStartScreen(
                    () -> selected[0] = 0,
                    () -> selected[0] = 1,
                    () -> selected[0] = 2,
                    () -> selected[0] = 3,
                    () -> selected[0] = 4);

            BorderPane root = (BorderPane) screen.showStartScreen();
            VBox buttonBox = (VBox) root.getCenter();
            Button normalMode = (Button) buttonBox.getChildren().get(0);
            Button itemMode = (Button) buttonBox.getChildren().get(1);
            Button scoreBoard = (Button) buttonBox.getChildren().get(2);
            Button settings = (Button) buttonBox.getChildren().get(3);
            Button exit = (Button) buttonBox.getChildren().get(4);

            assertEquals(stage, screen.getStage());
            assertEquals(scene, screen.getScene());
            assertEquals("Tetris", stage.getTitle());
            assertEquals("TETRIS", ((Label) root.getTop()).getText());
            assertEquals("↑↓ 이동   Enter 선택", ((Label) root.getBottom()).getText());
            assertEquals("일반모드 시작", normalMode.getText());
            assertEquals("아이템 모드 시작", itemMode.getText());
            assertEquals("스코어 보드", scoreBoard.getText());
            assertEquals("설정", settings.getText());
            assertEquals("게임 종료", exit.getText());
            assertEquals(180, normalMode.getPrefWidth());
            assertEquals(50, normalMode.getPrefHeight());
            assertTrue(normalMode.getStyle().contains("#4da3ff"));
            assertTrue(itemMode.getStyle().contains("#dcdcdc"));

            fireKey(root, KeyCode.UP);
            fireKey(root, KeyCode.DOWN);
            fireKey(root, KeyCode.UP);
            fireKey(root, KeyCode.ENTER);

            assertTrue(normalMode.getStyle().contains("#4da3ff"));
            assertEquals(0, selected[0]);

            scoreBoard.getOnMouseEntered().handle(null);
            fireKey(root, KeyCode.ENTER);
            itemMode.fire();
            settings.fire();

            assertEquals(3, selected[0]);
            assertTrue(scoreBoard.getStyle().contains("#4da3ff"));

            fireKey(root, KeyCode.DOWN);
            fireKey(root, KeyCode.DOWN);
            assertTrue(exit.getStyle().contains("#4da3ff"));
            assertEquals(3, selected[0]);

            screen.resetSelection();

            assertTrue(normalMode.getStyle().contains("#4da3ff"));
            assertTrue(exit.getStyle().contains("#dcdcdc"));
        });
    }

    private static void fireKey(BorderPane root, KeyCode keyCode) {
        root.fireEvent(new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false));
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}

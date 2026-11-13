package tetris.versusmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.settings.GameSettings.GameMode;
import tetris.versusmode.OnlineLobbyScreen.Role;

class OnlineLobbyScreenTest {
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
    void serverLobbyKeepsStartLockedUntilClientIsReady() throws Exception {
        runOnFxThread(() -> {
            OnlineLobbyScreen server = new OnlineLobbyScreen(Role.SERVER, GameMode.NORMAL, () -> {});

            assertEquals("접속 대기", server.getClientStatusText());
            assertTrue(server.getServerDetailText().matches("\\d+\\.\\d+\\.\\d+\\.\\d+"));
            assertEquals(GameMode.NORMAL, server.getModeCombo().getValue());
            assertEquals("모드 선택", server.getModeCombo().getPromptText());
            assertFalse(server.getModeCombo().isDisabled());
            assertEquals("게임 시작", server.getActionButton().getText());
            assertTrue(server.getActionButton().getStyle().contains("#e53935"));
            assertTrue(server.getActionButton().isDisabled());
            assertEquals(0.35, server.getActionButton().getOpacity());
            assertEquals(OnlineLobbyScreen.BUTTON_HEIGHT, server.getActionButton().getPrefHeight());
            assertTrue(OnlineLobbyScreen.CHAT_HEIGHT > OnlineLobbyScreen.BUTTON_HEIGHT * 2);

            server.getModeCombo().setValue(GameMode.TIMELIMIT);
            server.getModeCombo().fireEvent(new ActionEvent());
            assertEquals(GameMode.TIMELIMIT, server.getMode());

            server.setClientConnected(true);
            assertEquals("준비중", server.getClientStatusText());
            assertTrue(server.getActionButton().isDisabled());

            server.setClientReady(true);
            assertEquals("준비완료", server.getClientStatusText());
            assertFalse(server.getActionButton().isDisabled());
            assertEquals(1, server.getActionButton().getOpacity());

            server.addIncomingMessage("Server: 안녕");
            assertEquals(1, server.getMessages().size());
            server.getActionButton().fire();
            assertTrue(server.getMessages().isEmpty());
        });
    }

    @Test
    void clientLobbyUsesReadyButtonAndCannotChangeMode() throws Exception {
        runOnFxThread(() -> {
            OnlineLobbyScreen client = new OnlineLobbyScreen(Role.CLIENT, GameMode.ITEM, () -> {});

            assertEquals("준비중", client.getClientStatusText());
            assertEquals("준비하기", client.getActionButton().getText());
            assertTrue(client.getModeCombo().isDisabled());
            assertEquals(GameMode.ITEM, client.getModeCombo().getValue());

            client.getActionButton().fire();
            assertEquals("준비중", client.getClientStatusText());

            client.setOnReady(() -> {});
            client.getActionButton().fire();
            assertEquals("준비완료", client.getClientStatusText());

            client.showMode(GameMode.TIMELIMIT);
            assertEquals(GameMode.TIMELIMIT, client.getModeCombo().getValue());
            assertTrue(client.getModeCombo().isDisabled());
        });
    }

    @Test
    void connectScreenShowsRecentHostOrPrompt(@TempDir Path tempDir) throws Exception {
        Path recentFile = tempDir.resolve("recent-host.txt");
        runOnFxThread(() -> {
            OnlineConnectScreen empty = new OnlineConnectScreen(new RecentHostStore(recentFile), host -> {}, () -> {});
            TextField emptyField = hostField(empty);
            assertEquals("", emptyField.getText());
            assertEquals("ip를 입력해주세요", emptyField.getPromptText());

            new RecentHostStore(recentFile).save("10.0.0.8");
            OnlineConnectScreen saved = new OnlineConnectScreen(new RecentHostStore(recentFile), host -> {}, () -> {});
            TextField savedField = hostField(saved);
            Button connect = (Button) ((javafx.scene.layout.VBox) ((StackPane) saved.getRoot()).getChildren().get(0))
                    .getChildren().get(1);
            assertEquals("10.0.0.8", savedField.getText());
            assertEquals("접속하기", connect.getText());
        });
    }

    @Test
    void clientReadyReachesServerLobby() throws Exception {
        OnlineLobbyScreen[] screens = new OnlineLobbyScreen[2];
        OnlineLobbyServer[] servers = new OnlineLobbyServer[1];
        runOnFxThread(() -> {
            screens[0] = new OnlineLobbyScreen(Role.SERVER, GameMode.ITEM, () -> {});
            screens[1] = new OnlineLobbyScreen(Role.CLIENT, GameMode.NORMAL, () -> {});
            servers[0] = new OnlineLobbyServer(screens[0], 0);
            try {
                servers[0].start();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
        OnlineLobbyClient client = new OnlineLobbyClient("127.0.0.1", servers[0].getPort(), screens[1]);
        try {
            client.connect();
            runOnFxThread(() -> {
                screens[1].setOnReady(client::sendReady);
                screens[1].getActionButton().fire();
            });
            assertTrue(waitUntil(() -> {
                try {
                    return readOnFx(() -> ("준비완료".equals(screens[0].getClientStatusText())
                            && !screens[0].getActionButton().isDisabled()
                            && screens[1].getModeCombo().getValue() == GameMode.ITEM));
                } catch (Exception exception) {
                    return false;
                }
            }));
        } finally {
            client.close();
            servers[0].close();
        }
    }

    private static TextField hostField(OnlineConnectScreen screen) {
        return (TextField) ((javafx.scene.layout.VBox) ((StackPane) screen.getRoot()).getChildren().get(0))
                .getChildren().get(0);
    }

    private static boolean waitUntil(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }

    private static boolean readOnFx(java.util.function.BooleanSupplier supplier) throws Exception {
        FutureTask<Boolean> task = new FutureTask<>(supplier::getAsBoolean);
        Platform.runLater(task);
        return task.get(2, TimeUnit.SECONDS);
    }

    private static void runOnFxThread(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null);
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }
}

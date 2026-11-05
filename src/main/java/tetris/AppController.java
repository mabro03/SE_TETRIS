package tetris;

import java.util.Objects;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import tetris.gameoverscene.GameOverRoot;
import tetris.gamescene.GameController;
import tetris.gamescene.GameResult;
import tetris.pausescene.PauseScene;
import tetris.settings.GameSettings;
import tetris.settings.SettingsStore;
import tetris.settings.GameSettings.GameMode;
import tetris.settings.GameSettings.PlayMode;
import tetris.scoreboard.ScoreBoardScreen;
import tetris.settings.SettingsScreen;

/**
 * Scene과 Stage를 가진 시작 화면 루트를 받아, 하나의 창에서 화면을 전환한다.
 */
public class AppController {
    private final Stage stage;
    private final Scene scene;
    private final SettingsStore settingsStore;
    private ScoreBoardScreen scoreBoardScreen;
    private GameController gameController;
    private PauseScene pauseScene;
    private GameOverRoot gameOverRoot;

    public AppController(Stage stage, Scene scene, SettingsStore settingsStore) {
        this.stage = Objects.requireNonNull(stage, "stage cannot be null");
        this.scene = Objects.requireNonNull(scene, "scene cannot be null");
        this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore cannot be null");
    }

    public void ShowStartScreen() {
        StartScreen startScreen = new StartScreen(stage, scene);
        startScreen.makeStartScreen(
            this::ShowSoloGame, this::ShowLocalMultiGame, this::ShowOnlineMultiGame, this::ShowScoreBoard, this::ShowSettings);
        scene.setOnKeyPressed(null);
        stage.setTitle("Tetris");
        scene.setRoot(startScreen.showStartScreen());
        scene.getRoot().requestFocus();
    }

    public void ShowSoloGame() {
        gameModeSelectScreen(PlayMode.SOLO);
    }

    public void ShowLocalMultiGame() {
        gameModeSelectScreen(PlayMode.LOCAL);
    }

    public void ShowOnlineMultiGame() {
        gameModeSelectScreen(PlayMode.ONLINE);
    }

    private void startGame(PlayMode playMode, GameSettings gameSettings) {
        if(playMode == PlayMode.SOLO) {
            gameController = new GameController(
                this::ShowPauseScreen, this::ShowSoloEndGame, gameSettings);
        }
        else {
            gameController = new GameController(
                this::ShowPauseScreen, this::ShowMultiEndGame, gameSettings);
        }
        scene.setOnKeyPressed(null);// 게임 시작 시 키 이벤트 초기화
        scene.setRoot(gameController.GetRoot());// 게임 화면 루트로 전환
        gameController.GetRoot().requestFocus();// 게임 화면 루트에 포커스 설정
        gameController.GameStart();// 게임 시작
    }

    public void ShowSettings() {
        SettingsScreen settingsScreen = new SettingsScreen();
        settingsScreen.makeSS(stage, scene, this::ShowStartScreen, settingsStore);
        scene.setOnKeyPressed(null);
        stage.setTitle("Tetris - 설정");
        scene.setRoot(settingsScreen.showSS());
        settingsScreen.activate();
    }

    public void ShowScoreBoard() {
        if (scoreBoardScreen == null) {
            scoreBoardScreen = new ScoreBoardScreen(this::ShowStartScreen);
        } else {
            scoreBoardScreen.refreshScores();
        }
        scene.setOnKeyPressed(null);
        stage.setTitle("Top 10 Scores");
        scene.setRoot(scoreBoardScreen.showSB());
        scoreBoardScreen.focusBackButton();
    }

    public void ShowPauseScreen() {
        
        if (pauseScene == null) {
            pauseScene = new PauseScene(this::ResumeGame, Platform::exit); // 일시정지 화면에서 종료시 바로 종료해버림
        }

        scene.setOnKeyPressed(null);
        Parent pauseSceneRoot = pauseScene.GetRoot();
        scene.setRoot(pauseSceneRoot);
        pauseSceneRoot.requestFocus();
    }

    public void ResumeGame() {
        if (gameController == null) {
            return;
        }

        scene.setOnKeyPressed(null);
        Parent gameSceneRoot = gameController.GetRoot();
        scene.setRoot(gameSceneRoot);
        gameSceneRoot.requestFocus();
        gameController.ResumeGame();
    }

    public void ShowSoloEndGame(GameResult result) {
        scene.setOnKeyPressed(null);
        gameOverRoot = new GameOverRoot(result, this::ShowStartScreen);
        scene.setRoot(gameOverRoot);
        gameOverRoot.requestFocus();
        gameController = null;
        pauseScene = null;
    }

    public void ShowMultiEndGame(GameResult result) {
        //TODO: 멀티플레이 종료 화면 구현
    }

    // 게임 모드 선택 화면을 표시하고, 선택된 모드에 따라 게임 설정을 저장
    public void gameModeSelectScreen(PlayMode playMode) {
        Button[] modebuttons;

        if(playMode == PlayMode.SOLO) {
            modebuttons = new Button[2];
            modebuttons[0] = new Button("일반 모드");
            modebuttons[1] = new Button("아이템 모드");
        }
        else {
            modebuttons = new Button[3];
            modebuttons[0] = new Button("일반 모드");
            modebuttons[1] = new Button("아이템 모드");
            modebuttons[2] = new Button("시간제한 모드");
        }
        makeModeSelectionScreen(modebuttons);
        storeModeSelection(playMode, modebuttons);
    }

    // 게임 모드 선택 화면을 생성
    private void makeModeSelectionScreen(Button[] modebuttons) {
        Label title = new Label("모드 선택");
        title.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox modeSelectionRoot = new VBox(20);
        modeSelectionRoot.setAlignment(Pos.CENTER);
        modeSelectionRoot.setStyle("-fx-background-color: black;");

        for (Button button : modebuttons) {
            button.setPrefSize(180, 50);
        }
        Button startMenuButton = new Button("시작화면");
        startMenuButton.setPrefSize(180, 50);
        startMenuButton.setOnAction(event -> ShowStartScreen());

        Button[] selectableButtons = new Button[modebuttons.length + 1];
        System.arraycopy(modebuttons, 0, selectableButtons, 0, modebuttons.length);
        selectableButtons[modebuttons.length] = startMenuButton;
        modeSelectionRoot.getChildren().add(title);
        modeSelectionRoot.getChildren().addAll(selectableButtons);
        bindModeSelection(modeSelectionRoot, selectableButtons);

        scene.setOnKeyPressed(null);
        stage.setTitle("Tetris - 모드 선택");
        scene.setRoot(modeSelectionRoot);
        modeSelectionRoot.requestFocus();
    }

    // 시작 화면과 같이 마우스, 위·아래 방향키, Enter로 버튼을 선택하고 실행한다.
    private void bindModeSelection(Parent root, Button[] buttons) {
        int[] selectedIndex = {0};
        updateModeSelection(buttons, selectedIndex[0]);
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].setOnMouseEntered(event -> {
                selectedIndex[0] = index;
                updateModeSelection(buttons, selectedIndex[0]);
            });
            buttons[i].setFocusTraversable(false);
        }
        root.addEventFilter(KeyEvent.KEY_PRESSED,
                event -> handleModeKey(event, buttons, selectedIndex));
    }

    private void handleModeKey(KeyEvent event, Button[] buttons, int[] selectedIndex) {
        if (event.getCode() == KeyCode.DOWN) {
            if (selectedIndex[0] < buttons.length - 1) {
                selectedIndex[0]++;
                updateModeSelection(buttons, selectedIndex[0]);
            }
        } else if (event.getCode() == KeyCode.UP) {
            if (selectedIndex[0] > 0) {
                selectedIndex[0]--;
                updateModeSelection(buttons, selectedIndex[0]);
            }
        } else if (event.getCode() == KeyCode.ENTER) {
            buttons[selectedIndex[0]].fire();
        }
    }

    private void updateModeSelection(Button[] buttons, int selectedIndex) {
        for (int i = 0; i < buttons.length; i++) {
            if (i == selectedIndex) {
                buttons[i].setStyle("-fx-background-color: #4da3ff; -fx-text-fill: white;");
            } else {
                buttons[i].setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: black;");
            }
        }
    }

    // 버튼 선택 시 모드를 저장하고 그 모드로 게임을 시작한다.
    private void storeModeSelection(PlayMode playMode, Button[] modebuttons) {
        GameMode[] availableModes = {
            GameMode.NORMAL,
            GameMode.ITEM,
            GameMode.TIMELIMIT
        };
        for (int i = 0; i < modebuttons.length; i++) {
            GameMode selectedMode = availableModes[i];
            modebuttons[i].setOnAction(event -> {
                settingsStore.saveGameMode(selectedMode);
                startGame(playMode, settingsStore.gameSettingsFor(playMode));
            });
        }
    }
}

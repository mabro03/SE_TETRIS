package tetris.settings;

import javafx.geometry.Insets; // 여백 설정
import javafx.geometry.Pos; // 정렬 위치
import javafx.scene.Scene; // JavaFX 화면 객체
import javafx.scene.control.ComboBox; // 선택 상자
import javafx.scene.control.Button; // 버튼 컨트롤
import javafx.scene.control.Label; // 텍스트 라벨
import javafx.scene.control.Tab; // 설정 탭
import javafx.scene.control.TabPane; // 설정 탭 목록
import javafx.scene.layout.BorderPane; // 전체 화면 배치 레이아웃
import javafx.scene.layout.HBox; // 역할 이름과 키 버튼을 한 줄로 배치
import javafx.scene.layout.Priority; // 키 버튼이 남은 너비를 사용하도록 설정
import javafx.scene.layout.Region; // 하단 버튼을 좌우로 밀어내는 여백
import javafx.scene.layout.VBox; // 세로 배치 레이아웃
import java.util.ArrayList; // 플레이어 열 목록
import java.util.List; // 플레이어 열 목록
import javafx.scene.control.ScrollPane; // 설정 항목이 창보다 많을 때 스크롤
import javafx.stage.Stage; // 창 객체
import tetris.settings.KeyBindingSettings.ActionType; // 키 역할 목록

// 설정 화면을 보여주는 클래스
public class SettingsScreen {
    private BorderPane root;
    private SettingsController controller;
    private Label localStatus;

    public BorderPane showSS() {
        return root;
    }

    public void activate() {
        controller.activate();
    }

    public void makeSS(Stage stage, Scene scene, Runnable onBack, SettingsStore settingsStore) {
        ComboBox<String> resolutionComboBox = new ComboBox<>();
        resolutionComboBox.setPromptText("화면 해상도 선택");
        resolutionComboBox.setPrefWidth(220);

        ComboBox<GameSettings.ColorBlindMode> colorBlindModeComboBox = new ComboBox<>();
        colorBlindModeComboBox.setPromptText("색상 모드 선택");
        colorBlindModeComboBox.setPrefWidth(220);

        ComboBox<GameSettings.Difficulty> difficultyComboBox = new ComboBox<>();
        difficultyComboBox.setPromptText("난이도 선택");
        difficultyComboBox.setPrefWidth(220);

        Label displayStatus = new Label("화면 설정을 선택한 뒤 저장하세요.");
        displayStatus.setStyle("-fx-font-size: 15px; -fx-text-fill: white;");
        Button displaySaveButton = createActionButton("설정 저장", 180, 44);
        Button displayResetButton = createActionButton("설정 초기화", 180, 50);
        Button resetScoreButton = createActionButton("스코어보드 초기화", 180, 50);
        VBox displayContent = new VBox(14, resolutionComboBox, colorBlindModeComboBox, difficultyComboBox,
                displayStatus, displaySaveButton, displayResetButton, resetScoreButton);
        displayContent.setAlignment(Pos.CENTER);
        displayContent.setFillWidth(false);
        displayContent.setPadding(new Insets(20));
        displayContent.setStyle("-fx-background-color: black;");

        Label keySettingsStatus = new Label("역할 버튼을 누른 뒤 새 키를 입력하세요.");
        keySettingsStatus.setStyle("-fx-font-size: 15px; -fx-text-fill: white;");
        VBox keyBindingRows = new VBox(8);
        for (ActionType actionType : ActionType.values()) {
            keyBindingRows.getChildren().add(createKeyRow(actionType.getDisplayName(), 190));
        }
        VBox keyContent = new VBox(14, keySettingsStatus, keyBindingRows);
        keyContent.setAlignment(Pos.CENTER);
        keyContent.setPadding(new Insets(20));
        keyContent.setStyle("-fx-background-color: black;");

        VBox commonKeyRows = createActionRows(commonActions());
        List<VBox> playerKeyRows = new ArrayList<>();
        VBox multiContent = createLocalSettingsContent(
                commonKeyRows, playerKeyRows, settingsStore.localPlayerKeys().size());

        TabPane settingsTabs = new TabPane(
                createTab("화면", displayContent),
                createTab("키설정", keyContent),
                createTab("멀티 설정", multiContent));
        settingsTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Button backButton = createActionButton("뒤로가기", 180, 50);
        Button saveSettingsButton = createActionButton("설정 저장", 180, 44);
        Button resetSettingsButton = createActionButton("설정 초기화", 180, 50);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox keyActionBox = new HBox(12, saveSettingsButton, resetSettingsButton);
        keyActionBox.setAlignment(Pos.CENTER_RIGHT);
        HBox bottomBar = new HBox(12, backButton, spacer, keyActionBox);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(12));

        root = new BorderPane();
        root.setStyle("-fx-background-color: black; -fx-font-size: 16px;");
        root.setCenter(settingsTabs);
        root.setBottom(bottomBar);

        controller = new SettingsController(stage, scene, resolutionComboBox,
                colorBlindModeComboBox, difficultyComboBox, settingsStore.getInputSettingData(), keyBindingRows,
                keySettingsStatus, saveSettingsButton, resetSettingsButton, resetScoreButton,
                backButton, settingsStore);
        controller.attachLocalKeys(localStatus, commonKeyRows, playerKeyRows);
        controller.attachTabActions(settingsTabs, keyActionBox);
        displaySaveButton.setOnAction(e -> controller.saveCurrentSettings(displayStatus));
        displayResetButton.setOnAction(e -> controller.resetDisplaySettings(displayStatus));
        backButton.setOnAction(e -> {
            controller.dispose();
            onBack.run();
        });

        controller.initialize();
    }

    private Tab createTab(String title, VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: black; -fx-background-color: black;");
        Tab tab = new Tab(title, scrollPane);
        tab.setClosable(false);
        return tab;
    }

    private Button createActionButton(String text, double width, double height) {
        Button button = new Button(text);
        button.setPrefSize(width, height);
        button.setStyle("-fx-font-size: 16px;");
        return button;
    }

    private VBox createLocalSettingsContent(VBox commonKeyRows, List<VBox> playerKeyRows, int playerCount) {
        Label commonTitle = new Label("공통 키 설정");
        commonTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox players = new HBox(40);
        players.setAlignment(Pos.TOP_CENTER);
        List<ActionType> playerActionTypes = playerActions();
        for (int playerNumber = 1; playerNumber <= playerCount; playerNumber++) {
            Label playerTitle = new Label("Player " + playerNumber);
            playerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
            VBox rows = createActionRows(playerActionTypes);
            playerKeyRows.add(rows);
            VBox column = new VBox(10, playerTitle, rows);
            column.setAlignment(Pos.TOP_CENTER);
            HBox.setHgrow(column, Priority.ALWAYS);
            players.getChildren().add(column);
        }

        localStatus = new Label("역할 버튼을 누른 뒤 새 키를 입력하세요.");
        localStatus.setStyle("-fx-font-size: 15px; -fx-text-fill: white;");

        VBox content = new VBox(16, commonTitle, commonKeyRows, players, localStatus);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: black;");
        return content;
    }

    private VBox createActionRows(List<ActionType> actions) {
        VBox rows = new VBox(8);
        rows.setAlignment(Pos.CENTER);
        for (ActionType actionType : actions) {
            rows.getChildren().add(createKeyRow(actionType.getDisplayName()));
        }
        return rows;
    }

    private HBox createKeyRow(String actionName) {
        return createKeyRow(actionName, 160);
    }

    private HBox createKeyRow(String actionName, double labelWidth) {
        Label actionLabel = new Label(actionName);
        actionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        actionLabel.setPrefWidth(labelWidth);
        Button keyButton = new Button();
        keyButton.setMaxWidth(Double.MAX_VALUE);
        keyButton.setPrefWidth(140);
        keyButton.setStyle("-fx-font-size: 16px;");
        HBox.setHgrow(keyButton, Priority.ALWAYS);
        HBox row = new HBox(12, actionLabel, keyButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private List<ActionType> commonActions() {
        return List.of(ActionType.PAUSE);
    }

    private List<ActionType> playerActions() {
        return List.of(
                ActionType.MOVE_LEFT,
                ActionType.MOVE_RIGHT,
                ActionType.SOFT_DROP,
                ActionType.HARD_DROP,
                ActionType.ROTATE_CLOCK,
                ActionType.ROTATE_COUNTERCLOCK,
                ActionType.HOLD);
    }
}

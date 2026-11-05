package tetris;

import javafx.geometry.Insets; // 안내 문구 여백
import javafx.geometry.Pos; // 화면 요소의 정렬 위치
import javafx.scene.Parent; // 시작 메뉴 루트
import javafx.scene.Scene; // 화면에 표시할 장면
import javafx.scene.control.Button; // 버튼 컨트롤
import javafx.scene.control.Label; // 제목과 문구를 표시
import javafx.scene.input.KeyCode; // 키보드 키 종류
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane; // 화면을 상·중·하로 나누는 레이아웃
import javafx.scene.layout.VBox; // 버튼을 세로로 배치하는 레이아웃
import javafx.stage.Stage; // 실제 앱 창

// 테트리스의 시작 메뉴 화면. Scene과 Stage를 받아 메뉴 루트를 만든다.
public class StartScreen {
    private final Stage stage;
    private final Scene scene;
    private Button[] buttons;//버튼 배열 생성
    private int selectedIndex = 0;//선택된 버튼의 인덱스 초기화
    private BorderPane root;

    public StartScreen(Stage stage, Scene scene) {
        this.stage = stage;
        this.scene = scene;
    }

    public Stage getStage() {
        return stage;
    }

    public Scene getScene() {
        return scene;
    }

    private void updateSelection() {// 선택된 버튼과 일반 버튼의 색상 갱신
        for (int i = 0; i < buttons.length; i++) {//버튼 배열의 길이만큼 반복
            if (i == selectedIndex) {//선택된 버튼이면 푸른색으로 변경
                buttons[i].setStyle("-fx-background-color: #4da3ff; -fx-text-fill: white;");
            } else {//선택되지 않은 버튼이면 회색으로 변경
                buttons[i].setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: black;");
            }
        }
    }

    public void makeStartScreen(
            Runnable soloPlay,
            Runnable localPlay,
            Runnable onlinePlay,
            Runnable onScoreBoard,
            Runnable onSettings) {
        selectedIndex = 0;
        Label title = createTitleLabel();
        buttons = createMenuButtons();
        bindMenuActions(soloPlay, localPlay, onlinePlay, onScoreBoard, onSettings);
        bindMouseSelection();
        root = createStartRoot(title, createButtonBox(), createKeyHint());
        bindKeySelection();
        updateSelection();
        stage.setTitle("Tetris");
    }

    private Label createTitleLabel() {
        Label title = new Label("TETRIS");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");
        return title;
    }

    private Button[] createMenuButtons() {
        Button[] menuButtons = {
            new Button("Solo play"),
            new Button("Local multiplay"),
            new Button("Online multiplay"),
            new Button("Scoreboard"),
            new Button("Settings"),
            new Button("Exit")
        };
        for (Button button : menuButtons) {
            button.setPrefSize(180, 50);
        }
        return menuButtons;
    }

    private void bindMenuActions(//버튼 선택 시 동작 연결
            Runnable soloPlay,
            Runnable localPlay,
            Runnable onlinePlay,
            Runnable onScoreBoard,
            Runnable onSettings) {
        buttons[0].setOnAction(e -> soloPlay.run());
        buttons[1].setOnAction(e -> localPlay.run());
        buttons[2].setOnAction(e -> onlinePlay.run());
        buttons[3].setOnAction(e -> onScoreBoard.run());
        buttons[4].setOnAction(e -> onSettings.run());
        buttons[5].setOnAction(e -> System.exit(0));
    }

    private void bindMouseSelection() {
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].setOnMouseEntered(e -> {
                selectedIndex = index;
                updateSelection();
            });
            buttons[i].setFocusTraversable(false);
        }
    }

    private VBox createButtonBox() {//버튼을 세로로 배치하는 VBox 생성
        VBox buttonBox = new VBox(20);
        buttonBox.getChildren().addAll(buttons);
        buttonBox.setAlignment(Pos.CENTER);
        return buttonBox;
    }

    private Label createKeyHint() {
        Label keyHint = new Label("↑↓ 이동   Enter 선택");
        keyHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #aaaaaa;");
        return keyHint;
    }

    //만든 화면 요소를 배치
    private BorderPane createStartRoot(Label title, VBox buttonBox, Label keyHint) {
        BorderPane screen = new BorderPane();
        screen.setStyle("-fx-background-color: black;");
        screen.setTop(title);
        BorderPane.setAlignment(title, Pos.CENTER);
        screen.setCenter(buttonBox);
        screen.setBottom(keyHint);
        BorderPane.setAlignment(keyHint, Pos.BOTTOM_RIGHT);
        BorderPane.setMargin(keyHint, new Insets(0, 16, 12, 0));
        return screen;
    }

    private void bindKeySelection() {
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleMenuKey);
    }

    private void handleMenuKey(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN) {
            if (selectedIndex < buttons.length - 1) {
                selectedIndex++;
                updateSelection();
            }
        } else if (event.getCode() == KeyCode.UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                updateSelection();
            }
        } else if (event.getCode() == KeyCode.ENTER) {
            buttons[selectedIndex].fire();
        }
    }

    public Parent showStartScreen() {
        return root;
    }

    public void resetSelection() {
        selectedIndex = 0;
        updateSelection();
    }
}

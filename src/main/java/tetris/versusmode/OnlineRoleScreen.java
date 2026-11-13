package tetris.versusmode;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

// 온라인 멀티에서 서버와 클라이언트를 고르는 화면.
public final class OnlineRoleScreen {
    private final BorderPane root;
    private final Label errorLabel = new Label();
    private final Button[] buttons;
    private int selectedIndex;

    public OnlineRoleScreen(Runnable onServer, Runnable onClient, Runnable onBack) {
        Label title = new Label("온라인 멀티");
        title.setStyle("-fx-font-size: 40px; -fx-font-weight: bold; -fx-text-fill: white;");

        Button serverButton = createButton("Server");
        Button clientButton = createButton("Client");
        Button backButton = createButton("시작화면");
        serverButton.setOnAction(event -> onServer.run());
        clientButton.setOnAction(event -> onClient.run());
        backButton.setOnAction(event -> onBack.run());
        buttons = new Button[] {serverButton, clientButton, backButton};

        errorLabel.setStyle("-fx-text-fill: #ff8a8a; -fx-font-size: 14px;");
        VBox menu = new VBox(20, title, serverButton, clientButton, backButton, errorLabel);
        menu.setAlignment(Pos.CENTER);

        root = new BorderPane(menu);
        root.setStyle("-fx-background-color: black;");
        bindSelection();
        updateSelection();
    }

    public Parent getRoot() {
        return root;
    }

    public void showError(String message) {
        errorLabel.setText(message == null ? "" : message);
    }

    private Button createButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(180, 50);
        button.setFocusTraversable(false);
        return button;
    }

    private void bindSelection() {
        for (int index = 0; index < buttons.length; index++) {
            int selected = index;
            buttons[index].setOnMouseEntered(event -> {
                selectedIndex = selected;
                updateSelection();
            });
        }
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKey);
        root.setPadding(new Insets(16));
    }

    private void handleKey(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN && selectedIndex < buttons.length - 1) {
            selectedIndex++;
            updateSelection();
        } else if (event.getCode() == KeyCode.UP && selectedIndex > 0) {
            selectedIndex--;
            updateSelection();
        } else if (event.getCode() == KeyCode.ENTER) {
            buttons[selectedIndex].fire();
        }
    }

    private void updateSelection() {
        for (int index = 0; index < buttons.length; index++) {
            if (index == selectedIndex) {
                buttons[index].setStyle("-fx-background-color: #4da3ff; -fx-text-fill: white;");
            } else {
                buttons[index].setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: black;");
            }
        }
    }
}

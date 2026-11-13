package tetris.onlinemulti;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

// 클라이언트가 접속할 호스트 주소를 입력하는 화면.
public final class OnlineConnectScreen {
    private final StackPane root;
    private final TextField hostField = new TextField();
    private final Label errorLabel = new Label();
    private final Button connectButton = new Button("접속하기");

    public OnlineConnectScreen(RecentHostStore recentHosts, java.util.function.Consumer<String> onConnect,
            Runnable onBack) {
        String recentHost = recentHosts.load();
        hostField.setPromptText("ip를 입력해주세요");
        hostField.setText(recentHost);
        hostField.setPrefWidth(280);
        hostField.setStyle("-fx-font-size: 16px;");

        connectButton.setPrefSize(180, 50);
        connectButton.setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: black; -fx-font-size: 16px;");
        connectButton.setOnAction(event -> submit(onConnect));
        hostField.setOnAction(event -> submit(onConnect));

        Button backButton = new Button("뒤로가기");
        backButton.setPrefSize(180, 50);
        backButton.setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: black; -fx-font-size: 16px;");
        backButton.setOnAction(event -> onBack.run());

        errorLabel.setStyle("-fx-text-fill: #ff8a8a; -fx-font-size: 14px;");
        VBox form = new VBox(16, hostField, connectButton, backButton, errorLabel);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(320);

        root = new StackPane(form);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: black;");
    }

    public Parent getRoot() {
        return root;
    }

    public void showError(String message) {
        connectButton.setDisable(false);
        errorLabel.setText(message == null ? "" : message);
    }

    private void submit(java.util.function.Consumer<String> onConnect) {
        String host = hostField.getText() == null ? "" : hostField.getText().trim();
        if (host.isEmpty()) {
            showError("ip를 입력해주세요");
            return;
        }
        connectButton.setDisable(true);
        errorLabel.setText("");
        onConnect.accept(host);
    }
}

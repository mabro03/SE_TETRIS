package tetris.onlinemulti;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tetris.settings.GameSettings.GameMode;

// 서버와 클라이언트가 같이 보는 대전 대기 화면.
public final class OnlineLobbyScreen {
    public static final double BUTTON_HEIGHT = 90;
    public static final double CHAT_HEIGHT = 300;
    private static final String START_BUTTON_STYLE =
            "-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-size: 16px;";

    public enum Role {
        SERVER, CLIENT
    }

    private final Role role;
    private final Runnable onBack;
    private GameMode mode;
    private boolean clientConnected;
    private boolean clientReady;
    private Consumer<GameMode> onModeChanged;
    private Consumer<String> onChat;
    private Runnable onReady;
    private Runnable onStart;

    private final Label clientStatus = new Label();
    private final Label serverDetail = new Label();
    private final ComboBox<GameMode> modeCombo = new ComboBox<>();
    private final Button actionButton = new Button();
    private boolean applyingMode;
    private final VBox messageBox = new VBox(4);
    private final List<String> messages = new ArrayList<>();
    private final BorderPane root;

    public OnlineLobbyScreen(Role role, GameMode initialMode, Runnable onBack) {
        this.role = role;
        this.onBack = onBack;
        this.mode = initialMode == null ? GameMode.NORMAL : initialMode;
        this.clientConnected = role == Role.CLIENT;
        root = createRoot();
        refresh();
    }

    public Parent getRoot() {
        return root;
    }

    public Role getRole() {
        return role;
    }

    public GameMode getMode() {
        return mode;
    }

    public ComboBox<GameMode> getModeCombo() {
        return modeCombo;
    }

    public Button getActionButton() {
        return actionButton;
    }

    public String getClientStatusText() {
        return clientStatus.getText();
    }

    public void setOnModeChanged(Consumer<GameMode> onModeChanged) {
        this.onModeChanged = onModeChanged;
    }

    public void setOnChat(Consumer<String> onChat) {
        this.onChat = onChat;
    }

    public void setOnReady(Runnable onReady) {
        this.onReady = onReady;
    }

    public void setOnStart(Runnable onStart) {
        this.onStart = onStart;
    }

    public String getServerDetailText() {
        return serverDetail.getText();
    }

    public List<String> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public void showMode(GameMode mode) {
        this.mode = mode == null ? GameMode.NORMAL : mode;
        refreshModeCombo();
    }

    public void showServerAddress(String address) {
        serverDetail.setText(address == null ? "" : address);
    }

    public void addIncomingMessage(String message) {
        addMessage(message, "-fx-text-fill: white; -fx-font-size: 14px;");
    }

    //메시지를 채팅창에 공지로 띄우기
    public void addAnnouncementMessage(String message) {
        addMessage(message, "-fx-text-fill: yellow; -fx-font-size: 14px; -fx-font-weight: bold;");
    }

    private void addMessage(String message, String style) {
        if (message == null || message.isBlank()) {
            return;
        }
        messages.add(message);
        Label line = new Label(message);
        line.setWrapText(true);
        line.setStyle(style);
        messageBox.getChildren().add(line);
    }

    public void clearMessages() {
        messages.clear();
        messageBox.getChildren().clear();
    }

    public void setClientConnected(boolean connected) {
        clientConnected = connected;
        if (!connected) {
            clientReady = false;
        }
        refreshClientStatus();
        refreshActionButton();
    }

    public void setClientReady(boolean ready) {
        clientReady = ready;
        if (ready) {
            clientConnected = true;
        }
        refreshClientStatus();
        refreshActionButton();
    }

    private BorderPane createRoot() {
        Label versus = new Label("VS");
        versus.setStyle("-fx-font-size: 64px; -fx-font-weight: bold; -fx-text-fill: white;");
        StackPane versusPane = new StackPane(versus);
        versusPane.setAlignment(Pos.CENTER);
        versusPane.setMaxHeight(Double.MAX_VALUE);

        serverDetail.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        serverDetail.setText(role == Role.SERVER ? currentIpv4() : "방장");
        clientStatus.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

        VBox serverBox = createPlayerBox("Server", "#ffe14a", serverDetail, role == Role.SERVER);
        VBox clientBox = createPlayerBox("Client", "white", clientStatus, role == Role.CLIENT);
        HBox.setHgrow(serverBox, Priority.ALWAYS);
        HBox.setHgrow(clientBox, Priority.ALWAYS);
        HBox players = new HBox(16, serverBox, versusPane, clientBox);
        players.setAlignment(Pos.CENTER);
        BorderPane.setMargin(players, new Insets(20, 0, 20, 0));

        Button backButton = new Button("뒤로가기");
        backButton.setFocusTraversable(false);
        backButton.setStyle("-fx-background-color: #dcdcdc; -fx-text-fill: black; -fx-font-size: 14px;");
        backButton.setOnAction(event -> onBack.run());

        VBox chat = createChatBox();
        Parent actions = createActionPane();

        HBox bottom = new HBox(16, chat, actions);
        bottom.setAlignment(Pos.BOTTOM_LEFT);

        BorderPane screen = new BorderPane();
        screen.setStyle("-fx-background-color: black;");
        screen.setPadding(new Insets(20));
        screen.setTop(backButton);
        screen.setCenter(players);
        screen.setBottom(bottom);
        screen.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && !(event.getTarget() instanceof TextField)) {
                onBack.run();
            }
        });
        return screen;
    }

    private VBox createChatBox() {
        messageBox.setAlignment(Pos.TOP_LEFT);
        messageBox.setFillWidth(true);
        ScrollPane history = new ScrollPane(messageBox);
        history.setFitToWidth(true);
        history.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        history.setStyle("-fx-background: #1a1a1a; -fx-background-color: transparent;");
        VBox.setVgrow(history, Priority.ALWAYS);

        TextField input = new TextField();
        input.setPromptText("메시지를 입력하세요");
        input.setStyle("-fx-font-size: 14px;");
        input.setOnAction(event -> submitChat(input));

        VBox chat = new VBox(8, history, input);
        chat.setPadding(new Insets(8));
        chat.setMinHeight(CHAT_HEIGHT);
        chat.setPrefHeight(CHAT_HEIGHT);
        chat.setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #dcdcdc;");
        HBox.setHgrow(chat, Priority.ALWAYS);
        return chat;
    }

    private void submitChat(TextField input) {
        String text = input.getText() == null ? "" : input.getText().trim();
        input.clear();
        if (text.isEmpty()) {
            return;
        }
        String line = (role == Role.SERVER ? "Server" : "Client") + ": " + text;
        addIncomingMessage(line);
        if (onChat != null) {
            onChat.accept(line);
        }
    }
    
    private Parent createActionPane() {
        configureActionButtons();
        VBox actions = new VBox(8, modeCombo, actionButton);
        actions.setPrefWidth(220);
        actions.setMaxWidth(220);
        actions.setAlignment(Pos.BOTTOM_CENTER);
        return actions;
    }

    private VBox createPlayerBox(String titleText, String titleColor, Label detail, boolean highlighted) {
        Label title = new Label(titleText);
        String titleStyle = highlighted
                ? "-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";"
                : "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + "; -fx-opacity: 0.7;";
        title.setStyle(titleStyle);
        VBox box = new VBox(8, title, detail);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(16));
        box.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        if (highlighted) {
            box.setStyle(
                    "-fx-background-color: #2a2a2a; -fx-border-color: " + titleColor
                            + "; -fx-border-width: 3;");
        } else {
            box.setStyle("-fx-background-color: #141414; -fx-border-color: #666666; -fx-border-width: 1;");
            box.setOpacity(0.7);
        }
        return box;
    }

    private void configureActionButtons() {
        modeCombo.getItems().setAll(GameMode.values());
        modeCombo.setPromptText("모드 선택");
        modeCombo.setMaxWidth(Double.MAX_VALUE);
        modeCombo.setPrefWidth(220);
        modeCombo.setMinHeight(72);
        modeCombo.setPrefHeight(72);
        modeCombo.setMaxHeight(72);
        modeCombo.setFocusTraversable(false);
        modeCombo.setOnAction(event -> {
            if (applyingMode || role != Role.SERVER || modeCombo.getValue() == null) {
                return;
            }
            mode = modeCombo.getValue();
            if (onModeChanged != null) {
                onModeChanged.accept(mode);
            }
        });

        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setMinHeight(BUTTON_HEIGHT);
        actionButton.setPrefHeight(BUTTON_HEIGHT);
        actionButton.setMaxHeight(BUTTON_HEIGHT);
        actionButton.setFocusTraversable(false);
        actionButton.setText(role == Role.SERVER ? "게임 시작" : "준비하기");
        actionButton.setOnAction(event -> {
            if (actionButton.isDisabled()) {
                return;
            }
            if (role == Role.CLIENT) {
                if (onReady == null) {
                    return;
                }
                setClientReady(true);
                onReady.run();
                return;
            }
            clearMessages();
            if (onStart != null) {
                onStart.run();
            }
        });
    }

    private void refresh() {
        refreshModeCombo();
        refreshClientStatus();
        refreshActionButton();
    }

    private void refreshModeCombo() {
        applyingMode = true;
        modeCombo.setValue(mode);
        applyingMode = false;
        boolean serverCanEdit = role == Role.SERVER;
        modeCombo.setDisable(!serverCanEdit);
        modeCombo.setOpacity(serverCanEdit ? 1 : 0.35);
    }

    private void refreshClientStatus() {
        if (!clientConnected) {
            clientStatus.setText("접속 대기");
        } else if (clientReady) {
            clientStatus.setText("준비완료");
        } else {
            clientStatus.setText("준비중");
        }
    }

    private void refreshActionButton() {
        actionButton.setStyle(START_BUTTON_STYLE);
        if (role == Role.CLIENT) {
            actionButton.setDisable(false);
            actionButton.setOpacity(1);
            return;
        }
        actionButton.setDisable(!clientReady);
        actionButton.setOpacity(clientReady ? 1 : 0.35);
    }

    static String currentIpv4() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress("8.8.8.8", 53));
            InetAddress localAddress = socket.getLocalAddress();
            if (localAddress instanceof Inet4Address ipv4 && !ipv4.isAnyLocalAddress()) {
                return ipv4.getHostAddress();
            }
        } catch (Exception exception) {
            // 경로가 없으면 아래 인터페이스 조회로 넘어간다.
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address ipv4 && !ipv4.isLoopbackAddress()) {
                        return ipv4.getHostAddress();
                    }
                }
            }
        } catch (SocketException exception) {
            // 주소를 찾지 못하면 루프백을 보여 준다.
        }
        return "127.0.0.1";
    }
}

package tetris.versusmode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javafx.application.Platform;
import tetris.settings.GameSettings.GameMode;

// 대기실 서버. 클라이언트 한 명의 준비 상태와 모드를 주고받는다.
public final class OnlineLobbyServer implements AutoCloseable {
    public static final int DEFAULT_PORT = 7777;

    private final OnlineLobbyScreen lobby;
    private final int requestedPort;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter clientWriter;
    private volatile boolean running;
    private final Object writeLock = new Object();

    public OnlineLobbyServer(OnlineLobbyScreen lobby) {
        this(lobby, DEFAULT_PORT);
    }

    public OnlineLobbyServer(OnlineLobbyScreen lobby, int port) {
        this.lobby = lobby;
        this.requestedPort = port;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(requestedPort);
        running = true;
        lobby.showServerAddress(OnlineLobbyScreen.currentIpv4());
        lobby.setOnModeChanged(this::sendMode);
        lobby.setOnChat(text -> send("CHAT " + text));
        lobby.setOnStart(() -> send("START"));
        Thread acceptThread = new Thread(this::acceptLoop, "online-lobby-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int getPort() {
        return serverSocket == null ? requestedPort : serverSocket.getLocalPort();
    }

    public void sendMode(GameMode mode) {
        send("MODE " + (mode == null ? GameMode.NORMAL.name() : mode.name()));
    }

    @Override
    public void close() {
        running = false;
        closeQuietly(clientSocket);
        closeQuietly(serverSocket);
        synchronized (writeLock) {
            clientWriter = null;
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                closeQuietly(clientSocket);
                clientSocket = socket;
                synchronized (writeLock) {
                    clientWriter = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                }
                Platform.runLater(() -> lobby.setClientConnected(true));
                sendMode(lobby.getMode());
                readClient(socket);
            } catch (IOException exception) {
                if (running) {
                    Platform.runLater(() -> lobby.setClientConnected(false));
                }
            }
        }
    }

    private void readClient(Socket socket) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if ("READY".equals(line)) {
                    Platform.runLater(() -> lobby.setClientReady(true));
                } else if (line.startsWith("CHAT ")) {
                    String message = line.substring("CHAT ".length());
                    Platform.runLater(() -> lobby.addIncomingMessage(message));
                }
            }
        } finally {
            synchronized (writeLock) {
                clientWriter = null;
            }
            Platform.runLater(() -> lobby.setClientConnected(false));
        }
    }

    private void send(String line) {
        synchronized (writeLock) {
            if (clientWriter != null) {
                clientWriter.println(line);
            }
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception exception) {
            // 이미 닫힌 접속은 무시한다.
        }
    }
}

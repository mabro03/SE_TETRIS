package tetris.onlinemulti;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javafx.application.Platform;
import tetris.settings.GameSettings.GameMode;

// 대기실 클라이언트. 호스트의 모드를 받고 준비 상태를 보낸다.
public final class OnlineLobbyClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final OnlineLobbyScreen lobby;
    private Socket socket;
    private PrintWriter writer;
    private final Object writeLock = new Object();

    public OnlineLobbyClient(String hostText, OnlineLobbyScreen lobby) {
        HostAddress address = HostAddress.parse(hostText);
        this.host = address.host();
        this.port = address.port();
        this.lobby = lobby;
    }

    public OnlineLobbyClient(String host, int port, OnlineLobbyScreen lobby) {
        this.host = host;
        this.port = port;
        this.lobby = lobby;
    }

    public void connect() throws IOException {
        Socket connected = new Socket();
        connected.connect(new InetSocketAddress(host, port), 3000);
        socket = connected;
        synchronized (writeLock) {
            writer = new PrintWriter(
                    new OutputStreamWriter(connected.getOutputStream(), StandardCharsets.UTF_8), true);
        }
        lobby.setOnChat(text -> sendLine("CHAT " + text));
        Platform.runLater(() -> lobby.showServerAddress(host));
        Thread readerThread = new Thread(this::readHost, "online-lobby-client");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void sendReady() {
        sendLine("READY");
        lobby.addAnnouncementMessage("Client가 준비를 완료했습니다.");
    }

    private void sendLine(String line) {
        synchronized (writeLock) {
            if (writer != null) {
                writer.println(line);
            }
        }
    }

    @Override
    public void close() {
        synchronized (writeLock) {
            writer = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException exception) {
                // 이미 닫힌 접속은 무시한다.
            }
        }
    }

    private void readHost() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MODE ")) {
                    GameMode mode = GameMode.valueOf(line.substring("MODE ".length()).trim());
                    Platform.runLater(() -> {
                        lobby.showMode(mode);
                        lobby.addAnnouncementMessage("방장이 게임 모드를 " + mode + "로 설정했습니다.");
                    });
                } else if (line.startsWith("CHAT ")) {
                    String message = line.substring("CHAT ".length());
                    Platform.runLater(() -> lobby.addIncomingMessage(message));
                } else if ("START".equals(line)) {
                    Platform.runLater(lobby::clearMessages);
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            // 호스트가 닫히면 읽기를 끝낸다.
        }
    }

    private record HostAddress(String host, int port) {
        private static HostAddress parse(String text) {
            String trimmed = text == null ? "" : text.trim();
            int colon = trimmed.lastIndexOf(':');
            if (colon > 0 && trimmed.indexOf(':') == colon) {
                String host = trimmed.substring(0, colon).trim();
                int port = Integer.parseInt(trimmed.substring(colon + 1).trim());
                return new HostAddress(host, port);
            }
            return new HostAddress(trimmed, OnlineLobbyServer.DEFAULT_PORT);
        }
    }
}

package tetris.onlinemulti;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// 온라인 클라이언트에 다시 보여줄 최근 접속 주소를 저장한다.
public final class RecentHostStore {
    private final Path file;

    public RecentHostStore() {
        this(Path.of(System.getProperty("user.home"), ".se_tetris", "recent-host.txt"));
    }

    public RecentHostStore(Path file) {
        this.file = file;
    }

    public String load() {
        try {
            if (file == null || !Files.exists(file)) {
                return "";
            }
            return Files.readString(file).trim();
        } catch (IOException exception) {
            return "";
        }
    }

    public void save(String host) {
        if (file == null || host == null || host.isBlank()) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, host.trim());
        } catch (IOException exception) {
            // 주소를 기억하지 못해도 이번 접속은 유지한다.
        }
    }
}

package tetris.settings;

import java.util.Collections; // 읽기 전용 설정 복사본 생성
import java.util.EnumMap; // 동작 enum을 키로 하는 설정 맵 생성
import java.util.List; // 동작별 키 목록 표현
import java.util.Map; // 동작별 키 설정 표현

import javafx.scene.input.KeyCode; // 저장된 키 입력 종류 표현
import tetris.settings.KeyBindingSettings.ActionType; // 키 설정 동작 종류

// 저장 시점에 게임으로 넘기는 설정 스냅샷
public final class GameSettings {

    // 색상 모드를 블록 색상 모드 번호와 연결
    public enum ColorBlindMode {
        NORMAL(0, "일반 모드"),
        RED_GREEN(1, "적록색맹 모드"),
        BLUE_YELLOW(2, "청황색맹 모드");

        private final int colorMode; // 게임에서 사용할 색상 모드 번호
        private final String displayName; // 설정 화면에 보여줄 이름

        ColorBlindMode(int colorMode, String displayName) { // 모드의 번호와 표시명 저장
            this.colorMode = colorMode; // 색상 모드 번호 저장
            this.displayName = displayName; // 화면 표시명 저장
        }

        // 어느 색맹 모드인지를 구분해주는 int값 도출
        public int getColorMode() {
            return colorMode;
        }

        // 재정의를 통해 읽기 힘든 주소값을 실제 저장된 값을 호출
        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum Difficulty {
        EASY("쉬움"),
        NORMAL("보통"),
        HARD("어려움");

        private final String displayName;

        Difficulty(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum GameMode {
        NORMAL("일반 모드"),
        ITEM("아이템 모드"),
        TIMELIMIT("시간제한 모드");

        private final String displayName;

        GameMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum PlayMode {
        SOLO("SoloPlay"),
        LOCAL("LocalMultiPlay"),
        ONLINE("OnlineMultiPlay");

        private final String displayName;

        PlayMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final String savedResolutionPreset;
    private final int screenWidth;
    private final int screenHeight;
    private final int colorMode;
    private final Map<ActionType, List<KeyCode>> keyBindings;
    private final List<Map<ActionType, List<KeyCode>>> playerKeyBindings;
    private final Difficulty savedDifficulty;
    private final GameMode savedGameMode;

    // 저장 버튼을 누른 시점의 설정을 게임 로직에 전달하는 불변 스냅샷
    public GameSettings(
            String resolutionPreset,
            int screenWidth,
            int screenHeight,
            int colorMode,
            Map<ActionType, List<KeyCode>> keyBindings,
            Difficulty savedDifficulty,
            GameMode savedGameMode) {
        this(resolutionPreset, screenWidth, screenHeight, colorMode, keyBindings,
                savedDifficulty, savedGameMode, null);
    }

    public GameSettings(
            String resolutionPreset,
            int screenWidth,
            int screenHeight,
            int colorMode,
            Map<ActionType, List<KeyCode>> keyBindings,
            Difficulty savedDifficulty,
            GameMode savedGameMode,
            List<Map<ActionType, List<KeyCode>>> playerKeyBindings) {
        this.savedResolutionPreset = resolutionPreset;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.colorMode = colorMode;
        this.savedDifficulty = savedDifficulty;
        this.savedGameMode = savedGameMode == null ? GameMode.NORMAL : savedGameMode;
        this.keyBindings = copyBindings(keyBindings);
        if (playerKeyBindings == null || playerKeyBindings.isEmpty()) {
            this.playerKeyBindings = List.of(this.keyBindings);
        } else {
            this.playerKeyBindings = playerKeyBindings.stream().map(this::copyBindings).toList();
        }
    }

    private Map<ActionType, List<KeyCode>> copyBindings(Map<ActionType, List<KeyCode>> source) {
        EnumMap<ActionType, List<KeyCode>> copiedBindings = new EnumMap<>(ActionType.class);
        source.forEach((action, keys) -> copiedBindings.put(action, List.copyOf(keys)));
        return Collections.unmodifiableMap(copiedBindings);
    }

    public Difficulty difficulty() {
        return savedDifficulty;
    }

    // 저장된 설정은 유지하고 난이도만 바꾼 스냅샷을 만든다.
    public GameSettings withDifficulty(Difficulty difficulty) {
        return new GameSettings(
                savedResolutionPreset,
                screenWidth,
                screenHeight,
                colorMode,
                keyBindings,
                difficulty == null ? savedDifficulty : difficulty,
                savedGameMode,
                playerKeyBindings);
    }

    public GameMode gameMode() {
        return savedGameMode;
    }

    // 해상도 등 현재 설정은 유지하고 로컬 플레이어 키만 바꾼 스냅샷을 만든다.
    public GameSettings withPlayerKeyBindings(List<Map<ActionType, List<KeyCode>>> players) {
        Map<ActionType, List<KeyCode>> firstPlayer = players.get(0);
        return new GameSettings(
                savedResolutionPreset,
                screenWidth,
                screenHeight,
                colorMode,
                firstPlayer,
                savedDifficulty,
                savedGameMode,
                players);
    }

    public String resolutionPreset() {
        return savedResolutionPreset;
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public int colorMode() {
        return colorMode;
    }

    public Map<ActionType, List<KeyCode>> keyBindings() {
        return keyBindings;
    }

    // 로컬 모드는 플레이어 수만큼, 그 외에는 현재 키 설정 하나다.
    public List<Map<ActionType, List<KeyCode>>> playerKeyBindings() {
        return playerKeyBindings;
    }
}

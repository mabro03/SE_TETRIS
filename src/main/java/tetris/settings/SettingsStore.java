package tetris.settings;

import java.io.IOException; // 파일 입출력 예외
import java.io.InputStream; // 설정 파일 입력 스트림
import java.io.OutputStream; // 설정 파일 출력 스트림
import java.nio.file.Files; // 파일 생성과 읽기/쓰기
import java.nio.file.Path; // 설정 파일 경로
import java.util.ArrayList;
import java.util.Arrays; // 저장된 키 문자열 분리
import java.util.EnumMap;
import java.util.List; // 키 목록
import java.util.Map;
import java.util.Objects;
import java.util.Properties; // key=value 형식 저장소
import java.util.stream.Collectors; // 키 목록을 문자열로 변환

import javafx.scene.input.KeyCode; // 키 이름 변환
import tetris.settings.KeyBindingSettings.ActionType; // 역할 목록
import tetris.settings.GameSettings.ColorBlindMode;
import tetris.settings.GameSettings.Difficulty;
import tetris.settings.GameSettings.GameMode;
import tetris.settings.GameSettings.PlayMode;

// 사용자 설정을 홈 디렉터리의 properties 파일에 저장하고 불러오는 클래스
public final class SettingsStore {
    // 이 PC의 논리 화면 1536x864(작업 영역 1536x816) 안에 들어가는 16:9 크기
    public final String[] resolutionPresets = {
        "960x540",
        "1120x630",
        "1200x675"
    };

    // 각 해상도 문자열에 대응되는 실제 픽셀 값
    public final int[][] resolutionValues = {
        {960, 540},
        {1120, 630},
        {1200, 675}
    };

    private final Path settingsFile;
    private final KeyBindingSettings inputSettingData = new KeyBindingSettings();
    private final KeyBindingSettings localCommonKeys = new KeyBindingSettings();
    private final List<KeyBindingSettings> localPlayerKeys = createDefaultLocalPlayers();
    private ColorBlindMode colorBlindMode = ColorBlindMode.NORMAL;
    private String resolutionPreset = resolutionPresets[0];
    private Difficulty currentDifficulty = Difficulty.NORMAL;
    private GameMode currentGameMode = GameMode.NORMAL;
    private GameSettings gameSettings; // 마지막으로 저장되거나 불러온 게임 설정 스냅샷

    public SettingsStore() {
        this(Path.of(System.getProperty("user.home"), ".se_tetris", "settings.properties"));
    }

    // 테스트가 사용자 설정 파일 대신 임시 파일을 쓰도록 경로를 받는다.
    public SettingsStore(Path settingsFile) {
        this.settingsFile = Objects.requireNonNull(settingsFile);
    }

    // 현재 키 설정 객체 반환. 설정 화면과 저장소가 같은 객체를 사용한다.
    public KeyBindingSettings getInputSettingData() {
        return inputSettingData;
    }

    public KeyBindingSettings localCommonKeys() {
        return localCommonKeys;
    }

    public List<KeyBindingSettings> localPlayerKeys() {
        return List.copyOf(localPlayerKeys);
    }

    private static List<KeyBindingSettings> createDefaultLocalPlayers() {
        List<KeyBindingSettings> players = new ArrayList<>();
        players.add(KeyBindingSettings.player1LocalDefaults());
        players.add(KeyBindingSettings.player2LocalDefaults());
        return players;
    }

    // 캡슐화를 통해 수정은 불가하되 읽기만 가능
    public ColorBlindMode getColorBlindMode() {
        return colorBlindMode;
    }

    // 혹시라도 잘못된 값이 들어오면 기존의 설정을 유지
    public void setColorBlindMode(ColorBlindMode mode) {
        if (mode != null) {
            colorBlindMode = mode;
        }
    }

    // 현재 해상도 이름 반환
    public String getResolutionPreset() {
        return resolutionPreset;
    }

    // 지원되는 해상도만 저장
    public void setResolutionPreset(String preset) {
        for (String supportedPreset : resolutionPresets) {
            if (supportedPreset.equals(preset)) {
                resolutionPreset = preset;
                return;
            }
        }
    }

    // 현재 화면의 가로 크기 반환
    public int getScreenWidth() {
        return getResolutionValues()[0];
    }

    // 현재 화면의 세로 크기 반환
    public int getScreenHeight() {
        return getResolutionValues()[1];
    }

    private int[] getResolutionValues() {
        for (int index = 0; index < resolutionPresets.length; index++) {
            if (resolutionPresets[index].equals(resolutionPreset)) {
                return resolutionValues[index];
            }
        }
        return resolutionValues[0];
    }

    public Difficulty getDifficulty() {
        return currentDifficulty;
    }

    public void setDifficulty(Difficulty value) {
        if (value != null) {
            currentDifficulty = value;
        }
    }

    public GameMode getGameMode() {
        return currentGameMode;
    }

    public void setGameMode(GameMode value) {
        if (value != null) {
            currentGameMode = value;
        }
    }

    // 저장된 설정을 읽어 현재 설정에 적용한다.
    public void load() {
        setGameMode(GameMode.NORMAL);
        Properties properties = readSettingsFile();
        if (properties == null) {
            gameSettings = createGameSettings();
            return;
        }
        applySavedSettings(properties);
        gameSettings = createGameSettings();
    }

    private Properties readSettingsFile() {
        if (!Files.exists(settingsFile)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsFile)) {
            properties.load(input);
            return properties;
        } catch (IOException e) {
            System.err.println("설정 파일을 읽지 못했습니다: " + e.getMessage());
            return null;
        }
    }

    private void applySavedSettings(Properties properties) {
        applyResolution(properties);
        applyColorMode(properties);
        applyDifficulty(properties);
        applyGameMode(properties);
        applyKeyBindings(properties);
        applyLocalKeyBindings(properties);
    }

    private void applyResolution(Properties properties) {
        setResolutionPreset(properties.getProperty("resolution", getResolutionPreset()));
    }

    private void applyColorMode(Properties properties) {
        String colorMode = properties.getProperty("colorBlindMode");
        if (colorMode == null) {
            return;
        }
        try {
            setColorBlindMode(ColorBlindMode.valueOf(colorMode));
        } catch (IllegalArgumentException e) {
            System.err.println("저장된 색상 모드를 읽을 수 없습니다: " + colorMode);
        }
    }

    private void applyDifficulty(Properties properties) {
        String difficulty = properties.getProperty("difficulty");
        if (difficulty == null) {
            return;
        }
        try {
            setDifficulty(Difficulty.valueOf(difficulty));
        } catch (IllegalArgumentException e) {
            System.err.println("저장된 난이도를 읽을 수 없습니다: " + difficulty);
        }
    }

    private void applyGameMode(Properties properties) {
        String gameMode = properties.getProperty("gameMode", GameMode.NORMAL.name());
        try {
            setGameMode(GameMode.valueOf(gameMode));
        } catch (IllegalArgumentException e) {
            System.err.println("저장된 게임 모드를 읽을 수 없습니다: " + gameMode);
        }
    }

    private void applyKeyBindings(Properties properties) {
        for (ActionType actionType : ActionType.values()) {
            applyKeyBinding(actionType, properties.getProperty("key." + actionType.name()));
        }
    }

    private void applyKeyBinding(ActionType actionType, String savedKeys) {
        if (savedKeys == null || savedKeys.isBlank()) {
            return;
        }
        inputSettingData.setKeyCodes(actionType, parseSavedKeys(savedKeys));
    }

    private void applyLocalKeyBindings(Properties properties) {
        for (ActionType actionType : ActionType.values()) {
            if (actionType.isCommon()) {
                applyStoredKeys(localCommonKeys, actionType,
                        properties.getProperty("local.common." + actionType.name()));
            }
        }
        for (int index = 0; index < localPlayerKeys.size(); index++) {
            KeyBindingSettings playerKeys = localPlayerKeys.get(index);
            for (ActionType actionType : ActionType.values()) {
                if (actionType.isCommon()) {
                    continue;
                }
                applyStoredKeys(playerKeys, actionType, properties.getProperty(
                        "local.player." + (index + 1) + "." + actionType.name()));
            }
        }
    }

    private void applyStoredKeys(KeyBindingSettings bindings, ActionType actionType, String savedKeys) {
        if (savedKeys == null || savedKeys.isBlank()) {
            return;
        }
        bindings.setKeyCodes(actionType, parseSavedKeys(savedKeys));
    }

    private List<KeyCode> parseSavedKeys(String savedKeys) {
        return Arrays.stream(savedKeys.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(this::parseKeyCode)
                .filter(keyCode -> keyCode != null)
                .collect(Collectors.toList());
    }

    // 현재 설정을 파일에 저장하고 게임 설정 객체를 반환한다.
    public GameSettings save() {
        if (!writeSettingsFile(collectCurrentSettings())) {
            return null;
        }
        gameSettings = createGameSettings();
        return gameSettings;
    }

    private Properties collectCurrentSettings() {
        Properties properties = new Properties();
        properties.setProperty("resolution", getResolutionPreset());
        properties.setProperty("colorBlindMode", getColorBlindMode().name());
        properties.setProperty("difficulty", getDifficulty().name());
        properties.setProperty("gameMode", getGameMode().name());
        addKeyBindings(properties);
        addLocalKeyBindings(properties);
        return properties;
    }

    private void addKeyBindings(Properties properties) {
        inputSettingData.getAllBindings().forEach((actionType, keyCodes) -> {
            properties.setProperty("key." + actionType.name(), formatKeyCodes(keyCodes));
        });
    }

    private void addLocalKeyBindings(Properties properties) {
        localCommonKeys.getAllBindings().forEach((actionType, keyCodes) -> {
            if (actionType.isCommon()) {
                properties.setProperty("local.common." + actionType.name(), formatKeyCodes(keyCodes));
            }
        });
        for (int index = 0; index < localPlayerKeys.size(); index++) {
            int playerNumber = index + 1;
            localPlayerKeys.get(index).getAllBindings().forEach((actionType, keyCodes) -> {
                if (!actionType.isCommon()) {
                    properties.setProperty(
                            "local.player." + playerNumber + "." + actionType.name(),
                            formatKeyCodes(keyCodes));
                }
            });
        }
    }

    private String formatKeyCodes(List<KeyCode> keyCodes) {
        return keyCodes.stream()
                .map(KeyCode::name)
                .collect(Collectors.joining(","));
    }

    private boolean writeSettingsFile(Properties properties) {
        try {
            Files.createDirectories(settingsFile.getParent());
            try (OutputStream output = Files.newOutputStream(settingsFile)) {
                properties.store(output, "Tetris user settings");
            }
            return true;
        } catch (IOException e) {
            System.err.println("설정을 저장하지 못했습니다: " + e.getMessage());
            return false;
        }
    }

    public GameSettings saveGameMode(GameMode gameMode) {
        setGameMode(gameMode);
        GameSettings savedSettings = save();
        return savedSettings == null ? createGameSettings() : savedSettings;
    }

    public GameSettings resetToDefaults() {
        setResolutionPreset(resolutionPresets[0]);
        setColorBlindMode(ColorBlindMode.NORMAL);
        setDifficulty(Difficulty.NORMAL);
        setGameMode(GameMode.NORMAL);
        inputSettingData.resetToDefaults();
        resetLocalKeyBindings();
        return save();
    }

    public void resetDisplaySettings() {
        setResolutionPreset(resolutionPresets[0]);
        setColorBlindMode(ColorBlindMode.NORMAL);
        setDifficulty(Difficulty.NORMAL);
    }

    public void resetSoloKeyBindings() {
        inputSettingData.resetToDefaults();
    }

    public void resetLocalKeyBindings() {
        localCommonKeys.resetToDefaults();
        localPlayerKeys.get(0).copyFrom(KeyBindingSettings.player1LocalDefaults());
        localPlayerKeys.get(1).copyFrom(KeyBindingSettings.player2LocalDefaults());
    }

    // 솔로와 온라인은 현재 설정 스냅샷을, 로컬은 추가 설정의 키를 담은 스냅샷을 만든다.
    public GameSettings gameSettingsFor(PlayMode playMode) {
        GameSettings currentSettings = getGameSettings();
        if (playMode != PlayMode.LOCAL) {
            return currentSettings;
        }
        return currentSettings.withPlayerKeyBindings(localPlayerBindingMaps());
    }

    private List<Map<ActionType, List<KeyCode>>> localPlayerBindingMaps() {
        List<Map<ActionType, List<KeyCode>>> maps = new ArrayList<>();
        for (KeyBindingSettings playerKeys : localPlayerKeys) {
            maps.add(combinedLocalBindings(playerKeys));
        }
        return maps;
    }

    private Map<ActionType, List<KeyCode>> combinedLocalBindings(KeyBindingSettings playerKeys) {
        Map<ActionType, List<KeyCode>> combined = new EnumMap<>(ActionType.class);
        for (ActionType actionType : ActionType.values()) {
            KeyBindingSettings source = actionType.isCommon() ? localCommonKeys : playerKeys;
            combined.put(actionType, source.getKeyCodes(actionType));
        }
        return combined;
    }

    // 게임 시작 화면에서 마지막 저장 설정 객체를 조회
    public GameSettings getGameSettings() {
        if (gameSettings == null) {
            gameSettings = createGameSettings(); // 아직 로드되지 않은 경우 기본값으로 초기화
        }
        return gameSettings;
    }

    // 현재 설정값을 해상도, 색상 모드, 키 배정이 포함된 객체로 복사
    private GameSettings createGameSettings() {
        return new GameSettings(
                getResolutionPreset(),
                getScreenWidth(),
                getScreenHeight(),
                getColorBlindMode().getColorMode(),
                inputSettingData.getAllBindings(),
                getDifficulty(),
                getGameMode());
    }

    private KeyCode parseKeyCode(String value) { // 문자열을 JavaFX 키 코드로 변환
        try {
            return KeyCode.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

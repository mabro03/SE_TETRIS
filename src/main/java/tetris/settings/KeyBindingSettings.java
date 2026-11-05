package tetris.settings;

import java.util.Collections; // 수정 불가능한 결과 제공
import java.util.EnumMap; // enum을 키로 사용하는 맵
import java.util.LinkedHashMap; // 입력 순서를 유지하는 맵
import java.util.List; // 역할별 키 목록
import java.util.Map; // 키 배정 자료 구조

import javafx.scene.input.KeyCode; // JavaFX 키 코드

// 설정 화면에서 키 역할과 배정된 기본/보조 키를 관리
public final class KeyBindingSettings {

    // 게임에서 수행할 수 있는 동작 목록
    public enum ActionType {
        MOVE_LEFT("왼쪽 이동"),
        MOVE_RIGHT("오른쪽 이동"),
        SOFT_DROP("소프트 드롭"),
        HARD_DROP("하드 드롭"),
        ROTATE_CLOCK("시계 방향 회전"),
        ROTATE_COUNTERCLOCK("반시계 방향 회전"),
        HOLD("홀드"),
        PAUSE("일시정지");

        private final String displayName; // 설정 화면에 표시할 역할 이름

        // 역할 이름을 저장
        ActionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        // 플레이어가 늘어도 한 번만 있는 키. 나머지는 플레이어마다 추가된다.
        public boolean isCommon() {
            return this == PAUSE;
        }

        public static List<ActionType> valuesList() {
            return List.of(values());
        }
    }

    // 역할별 키 저장
    private final Map<ActionType, List<KeyCode>> bindings = new EnumMap<>(ActionType.class);

    // 기본 키 배정을 생성
    public KeyBindingSettings() {
        bindings.put(ActionType.MOVE_LEFT, List.of(KeyCode.LEFT));
        bindings.put(ActionType.MOVE_RIGHT, List.of(KeyCode.RIGHT));
        bindings.put(ActionType.SOFT_DROP, List.of(KeyCode.DOWN));
        bindings.put(ActionType.HARD_DROP, List.of(KeyCode.SPACE));
        bindings.put(ActionType.ROTATE_CLOCK, List.of(KeyCode.X, KeyCode.UP));
        bindings.put(ActionType.ROTATE_COUNTERCLOCK, List.of(KeyCode.Z, KeyCode.CONTROL));
        bindings.put(ActionType.HOLD, List.of(KeyCode.SHIFT, KeyCode.C));
        bindings.put(ActionType.PAUSE, List.of(KeyCode.ESCAPE));
    }

    // 플레이어1의 키 배정 회전과 홀드는 JKL, 하드 드롭은 마침표
    public static KeyBindingSettings player1LocalDefaults() {
        KeyBindingSettings settings = new KeyBindingSettings();
        settings.setKeyCode(ActionType.MOVE_LEFT, KeyCode.LEFT);
        settings.setKeyCode(ActionType.MOVE_RIGHT, KeyCode.RIGHT);
        settings.setKeyCode(ActionType.SOFT_DROP, KeyCode.DOWN);
        settings.setKeyCode(ActionType.HARD_DROP, KeyCode.PERIOD);
        settings.setKeyCode(ActionType.ROTATE_COUNTERCLOCK, KeyCode.J);
        settings.setKeyCode(ActionType.ROTATE_CLOCK, KeyCode.K);
        settings.setKeyCode(ActionType.HOLD, KeyCode.L);
        return settings;
    }

    // 플레이어2의 키 배정 회전과 홀드는 CVB, 하드 드롭은 스페이스
    public static KeyBindingSettings player2LocalDefaults() {
        KeyBindingSettings settings = new KeyBindingSettings();
        settings.setKeyCode(ActionType.MOVE_LEFT, KeyCode.A);
        settings.setKeyCode(ActionType.MOVE_RIGHT, KeyCode.D);
        settings.setKeyCode(ActionType.SOFT_DROP, KeyCode.S);
        settings.setKeyCode(ActionType.HARD_DROP, KeyCode.SPACE);
        settings.setKeyCode(ActionType.ROTATE_COUNTERCLOCK, KeyCode.C);
        settings.setKeyCode(ActionType.ROTATE_CLOCK, KeyCode.V);
        settings.setKeyCode(ActionType.HOLD, KeyCode.B);
        return settings;
    }

    // 동작 순서와 같은 길이의 키 배열로 각 역할의 대표 키를 지정
    public KeyBindingSettings(KeyCode[] keyCodes) {
        this();
        ActionType[] actionTypes = ActionType.values();
        if (keyCodes == null || keyCodes.length != actionTypes.length) {
            throw new IllegalArgumentException("keyCodes must contain one value for each ActionType");
        }
        for (int index = 0; index < actionTypes.length; index++) {
            setKeyCode(actionTypes[index], keyCodes[index]);
        }
    }

    // 역할에 배정된 첫 키 반환
    public KeyCode getKeyCode(ActionType actionType) {
        List<KeyCode> keyCodes = getKeyCodes(actionType);
        if (keyCodes.isEmpty()) {
            return null;
        }
        return keyCodes.get(0);
    }

    // 특정 역할의 키 목록 반환. 없으면 빈 목록을 반환한다.
    public List<KeyCode> getKeyCodes(ActionType actionType) {
        return bindings.getOrDefault(actionType, List.of());
    }

    // 전체 키 배정의 읽기 전용 복사본 반환
    public Map<ActionType, List<KeyCode>> getAllBindings() {
        Map<ActionType, List<KeyCode>> copy = new LinkedHashMap<>();
        bindings.forEach((actionType, keyCodes) -> copy.put(actionType, List.copyOf(keyCodes)));
        return Collections.unmodifiableMap(copy);
    }

    // 특정 역할의 키를 하나로 변경
    public void setKeyCode(ActionType actionType, KeyCode keyCode) {
        if (actionType != null && keyCode != null && bindings.containsKey(actionType)) {
            bindings.put(actionType, List.of(keyCode));
        }
    }

    // 특정 역할의 키 목록을 변경
    public void setKeyCodes(ActionType actionType, List<KeyCode> keyCodes) {
        if (actionType != null && keyCodes != null && !keyCodes.isEmpty()
                && keyCodes.stream().noneMatch(keyCode -> keyCode == null)) {
            bindings.put(actionType, List.copyOf(keyCodes));
        }
    }

    // 기본 키 배정으로 설정 초기화
    public void resetToDefaults() {
        copyFrom(new KeyBindingSettings());
    }

    // 키 배정을 다른 설정에서 복사
    public void copyFrom(KeyBindingSettings source) {
        for (ActionType actionType : ActionType.values()) {
            setKeyCodes(actionType, source.getKeyCodes(actionType));
        }
    }
}

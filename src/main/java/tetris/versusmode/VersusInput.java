package tetris.versusmode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javafx.scene.input.KeyCode;
import tetris.gamescene.PlayerInput;
import tetris.settings.KeyBindingSettings.ActionType;

public class VersusInput {
    private static final double INITIAL_DELAY_MS = 200; 
    private static final double REPEAT_INTERVAL_MS = 50;

    private final Runnable onPauseRequested;
    private final List<PlayerInput> players;
    private final Map<KeyCode, Binding> keyBindings;
    private final KeyCode[] currentKeys = new KeyCode[2]; // 0은 플레이어 1이 마지막에 누르고 있는 키, 1은 플레이어 2가 마지막에 누르고 있는 키
    private final double[] remainingMs = new double[2]; // 위의 각 키코드에 따른 액션을 호출하기 위해 남은 시간 간격
    // OS가 보내는 반복 누름 이벤트만 걸러낸다. 이전 키로 복귀하지 않는다.
    private final Set<KeyCode> pressedKeys = new HashSet<>(); // 눌린 키들을 저장해서 handleKeyPressed가 호출될 때마다 currentkeys, remainingMs가 초기화되는 걸 막는다.
    private boolean inputEnabled = true;
    // 컴퓨터가 조작하는 플레이어 인덱스. -1이면 둘 다 사람.
    private final int computerPlayerIndex;

    private record Binding(int playerIndex, ActionType action) {}

    public VersusInput(Runnable onPauseRequested, PlayerInput player1, PlayerInput player2,
            List<Map<ActionType, List<KeyCode>>> playerKeyBindings) {
        this(onPauseRequested, player1, player2, playerKeyBindings, -1);
    }

    public VersusInput(Runnable onPauseRequested, PlayerInput player1, PlayerInput player2,
            List<Map<ActionType, List<KeyCode>>> playerKeyBindings, int computerPlayerIndex) {
        this.onPauseRequested = Objects.requireNonNull(onPauseRequested);
        players = List.of(player1, player2);
        keyBindings = CreateKeyBindings(playerKeyBindings);
        if (computerPlayerIndex < -1 || computerPlayerIndex >= players.size()) {
            throw new IllegalArgumentException("computerPlayerIndex out of range");
        }
        this.computerPlayerIndex = computerPlayerIndex;
    }

    private Map<KeyCode, Binding> CreateKeyBindings(
            List<Map<ActionType, List<KeyCode>>> playerKeyBindings) {
        Objects.requireNonNull(playerKeyBindings);
        if (playerKeyBindings.size() != 2) {
            throw new IllegalArgumentException("두 플레이어의 키 설정이 필요합니다.");
        }
        Map<KeyCode, Binding> keyBindings = new HashMap<>();
        for (int player = 0; player < players.size(); player++) {
            for (var entry : playerKeyBindings.get(player).entrySet()) {
                ActionType action = Objects.requireNonNull(entry.getKey());
                Binding binding = new Binding(action == ActionType.PAUSE ? -1 : player, action); // 퍼즈면 -1, PAUSE 바인딩을 생성한다.
                for (KeyCode key : entry.getValue()) {
                    Objects.requireNonNull(key);
                    Binding previous = keyBindings.putIfAbsent(key, binding); // 키, 바인딩 형태로 맵에 삽입, previous에는 넣은 바인딩에 들어감.
                    if (previous != null && !previous.equals(binding)) { // 만약 같은 키가 있는 경우나 previous가 널인 경우(50번 줄에서 같은 키가 있어서 previous에 널이 들어간 경우)
                        throw new IllegalArgumentException("키 배정이 중복됩니다: " + key);
                    }
                }
            }
        }
        return Map.copyOf(keyBindings);
    }

    public void HandleKeyPressed(KeyCode key) {
        if (!inputEnabled || key == null) return;
        Binding binding = keyBindings.get(key);
        if (binding == null || !pressedKeys.add(key)) return;// 만약 누른 키가 눌린 키 셋에 있는 거라면 해당 입력은 처낸다.
        if (binding.action() == ActionType.PAUSE) {
            onPauseRequested.run();
        } else if (binding.playerIndex() == computerPlayerIndex) {
            // 컴퓨터 플레이어 키는 무시한다.
        } else {
            currentKeys[binding.playerIndex()] = key;
            remainingMs[binding.playerIndex()] = INITIAL_DELAY_MS;
            players.get(binding.playerIndex()).HandleAction(binding.action());
        }
    }

    public void HandleKeyReleased(KeyCode key) { // 누르고 있는 키가 떼질때 작용
        if (key == null) return;
        pressedKeys.remove(key);
        Binding binding = keyBindings.get(key);
        if (binding == null || binding.action() == ActionType.PAUSE) return;
        int player = binding.playerIndex();
        if (currentKeys[player] == key) {
            currentKeys[player] = null;
            remainingMs[player] = 0;
        }
    }

    // 관리자가 매 프레임 경과 시간을 밀리초 단위로 전달한다.
    public void Update(double deltaTimeMs) {
        if (!inputEnabled || deltaTimeMs < 0) return;

        for (int player = 0; player < players.size(); player++) {
            if (player == computerPlayerIndex) {
                continue;
            }
            KeyCode key = currentKeys[player];
            if (key == null) continue;
            ActionType action = keyBindings.get(key).action();
            if (action != ActionType.MOVE_LEFT && action != ActionType.MOVE_RIGHT
                    && action != ActionType.SOFT_DROP) continue;
            remainingMs[player] -= deltaTimeMs;
            if (remainingMs[player] <= 0) {
                // 지연된 프레임에서 여러 칸을 한꺼번에 이동하지 않는다.
                remainingMs[player] = REPEAT_INTERVAL_MS;
                players.get(player).HandleAction(action);
            }
        }
    }

    public void SetInputEnabled(boolean enabled) {
        inputEnabled = enabled;
        if (!enabled) {
            Arrays.fill(currentKeys, null);
            Arrays.fill(remainingMs, 0);
            pressedKeys.clear();
        }
    }
}

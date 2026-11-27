package tetris.versusmode.computermode;

import java.util.Objects;
import java.util.function.Supplier;

import tetris.block.data.BlockData;
import tetris.gamescene.PlayerInput;
import tetris.gamescene.board.BoardElement;
import tetris.gamescene.scenerender.SceneRenderState;
import tetris.settings.KeyBindingSettings.ActionType;

// 0.5~1초 간격으로 키 결정을 호출해 사람과 비슷하게 한 수씩 입력한다.
public final class ComputerPlayer {
    public static final double DEFAULT_INTERVAL_MS = 750.0;
    public static final double MIN_INTERVAL_MS = 500.0;
    public static final double MAX_INTERVAL_MS = 1_000.0;

    private final ComputerKeyDecider keyDecider;
    private final Supplier<SceneRenderState> stateSupplier;
    private final PlayerInput playerInput;
    private final double intervalMs;
    private double elapsedMs;
    private boolean enabled = true;

    public ComputerPlayer(
            Supplier<SceneRenderState> stateSupplier,
            PlayerInput playerInput) {
        this(stateSupplier, playerInput, DEFAULT_INTERVAL_MS, new ComputerKeyDecider());
    }

    public ComputerPlayer(
            Supplier<SceneRenderState> stateSupplier,
            PlayerInput playerInput,
            double intervalMs,
            ComputerKeyDecider keyDecider) {
        this.stateSupplier = Objects.requireNonNull(stateSupplier, "stateSupplier cannot be null");
        this.playerInput = Objects.requireNonNull(playerInput, "playerInput cannot be null");
        this.keyDecider = Objects.requireNonNull(keyDecider, "keyDecider cannot be null");
        if (intervalMs < MIN_INTERVAL_MS || intervalMs > MAX_INTERVAL_MS) {
            throw new IllegalArgumentException("intervalMs must be between 500 and 1000");
        }
        this.intervalMs = intervalMs;
    }

    public void SetEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            elapsedMs = 0;
        }
    }

    public void ResetTimer() {
        elapsedMs = 0;
    }

    public void Update(double deltaTimeMs) {
        if (!enabled || deltaTimeMs < 0) {
            return;
        }
        elapsedMs += deltaTimeMs;
        if (elapsedMs < intervalMs) {
            return;
        }
        elapsedMs = 0;

        SceneRenderState state = stateSupplier.get();
        if (state == null) {
            return;
        }
        BlockData current = state.currentBlock();
        BoardElement[][] board = state.board();
        if (current == null || board == null) {
            return;
        }

        ActionType action = keyDecider.nextAction(board, current);
        if (action != null) {
            playerInput.HandleAction(action);
        }
    }
}

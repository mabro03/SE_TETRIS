package tetris.versusmode.computermode;

import java.util.Objects;

import tetris.block.data.BlockData;
import tetris.gamescene.board.BoardElement;
import tetris.settings.KeyBindingSettings.ActionType;

// 교수님 조건: 보드 상태를 입력으로 받아 다음에 눌러야 할 키(ActionType)를 반환한다.
public final class ComputerKeyDecider {
    private final ComputerBrain brain;

    public ComputerKeyDecider() {
        this(new ComputerBrain());
    }

    public ComputerKeyDecider(ComputerBrain brain) {
        this.brain = Objects.requireNonNull(brain, "brain cannot be null");
    }

    public ActionType nextAction(BoardElement[][] board, BlockData currentBlock) {
        Objects.requireNonNull(board, "board cannot be null");
        Objects.requireNonNull(currentBlock, "currentBlock cannot be null");

        Placement placement = brain.choosePlacement(board, currentBlock);
        if (placement == null) {
            return ActionType.SOFT_DROP;
        }

        if (placement.clockwiseRotations() > 0) {
            return ActionType.ROTATE_CLOCK;
        }
        int x = currentBlock.GetX();
        if (x < placement.targetX()) {
            return ActionType.MOVE_RIGHT;
        }
        if (x > placement.targetX()) {
            return ActionType.MOVE_LEFT;
        }
        return ActionType.HARD_DROP;
    }
}

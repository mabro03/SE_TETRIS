package tetris.versusmode.computermode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import tetris.block.data.BlockData;
import tetris.block.data.ColorType;
import tetris.block.data.ShapePalette;
import tetris.gamescene.board.BoardElement;
import tetris.gamescene.board.GameBoard;
import tetris.settings.KeyBindingSettings.ActionType;

class ComputerKeyDeciderTest {
    @Test
    void returnsActionForEmptyBoard() {
        BoardElement[][] board = new GameBoard(0).GetBoard();
        BlockData current = new BlockData(new ShapePalette().GetShapeT(), ColorType.BlockT, 0);
        ActionType action = new ComputerKeyDecider().nextAction(board, current);
        assertNotNull(action);
        assertTrue(action == ActionType.ROTATE_CLOCK
                || action == ActionType.MOVE_LEFT
                || action == ActionType.MOVE_RIGHT
                || action == ActionType.HARD_DROP
                || action == ActionType.SOFT_DROP);
    }

    @Test
    void brainChoosesPlacementOnEmptyBoard() {
        BoardElement[][] board = new GameBoard(0).GetBoard();
        BlockData current = new BlockData(new ShapePalette().GetShapeO(), ColorType.BlockO, 0);
        Placement placement = new ComputerBrain().choosePlacement(board, current);
        assertNotNull(placement);
        assertEquals(0, placement.clockwiseRotations());
        assertTrue(placement.targetX() >= 0 && placement.targetX() < GameBoard.boardCol);
    }
}

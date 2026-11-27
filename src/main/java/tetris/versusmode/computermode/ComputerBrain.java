package tetris.versusmode.computermode;

import java.util.Objects;

import tetris.block.data.BlockData;
import tetris.gamescene.board.BoardElement;
import tetris.gamescene.board.GameBoard;

// 현재 보드와 활성 블록으로 최적 배치(x, 회전 수)를 고른다.
public final class ComputerBrain {
    private final PlacementEvaluator evaluator;

    public ComputerBrain() {
        this(ComputerWeights.preset());
    }

    public ComputerBrain(ComputerWeights weights) {
        evaluator = new PlacementEvaluator(Objects.requireNonNull(weights, "weights cannot be null"));
    }

    public Placement choosePlacement(BoardElement[][] board, BlockData currentBlock) {
        Objects.requireNonNull(board, "board cannot be null");
        Objects.requireNonNull(currentBlock, "currentBlock cannot be null");
        boolean[][] occupied = PlacementEvaluator.boardToOccupied(board);
        boolean[][] baseShape = PlacementEvaluator.copyShape(currentBlock.GetShape());

        Placement best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        boolean[][] shape = baseShape;
        for (int rot = 0; rot < 4; rot++) {
            if (rot > 0 && PlacementEvaluator.shapesEqual(shape, baseShape)) {
                break;
            }
            int minCol = PlacementEvaluator.minFilledCol(shape);
            int maxCol = PlacementEvaluator.maxFilledCol(shape);
            int minX = -minCol;
            int maxX = GameBoard.boardCol - 1 - maxCol;
            for (int x = minX; x <= maxX; x++) {
                double score = evaluator.evaluate(occupied, shape, x);
                if (score > bestScore) {
                    bestScore = score;
                    best = new Placement(x, rot, PlacementEvaluator.copyShape(shape));
                }
            }
            shape = PlacementEvaluator.rotateClockwise(shape);
        }
        return best;
    }
}

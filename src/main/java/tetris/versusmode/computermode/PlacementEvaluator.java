package tetris.versusmode.computermode;

import java.util.Arrays;
import java.util.Objects;

import tetris.gamescene.board.BoardElement;

// 보드 mock에 블록을 떨어뜨린 뒤 ai-ga-tetris 방식의 점수를 계산한다.
final class PlacementEvaluator {
    private final ComputerWeights weights;

    PlacementEvaluator(ComputerWeights weights) {
        this.weights = Objects.requireNonNull(weights, "weights cannot be null");
    }

    double evaluate(boolean[][] occupied, boolean[][] shape, int placeX) {
        Objects.requireNonNull(occupied, "occupied cannot be null");
        Objects.requireNonNull(shape, "shape cannot be null");
        int dropY = findDropY(occupied, shape, placeX);
        if (dropY == Integer.MIN_VALUE) {
            return Double.NEGATIVE_INFINITY;
        }

        boolean[][] mock = copyBoard(occupied);
        int cleared = placeAndClear(mock, shape, placeX, dropY);
        if (cleared < 0) {
            return -99_999_999.0;
        }
        return scoreBoard(mock, shape, placeX, dropY, cleared);
    }

    static int findDropY(boolean[][] occupied, boolean[][] shape, int placeX) {
        int rows = occupied.length;
        int best = Integer.MIN_VALUE;
        for (int y = 0; y < rows; y++) {
            if (fits(occupied, shape, placeX, y)) {
                best = y;
            } else if (best != Integer.MIN_VALUE) {
                break;
            }
        }
        return best;
    }

    static boolean fits(boolean[][] occupied, boolean[][] shape, int x, int y) {
        int rows = occupied.length;
        int cols = occupied[0].length;
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (!shape[r][c]) {
                    continue;
                }
                int boardRow = y + r;
                int boardCol = x + c;
                if (boardCol < 0 || boardCol >= cols || boardRow < 0 || boardRow >= rows) {
                    return false;
                }
                if (occupied[boardRow][boardCol]) {
                    return false;
                }
            }
        }
        return true;
    }

    static int minFilledCol(boolean[][] shape) {
        int min = Integer.MAX_VALUE;
        for (boolean[] row : shape) {
            for (int c = 0; c < row.length; c++) {
                if (row[c]) {
                    min = Math.min(min, c);
                }
            }
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    static int maxFilledCol(boolean[][] shape) {
        int max = Integer.MIN_VALUE;
        for (boolean[] row : shape) {
            for (int c = 0; c < row.length; c++) {
                if (row[c]) {
                    max = Math.max(max, c);
                }
            }
        }
        return max == Integer.MIN_VALUE ? 0 : max;
    }

    static boolean[][] rotateClockwise(boolean[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        boolean[][] rotated = new boolean[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = shape[r][c];
            }
        }
        return rotated;
    }

    static boolean[][] copyShape(boolean[][] shape) {
        boolean[][] copy = new boolean[shape.length][];
        for (int i = 0; i < shape.length; i++) {
            copy[i] = Arrays.copyOf(shape[i], shape[i].length);
        }
        return copy;
    }

    static boolean shapesEqual(boolean[][] a, boolean[][] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (!Arrays.equals(a[i], b[i])) {
                return false;
            }
        }
        return true;
    }

    static boolean[][] boardToOccupied(BoardElement[][] board) {
        Objects.requireNonNull(board, "board cannot be null");
        boolean[][] occupied = new boolean[board.length][board[0].length];
        for (int r = 0; r < board.length; r++) {
            for (int c = 0; c < board[r].length; c++) {
                occupied[r][c] = board[r][c] != null && board[r][c].isBlock();
            }
        }
        return occupied;
    }

    private static boolean[][] copyBoard(boolean[][] occupied) {
        boolean[][] copy = new boolean[occupied.length][];
        for (int i = 0; i < occupied.length; i++) {
            copy[i] = Arrays.copyOf(occupied[i], occupied[i].length);
        }
        return copy;
    }

    // 배치 후 꽉 찬 줄을 지우고 지운 줄 수를 반환한다. 오버플로 시 -1.
    private static int placeAndClear(boolean[][] mock, boolean[][] shape, int x, int y) {
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (!shape[r][c]) {
                    continue;
                }
                int boardRow = y + r;
                int boardCol = x + c;
                if (boardRow < 0) {
                    return -1;
                }
                mock[boardRow][boardCol] = true;
            }
        }

        int cleared = 0;
        int rows = mock.length;
        int cols = mock[0].length;
        boolean found;
        do {
            found = false;
            for (int r = rows - 1; r >= 0; r--) {
                boolean full = true;
                for (int c = 0; c < cols; c++) {
                    if (!mock[r][c]) {
                        full = false;
                        break;
                    }
                }
                if (!full) {
                    continue;
                }
                cleared++;
                found = true;
                for (int move = r; move > 0; move--) {
                    System.arraycopy(mock[move - 1], 0, mock[move], 0, cols);
                }
                Arrays.fill(mock[0], false);
                break;
            }
        } while (found);
        return cleared;
    }

    private double scoreBoard(
            boolean[][] mock, boolean[][] shape, int placeX, int placeY, int cleared) {
        int rows = mock.length;
        int cols = mock[0].length;
        double score = 0.0;

        boolean[][] justPlaced = new boolean[rows][cols];
        for (int r = 0; r < shape.length; r++) {
            for (int c = 0; c < shape[r].length; c++) {
                if (shape[r][c]) {
                    int br = placeY + r;
                    int bc = placeX + c;
                    if (br >= 0 && br < rows && bc >= 0 && bc < cols) {
                        justPlaced[br][bc] = true;
                    }
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!justPlaced[r][c]) {
                    continue;
                }
                if (c == 0 || c == cols - 1) {
                    score += weights.touchingWalls;
                }
                if (r == rows - 1) {
                    score += weights.touchingFloor;
                }
                if (c + 1 < cols && mock[r][c + 1] && !justPlaced[r][c + 1]) {
                    score += weights.touchingEdges;
                }
                if (c > 0 && mock[r][c - 1] && !justPlaced[r][c - 1]) {
                    score += weights.touchingEdges;
                }
                if (r + 1 < rows && mock[r + 1][c] && !justPlaced[r + 1][c]) {
                    score += weights.touchingEdges;
                }
                if (r > 0 && mock[r - 1][c] && !justPlaced[r - 1][c]) {
                    score += weights.touchingEdges;
                }
            }
        }

        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (mock[r][c]) {
                    int curHeight = rows - r;
                    score += curHeight * weights.height;
                }
            }
        }

        for (int c = 0; c < cols; c++) {
            boolean seenBlock = false;
            int holes = 0;
            for (int r = 0; r < rows; r++) {
                if (mock[r][c]) {
                    seenBlock = true;
                } else if (seenBlock) {
                    holes++;
                }
            }
            boolean seenHole = false;
            int blockades = 0;
            for (int r = rows - 1; r >= 0; r--) {
                if (!mock[r][c]) {
                    seenHole = true;
                } else if (seenHole) {
                    blockades++;
                }
            }
            score += weights.holes * holes;
            score += weights.blockade * blockades;
        }

        score += cleared * weights.clear;
        return score;
    }
}

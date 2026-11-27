package tetris.versusmode.computermode;

// zacfermanis/ai-ga-tetris TetrisAI의 기본 가중치를 이식한다.
public final class ComputerWeights {
    public final double touchingEdges;
    public final double touchingWalls;
    public final double touchingFloor;
    public final double height;
    public final double holes;
    public final double blockade;
    public final double clear;

    public ComputerWeights(
            double touchingEdges,
            double touchingWalls,
            double touchingFloor,
            double height,
            double holes,
            double blockade,
            double clear) {
        this.touchingEdges = touchingEdges;
        this.touchingWalls = touchingWalls;
        this.touchingFloor = touchingFloor;
        this.height = height;
        this.holes = holes;
        this.blockade = blockade;
        this.clear = clear;
    }

    public static ComputerWeights preset() {
        return new ComputerWeights(3.97, 6.52, 0.65, -6.78, -3.31, -0.59, 3.6);
    }
}

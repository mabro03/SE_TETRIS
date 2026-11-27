package tetris.versusmode.computermode;

// 최적 배치: 목표 x와 현재 모양에서 시계 방향 회전 횟수.
public record Placement(int targetX, int clockwiseRotations, boolean[][] targetShape) {}

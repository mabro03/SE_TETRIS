package tetris.versusmode;

import java.io.Serializable;

import tetris.block.data.BlockData;
import tetris.block.data.CurrentBlock;
import tetris.gamescene.board.BoardElement;

public class NetworkData implements Serializable {
    // 클래스 버전 관리 (권장). 통신하는 양쪽의 클래스 형태가 같다는 것을 보증합니다.
    private static final long serialVersionUID = 1L;
    private BoardElement[][] board;
    private BlockData blockData;
    private BlockData[] nextBlocks;
    private BlockData blockHolding;
    private BoardElement[][] deletedBoard; // 2줄 이상 삭제 시, 삭제된 보드의 줄들을 저장
    private int score;
    private int hostIP;
    private int clientIP;
    private boolean isHost;

    public NetworkData(CurrentBlock currentBlock, int score, int hostIP, int clientIP, boolean isHost) {
        this.board = currentBlock.GetBoard();
        this.blockData = currentBlock.GetCurrentBlock();
        this.nextBlocks = currentBlock.GetBlockQueue().GetBlocksInQueueOrder();
        this.blockHolding = currentBlock.GetBlockHolding();
        this.deletedBoard = currentBlock.GetDelBoard();
        this.score = score;
        this.hostIP = hostIP;
        this.clientIP = clientIP;
        this.isHost = isHost;
    }

    public BoardElement[][] GetBoard() {
        return board;
    }
    public BlockData GetBlock() {
        return blockData;
    }
    public BlockData[] GetNextBlocks() {
        return nextBlocks;
    }
    public BlockData GetBlockHolding() {
        return blockHolding;
    }
    public BoardElement[][] GetVersusBoard() {
        return deletedBoard;
    }
    public int GetVersusScore() {
        return score;
    }
    public int GetHostIP() {
        return hostIP;
    }
    public int GetClientIP() {
        return clientIP;
    }
    public boolean GetIsHost() {
        return isHost;
    }
}

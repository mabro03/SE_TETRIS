package tetris.gamescene;


import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import java.util.Objects;

import tetris.block.data.BlockData;
import tetris.gamescene.board.*;

public class SceneRenderer  {
    private RenderConfig sceneRenderData;
    private Scene gameScene;
    private Canvas boardCanvas;
    private Canvas blockQueueCanvas;
    private Canvas blockHoldingCanvas;
    private Canvas scoreCanvas;
    private VBox leftVBox;
    private VBox rightVBox;
    private HBox contentHBox; 
    private BorderPane sceneLayout;

    public SceneRenderer() {
        this(new RenderConfig());
    }

    public SceneRenderer(RenderConfig config) {
        SetRenderConfig(config);
    }

    // 설정 교체만 한다. 기존 화면의 크기와 배치를 다시 적용하는 처리는 추후 구현한다.
    public void SetRenderConfig(RenderConfig config) {
        sceneRenderData = Objects.requireNonNull(config);
    }

    public Scene GetGameScene() {
        return gameScene;
    }

    public void CreateBoardCanvas(BoardElement[][] board){ // 보드 캔버스를 처음 생성하고 그린다.
        double blockSide = sceneRenderData.blockSide;
        boardCanvas = new Canvas(board[0].length * blockSide, board.length * blockSide);

    }

    public void UpdateBoardCanvas(BoardElement[][] board){ // 기존 보드 캔버스를 업데이트한다.
        double blockSide = sceneRenderData.blockSide;
        GraphicsContext graphicsContext = boardCanvas.getGraphicsContext2D();
        graphicsContext.clearRect(0, 0, boardCanvas.getWidth(), boardCanvas.getHeight());
        graphicsContext.setStroke(sceneRenderData.borderColor);

        for(int row = 0; row < board.length; row++){
            for(int col = 0; col < board[row].length; col++){
                double x = col * blockSide;
                double y = row * blockSide;

                graphicsContext.setFill(board[row][col].getElementColor());
                graphicsContext.fillRect(x, y, blockSide, blockSide);
                graphicsContext.strokeRect(x, y, blockSide, blockSide);
            }
        }
    }

   
    public void CreateBlockQueueCanvas() {
        blockQueueCanvas = new Canvas(sceneRenderData.blockQueueCanvasWidth,
                sceneRenderData.blockQueueCanvasHeight);
    }

    public void UpdateBlockQueueCanvas(BlockData[] blockQueue) {
        DrawPanel(blockQueueCanvas, "NEXT");
        if (blockQueue == null) return;
        int count = Math.min(sceneRenderData.blockQueueDisplayCount, blockQueue.length); // 화면에 보일 블럭 개수 설정 - 만약 수가 고정되면 없어도 됨
        for (int i = 0; i < count; i++) {
            double top = sceneRenderData.panelTitleHeight
                    + i * (sceneRenderData.previewSlotHeight + sceneRenderData.previewSlotGap);
            DrawBlockPreview(blockQueueCanvas, blockQueue[i], top, sceneRenderData.previewSlotHeight);
        }
    }

    public void CreateBlockHoldingCanvas() {
        blockHoldingCanvas = new Canvas(sceneRenderData.blockHoldingCanvasWidth,
                sceneRenderData.blockHoldingCanvasHeight);
    }

    public void UpdateBlockHoldingCanvas(BlockData blockHolding) {
        DrawPanel(blockHoldingCanvas, "HOLD");
        DrawBlockPreview(blockHoldingCanvas, blockHolding, sceneRenderData.panelTitleHeight,
                blockHoldingCanvas.getHeight() - sceneRenderData.panelTitleHeight - sceneRenderData.panelBottomPadding);
    }

    public void CreateScoreCanvas() {
        scoreCanvas = new Canvas(sceneRenderData.scoreCanvasWidth, sceneRenderData.scoreCanvasHeight);
    }

    public void UpdateScoreCanvas(long score) {
        DrawPanel(scoreCanvas, "SCORE");
        GraphicsContext graphicsContext = scoreCanvas.getGraphicsContext2D();
        graphicsContext.setFont(Font.font(sceneRenderData.scoreFontSize));
        graphicsContext.setTextBaseline(javafx.geometry.VPos.CENTER);
        graphicsContext.fillText(Long.toString(score), scoreCanvas.getWidth() / 2,
                (sceneRenderData.panelTitleHeight + scoreCanvas.getHeight() - sceneRenderData.panelBottomPadding) / 2,
                scoreCanvas.getWidth() - 2 * sceneRenderData.scoreHPadding);
    }

    private void CreateLayout() { // 씬을 구성할 레이아웃 (Hbox, VBox, borderpane)를 생성해서 정렬한다.
        leftVBox = new VBox(sceneRenderData.canvasVGap, blockQueueCanvas);
        rightVBox = new VBox(sceneRenderData.canvasVGap, blockHoldingCanvas, scoreCanvas);
        leftVBox.setAlignment(Pos.TOP_CENTER); 
        rightVBox.setAlignment(Pos.TOP_CENTER);

        // 세 영역을 묶어 창이 넓어져도 보드와 좌우 패널 사이의 간격을 유지한다.
        contentHBox = new HBox(sceneRenderData.sidePanelGap,
                leftVBox, boardCanvas, rightVBox);
        contentHBox.setAlignment(Pos.TOP_CENTER);
        contentHBox.setMinWidth(Region.USE_PREF_SIZE);
        contentHBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        sceneLayout = new BorderPane();
        sceneLayout.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(sceneRenderData.sceneColor,
                        javafx.scene.layout.CornerRadii.EMPTY, Insets.EMPTY)));
        sceneLayout.setPadding(new Insets(sceneRenderData.scenePadding));
        sceneLayout.setCenter(contentHBox);
        BorderPane.setAlignment(contentHBox, Pos.CENTER);
    }

     public Scene CreateScene(SceneRenderState state){ // 게임 씬을 생성

        CreateBoardCanvas(state.board());
        CreateBlockQueueCanvas();
        CreateBlockHoldingCanvas();
        CreateScoreCanvas();
        CreateLayout();
        gameScene = new Scene(sceneLayout, sceneRenderData.gameSceneWidth,
                sceneRenderData.gameSceneHeight, sceneRenderData.sceneColor);
        UpdateScene(state);
        return gameScene;
    }
    public void UpdateScene(SceneRenderState state){ // 게임 씬 업데이트
        
        UpdateBoardCanvas(state.board());
        UpdateBlockQueueCanvas(state.blockQueue());
        UpdateBlockHoldingCanvas(state.blockHolding());
        UpdateScoreCanvas(state.score());
        
    }





    private void DrawPanel(Canvas canvas, String title) {
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setFill(sceneRenderData.panelColor);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setStroke(sceneRenderData.borderColor);
        graphicsContext.strokeRect(0.5, 0.5, canvas.getWidth() - 1, canvas.getHeight() - 1);
        graphicsContext.setFill(sceneRenderData.textColor);
        graphicsContext.setFont(Font.font(sceneRenderData.titleFontSize));
        graphicsContext.setTextAlign(TextAlignment.CENTER);
        graphicsContext.setTextBaseline(javafx.geometry.VPos.CENTER);
        graphicsContext.fillText(title, canvas.getWidth() / 2, sceneRenderData.panelTitleHeight / 2,
                canvas.getWidth() - 2 * sceneRenderData.panelTitleHPadding);
    }

    // 빈 칸을 제외한 블럭 모양을 중앙에 그린다. 원본 모양이나 보드상의 위치는 변경하지 않는다.
    private void DrawBlockPreview(Canvas canvas, BlockData block, double top, double height) {
        if (block == null) return;
        boolean[][] shape = block.GetShape();
        int minRow = shape.length;
        int maxRow = -1;
        int minCol = Integer.MAX_VALUE;
        int maxCol = -1;
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (!shape[row][col]) continue;
                minRow = Math.min(minRow, row);
                maxRow = Math.max(maxRow, row);
                minCol = Math.min(minCol, col);
                maxCol = Math.max(maxCol, col);
            }
        }

        if (maxRow < 0) return;
        int rows = maxRow - minRow + 1;
        int cols = maxCol - minCol + 1;
        double side = Math.min(sceneRenderData.previewBlockSide,
                Math.min((canvas.getWidth() - 2 * sceneRenderData.blockPreviewPadding) / cols,
                        (height - 2 * sceneRenderData.blockPreviewPadding) / rows));
        if (side <= 0) return;
        double startX = (canvas.getWidth() - cols * side) / 2;
        double startY = top + (height - rows * side) / 2;
        Color color = block.GetCurrentColor();
        GraphicsContext graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.setFill(color);
        graphicsContext.setStroke(sceneRenderData.borderColor);
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (!shape[row][col]) continue;
                double x = startX + (col - minCol) * side;
                double y = startY + (row - minRow) * side;
                graphicsContext.fillRect(x, y, side, side);
                graphicsContext.strokeRect(x, y, side, side);
            }
        }
    }
}

// create함수는 빈 캔버스, 빈 씬 등을 최초로 생성만 함.
// 내용물은 Update 함수로 채워야 함.

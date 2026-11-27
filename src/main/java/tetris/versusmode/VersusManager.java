package tetris.versusmode;

import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.scene.Parent;
import javafx.util.Duration;
import tetris.block.data.ColorType;
import tetris.gamescene.board.BoardElement;
import tetris.gamescene.GameController;
import tetris.settings.GameSettings;
import tetris.settings.GameSettings.GameMode;
import tetris.versusmode.computermode.ComputerPlayer;

public class VersusManager {
    public enum Player { PLAYER1, PLAYER2 }
    public enum MatchState { READY, RUNNING, PAUSED, ENDED }
    public enum MatchOutcome { PLAYER1_WIN, PLAYER2_WIN, DRAW }


    private final GameController player1;
    private final GameController player2;
    private final VersusInput input;
    private final VersusRenderer renderer;
    private final VersusTimer timer;
    private final ComputerPlayer computerPlayer;
    private final boolean computerOpponent;
    private final Runnable onPauseRequested;
    private final Consumer<VersusResult> onMatchEnded;
    private long previousFrameTime;
    private MatchState state = MatchState.READY;
    private VersusResult result;

    private final AnimationTimer updateLoop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (state != MatchState.RUNNING) return;
            if (previousFrameTime == 0) {
                previousFrameTime = now;
                return;
            }
            double deltaTimeMs = (now - previousFrameTime) / 1_000_000.0;
            previousFrameTime = now;
            input.Update(deltaTimeMs);
            if (computerPlayer != null) {
                computerPlayer.Update(deltaTimeMs);
            }

            if (state == MatchState.RUNNING) { // 매 프레임마다 각 플레이어의 삭제한 줄을 읽어와 상대방의 versusBoard로 던져줌
                TransferAttack(Player.PLAYER1);
                TransferAttack(Player.PLAYER2);
            }

            if (timer != null &&state == MatchState.RUNNING) { // input.Update에서 게임이 끝나 EndMatch가 나 시간 업데이트나는 걸 막음
                timer.Update(deltaTimeMs);
                if (state == MatchState.RUNNING) { // timer.update에서 시간 만료가 되면 Endmatch가 나 FINISH가 떠야될 곳에 시간이 업데이트 되는 걸 막음.
                    renderer.RenderTime(timer.GetRemainingTimeMs());
                    }
            }
        }
    };


    public VersusManager(GameSettings settings, Runnable onPauseRequested, Consumer<VersusResult> onMatchEnded) {
        this(settings, onPauseRequested, onMatchEnded, 0, false);
    }

    public VersusManager(GameSettings settings, Runnable onPauseRequested,
            Consumer<VersusResult> onMatchEnded, boolean computerOpponent) {
        this(settings, onPauseRequested, onMatchEnded, 0, computerOpponent);
    }

    public VersusManager(GameSettings settings, Runnable onPauseRequested,
            Consumer<VersusResult> onMatchEnded, int initialClearedLineCount) {
        this(settings, onPauseRequested, onMatchEnded, initialClearedLineCount, false);
    }

    public VersusManager(GameSettings settings, Runnable onPauseRequested,
            Consumer<VersusResult> onMatchEnded, int initialClearedLineCount, boolean computerOpponent) {
        Objects.requireNonNull(settings);
        this.onPauseRequested = Objects.requireNonNull(onPauseRequested);
        this.onMatchEnded = Objects.requireNonNull(onMatchEnded);
        this.computerOpponent = computerOpponent;
        player1 = new GameController(
                ignored -> HandlePlayerDefeat(Player.PLAYER1), settings, initialClearedLineCount);
        player2 = new GameController(
                ignored -> HandlePlayerDefeat(Player.PLAYER2), settings, initialClearedLineCount);
        input = new VersusInput(this::PauseGame, player1.playerInput, player2.playerInput,
                settings.playerKeyBindings(), computerOpponent ? 1 : -1);
        input.SetInputEnabled(false);
        computerPlayer = computerOpponent
                ? new ComputerPlayer(player2::GetRenderState, player2.playerInput)
                : null;
        renderer = new VersusRenderer(player1.GetRoot(), player2.GetRoot(), settings);
        if(settings.gameMode() == GameMode.TIMELIMIT){
            timer = new VersusTimer(this::HandleTimeExpired);
            renderer.RenderTime(timer.GetRemainingTimeMs());
        }
        else{
            timer = null;
        }
        Parent root = renderer.GetRoot();
        root.setFocusTraversable(true);
        root.setOnKeyPressed(event -> input.HandleKeyPressed(event.getCode()));
        root.setOnKeyReleased(event -> input.HandleKeyReleased(event.getCode()));
    }

    public void GameStart() {
        if (state != MatchState.READY) return;
        state = MatchState.RUNNING;
        player1.GameStart();
        player2.GameStart();
        input.SetInputEnabled(true);
        if (computerPlayer != null) {
            computerPlayer.SetEnabled(true);
            computerPlayer.ResetTimer();
        }
        previousFrameTime = 0;
        updateLoop.start();
        GetRoot().requestFocus();
    }

    public void PauseGame() {
        if (state != MatchState.RUNNING) return;
        state = MatchState.PAUSED;
        input.SetInputEnabled(false);
        if (computerPlayer != null) {
            computerPlayer.SetEnabled(false);
        }
        updateLoop.stop();
        player1.PauseGame();
        player2.PauseGame();
        onPauseRequested.run();
    }

    public void ResumeGame() {
        if (state != MatchState.PAUSED) return;
        state = MatchState.RUNNING;
        player1.ResumeGame();
        player2.ResumeGame();
        input.SetInputEnabled(true);
        if (computerPlayer != null) {
            computerPlayer.SetEnabled(true);
            computerPlayer.ResetTimer();
        }
        previousFrameTime = 0;
        updateLoop.start();
        GetRoot().requestFocus();
    }

    public void HandlePlayerDefeat(Player loser) { // 플레이어 한 명이 패배해서 이 함수를 호출
        Objects.requireNonNull(loser);
        if (state != MatchState.RUNNING) return;
        EndMatch(loser == Player.PLAYER1 ? MatchOutcome.PLAYER2_WIN : MatchOutcome.PLAYER1_WIN);
    }

    public void HandleTimeExpired() { // 제한 시간 모드일 때, 시간이 만료될 때 이 함수 호출
        if (state != MatchState.RUNNING) return;
        int comparison = Integer.compare(player1.GetScore(), player2.GetScore());
        EndMatch(comparison > 0 ? MatchOutcome.PLAYER1_WIN
                : comparison < 0 ? MatchOutcome.PLAYER2_WIN : MatchOutcome.DRAW);
    }

    public void TransferAttack(Player sender) {
        Objects.requireNonNull(sender);
        GameController source = sender == Player.PLAYER1 ? player1 : player2;
        GameController target = sender == Player.PLAYER1 ? player2 : player1;
        BoardElement[][] attack = source.GetDelBoard();
        if (attack != null) {
            target.AddVersusBoards(attack);
        }
    }

    // 테스트용 임의 공격을 지정한 플레이어의 대기열에 추가한다.
    public void AddTestAttack(Player target, int lineCount) {
        Objects.requireNonNull(target);
        if (lineCount != 10 && lineCount != 20) {
            throw new IllegalArgumentException("테스트 공격 줄 수는 10 또는 20이어야 합니다.");
        }

        BoardElement[][] attack = new BoardElement[lineCount][10];
        for (int row = 0; row < attack.length; row++) {
            for (int col = 0; col < attack[row].length; col++) {
                attack[row][col] = new BoardElement(
                        col == (row % attack[row].length) ? ColorType.NONE : ColorType.GRAY,
                        col != (row % attack[row].length),
                        0);
            }
        }

        GameController targetController =
                target == Player.PLAYER1 ? player1 : player2;
        targetController.AddVersusBoards(attack);
        System.out.println(target + "에게 " + lineCount + "줄 테스트 공격을 추가했습니다.");
    }

    private void EndMatch(MatchOutcome outcome) { // HandlePlayerDefeat, HandleTimeExpired 함수로 호출됨. 게임을 정리하는 역할
        if (state == MatchState.ENDED) return;
        state = MatchState.ENDED;
        VersusResult.LocalWinLose localWinLose = toLocalWinLose(outcome);
        this.result = new VersusResult(
                computerOpponent ? "유저" : "플레이어1",
                player1.GetScore(),
                computerOpponent ? "컴퓨터" : "플레이어2",
                player2.GetScore(),
                localWinLose);
        input.SetInputEnabled(false);
        if (computerPlayer != null) {
            computerPlayer.SetEnabled(false);
        }
        updateLoop.stop();
        GetRoot().setOnKeyPressed(null);
        GetRoot().setOnKeyReleased(null);
        player1.CleanGame();
        player2.CleanGame();
        renderer.RenderFinish();
        PauseTransition finishDelay = new PauseTransition(Duration.seconds(3));
        finishDelay.setOnFinished(event -> onMatchEnded.accept(this.result));
        finishDelay.play();
    }

    private VersusResult.LocalWinLose toLocalWinLose(MatchOutcome outcome) {
        return switch (outcome) {
            case PLAYER1_WIN -> VersusResult.LocalWinLose.LOCALWIN;
            case PLAYER2_WIN -> VersusResult.LocalWinLose.LOCALLOSE;
            case DRAW -> VersusResult.LocalWinLose.LOCALDRAW;
        };
    }

    public Parent GetRoot() { return renderer.GetRoot(); }
    public MatchState GetState() { return state; }
    public VersusResult GetResult() { return result; }
}

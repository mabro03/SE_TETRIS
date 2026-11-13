package tetris;


import javafx.application.Application; // JavaFX 애플리케이션을 시작하는 클래스
import javafx.scene.Scene; // 화면 전환에 계속 사용하는 장면
import javafx.scene.layout.BorderPane; // Scene 생성 시 잠시 두는 빈 루트
import javafx.stage.Stage; // 실제 앱 창
import tetris.settings.GameSettings; // 저장된 화면 크기
import tetris.settings.SettingsStore; // 저장된 사용자 설정을 불러오는 클래스
import tetris.scoreboard.ScoreStorage; // 점수 저장 파일을 준비하는 클래스


// 프로그램 실행을 시작하는 진입점 클래스
// public class Main extends Application {
public class Main extends Application {
    private final SettingsStore settingsStore = new SettingsStore();

    // Java 프로그램이 처음 실행되는 메서드. JVM이 이 이름과 public static 형태만 찾는다.
    public static void main(String[] args) {
        launch(args); // JavaFX 창을 실행
    }

    @Override
    public void start(Stage stage) { // Scene과 Stage로 시작 화면 루트를 만든 뒤 화면 전환을 맡긴다
        settingsStore.load(); // 저장된 해상도, 색상 모드, 키 설정을 불러옴
        new ScoreStorage().initialize(); // 닉네임과 점수를 기록할 사용자 파일을 준비
        GameSettings savedSettings = settingsStore.getGameSettings();
        Scene scene = new Scene(new BorderPane(), savedSettings.screenWidth(), savedSettings.screenHeight());
        stage.setTitle("Tetris");
        stage.setResizable(false);
        stage.setScene(scene);
        new AppController(stage, scene, settingsStore).ShowStartScreen();
        stage.show();
    }
   
}

package tetris.settings;

import javafx.event.EventHandler; // 키 입력 필터 해제에 같은 인스턴스를 쓰기 위함
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox; // 해상도와 색상 모드 선택 상자
import javafx.scene.Scene; // 키 입력을 감지할 화면
import javafx.scene.control.Button; // 역할별 키 버튼
import javafx.scene.control.Label; // 상태 안내 문구
import javafx.scene.input.KeyCode; // 입력된 키 종류
import javafx.scene.input.KeyEvent; // 키보드 이벤트
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox; // 역할 행
import javafx.scene.layout.VBox; // 역할 행 목록
import javafx.stage.Stage; // 창 크기 변경을 위해 필요
import tetris.scoreboard.ScoreStorage;
import tetris.settings.KeyBindingSettings.ActionType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 설정 화면의 동작 로직을 담당하는 컨트롤러 클래스
public class SettingsController {
    // 현재 창과 각 UI 요소를 저장
    private final Stage stage; // 현재 앱 창
    private final Scene scene; // 현재 설정 화면
    private final ComboBox<String> resolutionComboBox; // 해상도 선택 상자
    private final ComboBox<GameSettings.ColorBlindMode> colorBlindModeComboBox; // 색상 모드 선택 상자
    private final ComboBox<GameSettings.Difficulty> difficultyComboBox; // 난이도 선택 상자
    private final KeyBindingSettings inputSettingData; // 키 배정 데이터
    private final VBox keyBindingRows; // 역할별 행 목록
    private final Label keySettingsStatus; // 키 변경 상태 표시
    private final Button saveSettingsButton; // 현재 설정을 저장하는 버튼
    private final Button resetSettingsButton;
    private final Button resetScoreButton;
    private final Button backButton;
    private final SettingsStore settingsStore;
    private final List<Button> navigationButtons;
    private int selectedButtonIndex;
    private final Map<ActionType, Button> keyButtons = new EnumMap<>(ActionType.class); // 역할과 버튼 연결
    private Label localStatus;
    private VBox localCommonRows;
    private HBox keyActionBox;
    private List<VBox> localPlayerRows = List.of();
    private final List<LocalKeyButton> localKeyButtons = new ArrayList<>();
    private ActionType actionWaitingForKey; // 새 키 입력을 기다리는 역할
    private KeyBindingSettings bindingsWaitingForKey;
    private Button buttonWaitingForKey;
    private Label statusWaitingForKey;
    private boolean waitingLocalKeys;
    private final EventHandler<KeyEvent> keyCaptureHandler = this::captureKeyBinding;
    private boolean keyCaptureActive;

    // 생성자: 필요한 UI 객체를 받아 저장
    public SettingsController(
            Stage stage,
            Scene scene,
            ComboBox<String> resolutionComboBox,
            ComboBox<GameSettings.ColorBlindMode> colorBlindModeComboBox,
            ComboBox<GameSettings.Difficulty> difficultyComboBox,
            KeyBindingSettings inputSettingData,
            VBox keyBindingRows,
            Label keySettingsStatus,
            Button saveSettingsButton,
            Button resetSettingsButton,
            Button resetScoreButton,
            Button backButton,
            SettingsStore settingsStore) {
        this.stage = stage;
        this.scene = scene;
        this.resolutionComboBox = resolutionComboBox;
        this.colorBlindModeComboBox = colorBlindModeComboBox;
        this.difficultyComboBox = difficultyComboBox;
        this.inputSettingData = inputSettingData;
        this.keyBindingRows = keyBindingRows;
        this.keySettingsStatus = keySettingsStatus;
        this.saveSettingsButton = saveSettingsButton;
        this.resetSettingsButton = resetSettingsButton;
        this.resetScoreButton = resetScoreButton;
        this.backButton = backButton;
        this.settingsStore = settingsStore;
        this.navigationButtons = List.of(
            saveSettingsButton, resetSettingsButton, resetScoreButton, backButton);
    }

    public void attachLocalKeys(Label localStatus, VBox localCommonRows, List<VBox> localPlayerRows) {
        this.localStatus = localStatus;
        this.localCommonRows = localCommonRows;
        this.localPlayerRows = localPlayerRows;
    }

    public void attachTabActions(TabPane settingsTabs, HBox keyActionBox) {
        this.keyActionBox = keyActionBox;
        settingsTabs.getSelectionModel().selectedIndexProperty().addListener(
                (observable, previous, index) -> applyVisibleKeyActions(index.intValue()));
        applyVisibleKeyActions(settingsTabs.getSelectionModel().getSelectedIndex());
    }

    // 화면이 열릴 때 저장값을 표시하고 버튼 동작을 연결한다.
    public void initialize() {
        settingsStore.load();
        showSavedSettings();
        bindSettingChanges();
        bindSettingButtons();
        createKeyBindingRows();
        bindLocalKeys();
    }

    private void showSavedSettings() {
        resolutionComboBox.getItems().addAll(settingsStore.resolutionPresets);
        resolutionComboBox.setValue(settingsStore.getResolutionPreset());
        colorBlindModeComboBox.getItems().setAll(GameSettings.ColorBlindMode.values());
        colorBlindModeComboBox.setValue(settingsStore.getColorBlindMode());
        difficultyComboBox.getItems().setAll(GameSettings.Difficulty.values());
        difficultyComboBox.setValue(settingsStore.getDifficulty());
        updateResolution();
    }

    private void bindSettingChanges() {
        resolutionComboBox.setOnAction(e -> {
            settingsStore.setResolutionPreset(resolutionComboBox.getValue());
            updateResolution();
        });
        colorBlindModeComboBox.setOnAction(e -> {
            settingsStore.setColorBlindMode(colorBlindModeComboBox.getValue());
        });
        difficultyComboBox.setOnAction(e -> {
            settingsStore.setDifficulty(difficultyComboBox.getValue());
        });
    }

    private void bindSettingButtons() {
        resetScoreButton.setOnAction(e -> resetScoreBoard());
    }

    private void applyVisibleKeyActions(int tabIndex) {
        boolean showsKeyActions = tabIndex == 1 || tabIndex == 2;
        keyActionBox.setVisible(showsKeyActions);
        keyActionBox.setManaged(showsKeyActions);
        if (tabIndex == 1) {
            saveSettingsButton.setOnAction(e -> saveSettings(keySettingsStatus));
            resetSettingsButton.setOnAction(e -> resetSoloKeys());
        } else if (tabIndex == 2) {
            saveSettingsButton.setOnAction(e -> saveSettings(localStatus));
            resetSettingsButton.setOnAction(e -> resetLocalKeys());
        }
    }

    public void saveCurrentSettings(Label statusLabel) {
        saveSettings(statusLabel);
    }

    public void resetDisplaySettings(Label statusLabel) {
        if (!confirm("설정 초기화", "화면 설정을 초기값으로 되돌릴까요?")) {
            return;
        }
        settingsStore.resetDisplaySettings();
        resolutionComboBox.setValue(settingsStore.getResolutionPreset());
        colorBlindModeComboBox.setValue(settingsStore.getColorBlindMode());
        difficultyComboBox.setValue(settingsStore.getDifficulty());
        updateResolution();
        GameSettings savedSettings = settingsStore.save();
        statusLabel.setText(savedSettings == null
                ? "기본값은 적용했지만 설정 파일 저장에 실패했습니다."
                : "화면 설정을 초기값으로 되돌렸습니다.");
    }

    private void bindLocalKeys() {
        if (localCommonRows == null) {
            return;
        }
        List<ActionType> commonActions = ActionType.valuesList().stream().filter(ActionType::isCommon).toList();
        List<ActionType> playerActions = ActionType.valuesList().stream().filter(action -> !action.isCommon()).toList();
        bindKeyRows(localCommonRows, commonActions, settingsStore.localCommonKeys(), true);
        List<KeyBindingSettings> players = settingsStore.localPlayerKeys();
        for (int index = 0; index < localPlayerRows.size(); index++) {
            bindKeyRows(localPlayerRows.get(index), playerActions, players.get(index), true);
        }
    }

    private void bindKeyRows(
            VBox rows,
            List<ActionType> actions,
            KeyBindingSettings bindings,
            boolean localKeys) {
        for (int index = 0; index < actions.size(); index++) {
            ActionType actionType = actions.get(index);
            Button keyButton = (Button) ((HBox) rows.getChildren().get(index)).getChildren().get(1);
            localKeyButtons.add(new LocalKeyButton(bindings, actionType, keyButton));
            keyButton.setText(displayedKeys(bindings, actionType));
            keyButton.setOnAction(event -> beginKeyCapture(bindings, actionType, keyButton, localStatus, localKeys));
        }
    }

    private void saveSettings(Label statusLabel) {
        GameSettings savedSettings = settingsStore.save();
        if (savedSettings == null) {
            statusLabel.setText("설정을 저장하지 못했습니다.");
            return;
        }
        statusLabel.setText("설정이 저장되었습니다.");
    }

    public void activate() {
        if (!keyCaptureActive) {
            scene.addEventFilter(KeyEvent.KEY_PRESSED, keyCaptureHandler);
            keyCaptureActive = true;
        }
        selectedButtonIndex = 0;
        navigationButtons.get(selectedButtonIndex).requestFocus();
    }

    // 같은 Scene을 다른 화면과 공유하므로, 설정 화면을 떠날 때 키 필터를 제거한다.
    public void dispose() {
        if (keyCaptureActive) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyCaptureHandler);
            keyCaptureActive = false;
        }
        clearKeyCapture();
    }

    // 선택된 해상도에 맞춰 창 크기 변경
    private void updateResolution() { // 선택한 해상도로 창 크기 변경
        String selected = resolutionComboBox.getValue();
        int index = -1;

        // 선택 값이 어떤 해상도인지 찾기
        for (int i = 0; i < settingsStore.resolutionPresets.length; i++) {
            if (settingsStore.resolutionPresets[i].equals(selected)) {
                index = i;
                break;
            }
        }

        // 유효한 해상도면 창 크기 조절
        if (index >= 0) {
            int width = settingsStore.resolutionValues[index][0];
            int height = settingsStore.resolutionValues[index][1];
            stage.setWidth(width);
            stage.setHeight(height);
        }
    }

    private void createKeyBindingRows() { // 역할별 버튼에 현재 키와 이벤트 연결
        for (ActionType actionType : ActionType.values()) {
            HBox row = (HBox) keyBindingRows.getChildren().get(actionType.ordinal());
            Button keyButton = (Button) row.getChildren().get(1);
            keyButtons.put(actionType, keyButton);
            updateKeyButton(actionType);
            keyButton.setOnAction(event -> beginKeyCapture(
                    inputSettingData, actionType, keyButton, keySettingsStatus, false));
        }
    }

    private void beginKeyCapture(
            KeyBindingSettings bindings,
            ActionType actionType,
            Button keyButton,
            Label statusLabel,
            boolean localKeys) {
        bindingsWaitingForKey = bindings;
        actionWaitingForKey = actionType;
        buttonWaitingForKey = keyButton;
        statusWaitingForKey = statusLabel;
        waitingLocalKeys = localKeys;
        statusLabel.setText(actionType.getDisplayName() + "에 지정할 키를 입력하세요.");
        scene.getRoot().requestFocus();
    }

    private void clearKeyCapture() {
        actionWaitingForKey = null;
        bindingsWaitingForKey = null;
        buttonWaitingForKey = null;
        statusWaitingForKey = null;
        waitingLocalKeys = false;
    }

    private void captureKeyBinding(KeyEvent event) { // 다음 키 입력을 새 배정값으로 처리
        if (actionWaitingForKey != null) {
            captureAssignedKey(event);
            return;
        }

        if (scene.getFocusOwner() instanceof ComboBox<?>) {
            return;
        }

        int focusedButtonIndex = navigationButtons.indexOf(scene.getFocusOwner());
        if (focusedButtonIndex >= 0) {
            selectedButtonIndex = focusedButtonIndex;
        }

        switch (event.getCode()) {
            case UP -> moveButtonFocus(-1, event);
            case DOWN -> moveButtonFocus(1, event);
            case ENTER -> {
                if (focusedButtonIndex >= 0) {
                    navigationButtons.get(selectedButtonIndex).fire();
                    event.consume();
                }
            }
            default -> { }
        }
    }

    private void captureAssignedKey(KeyEvent event) {
        KeyCode newKey = event.getCode();
        if (isKeyUsedByAnotherAction(newKey)) {
            showKeyAlreadyUsed(newKey);
        } else {
            assignWaitingKey(newKey);
        }
        event.consume();
    }

    private boolean isKeyUsedByAnotherAction(KeyCode newKey) {
        if (waitingLocalKeys) {
            if (usesKey(settingsStore.localCommonKeys(), newKey, true)) {
                return true;
            }
            return settingsStore.localPlayerKeys().stream()
                    .anyMatch(bindings -> usesKey(bindings, newKey, false));
        }
        return inputSettingData.getAllBindings().entrySet().stream()
                .anyMatch(binding -> binding.getKey() != actionWaitingForKey
                        && binding.getValue().contains(newKey));
    }

    private boolean usesKey(KeyBindingSettings bindings, KeyCode newKey, boolean commonActions) {
        return bindings.getAllBindings().entrySet().stream()
                .filter(binding -> binding.getKey().isCommon() == commonActions)
                .anyMatch(binding -> binding.getValue().contains(newKey)
                        && !(bindings == bindingsWaitingForKey && binding.getKey() == actionWaitingForKey));
    }

    private void showKeyAlreadyUsed(KeyCode newKey) {
        statusWaitingForKey.setText(newKey.getName() + " 키는 다른 역할에 이미 배정되어 있습니다.");
    }

    private void assignWaitingKey(KeyCode newKey) {
        ActionType updatedAction = actionWaitingForKey;
        bindingsWaitingForKey.setKeyCode(updatedAction, newKey);
        buttonWaitingForKey.setText(displayedKeys(bindingsWaitingForKey, updatedAction));
        if (!waitingLocalKeys) {
            updateKeyButton(updatedAction);
        }
        statusWaitingForKey.setText(updatedAction.getDisplayName() + " 키를 " + newKey.getName() + "(으)로 변경했습니다.");
        clearKeyCapture();
    }

    private void moveButtonFocus(int direction, KeyEvent event) {
        int buttonCount = navigationButtons.size();
        selectedButtonIndex = (selectedButtonIndex + direction + buttonCount) % buttonCount;
        navigationButtons.get(selectedButtonIndex).requestFocus();
        event.consume();
    }

    private void updateKeyButton(ActionType actionType) { // 버튼에 현재 키 목록 표시
        keyButtons.get(actionType).setText(displayedKeys(inputSettingData, actionType));
    }

    private String displayedKeys(KeyBindingSettings bindings, ActionType actionType) {
        return bindings.getKeyCodes(actionType).stream()
                .map(KeyCode::getName)
                .collect(Collectors.joining(" / "));
    }

    private void refreshLocalKeyButtons() {
        for (LocalKeyButton localKeyButton : localKeyButtons) {
            localKeyButton.button().setText(displayedKeys(localKeyButton.bindings(), localKeyButton.actionType()));
        }
    }

    private void resetSoloKeys() {
        if (!confirm("설정 초기화", "이 화면의 키 설정을 초기값으로 되돌릴까요?")) {
            return;
        }
        settingsStore.resetSoloKeyBindings();
        for (ActionType actionType : ActionType.values()) {
            updateKeyButton(actionType);
        }
        GameSettings savedSettings = settingsStore.save();
        keySettingsStatus.setText(savedSettings == null
                ? "기본값은 적용했지만 설정 파일 저장에 실패했습니다."
                : "키 설정을 초기값으로 되돌렸습니다.");
    }

    private void resetLocalKeys() {
        if (!confirm("설정 초기화", "로컬 키 설정을 초기값으로 되돌릴까요?")) {
            return;
        }
        settingsStore.resetLocalKeyBindings();
        refreshLocalKeyButtons();
        GameSettings savedSettings = settingsStore.save();
        localStatus.setText(savedSettings == null
                ? "기본값은 적용했지만 설정 파일 저장에 실패했습니다."
                : "로컬 키 설정을 초기값으로 되돌렸습니다.");
    }

    private void resetScoreBoard() {
        if (!confirm("스코어보드 초기화", "저장된 점수를 모두 삭제할까요?")) {
            return;
        }

        boolean cleared = new ScoreStorage().clearScores();
        showInformation(cleared
                ? "스코어보드를 초기화했습니다."
                : "스코어보드 초기화에 실패했습니다.");
    }

    private boolean confirm(String title, String message) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, message,
                ButtonType.YES, ButtonType.NO);
        confirmation.initOwner(stage);
        confirmation.setTitle(title);
        confirmation.setHeaderText(null);
        return confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void showInformation(String message) {
        Alert information = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        information.initOwner(stage);
        information.setHeaderText(null);
        information.showAndWait();
    }

    private record LocalKeyButton(KeyBindingSettings bindings, ActionType actionType, Button button) {
    }

}

package tetris.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tetris.settings.GameSettings.ColorBlindMode;
import tetris.settings.GameSettings.Difficulty;
import tetris.settings.GameSettings.GameMode;
import tetris.settings.GameSettings.PlayMode;
import tetris.settings.KeyBindingSettings.ActionType;

class SettingsStoreTest {
    @Test
    void providesGameSettingsSnapshotWhenLoadedLazily() {
        GameSettings settings = new SettingsStore().getGameSettings();

        assertNotNull(settings);
        assertTrue(settings.screenWidth() > 0);
        assertTrue(settings.screenHeight() > 0);
    }

    @Test
    void loadWithoutFileResetsGameModeOnly(@TempDir Path tempDir) {
        Path file = tempDir.resolve("settings.properties");
        SettingsStore store = new SettingsStore(file);
        store.setDifficulty(Difficulty.HARD);
        store.setGameMode(GameMode.ITEM);

        store.load();

        assertEquals(Difficulty.HARD, store.getDifficulty());
        assertEquals(GameMode.NORMAL, store.getGameMode());
        assertEquals("960x540", store.getResolutionPreset());
        assertEquals(960, store.getGameSettings().screenWidth());
        assertEquals(540, store.getGameSettings().screenHeight());
        assertFalse(Files.exists(file));
    }

    @Test
    void ignoresUnsupportedValues(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("settings.properties");
        Properties properties = new Properties();
        properties.setProperty("resolution", "10x10");
        properties.setProperty("colorBlindMode", "NOT_A_MODE");
        properties.setProperty("difficulty", "NOPE");
        properties.setProperty("gameMode", "NOPE");
        properties.setProperty("key.MOVE_LEFT", "A");
        properties.setProperty("key.PAUSE", "NOT_A_KEY");
        try (OutputStream output = Files.newOutputStream(file)) {
            properties.store(output, "test");
        }

        SettingsStore store = new SettingsStore(file);
        store.load();

        assertEquals("960x540", store.getResolutionPreset());
        assertEquals(960, store.getScreenWidth());
        assertEquals(540, store.getScreenHeight());
        assertEquals(ColorBlindMode.NORMAL, store.getColorBlindMode());
        assertEquals(Difficulty.NORMAL, store.getDifficulty());
        assertEquals(GameMode.NORMAL, store.getGameMode());
        assertEquals(KeyCode.A, store.getInputSettingData().getKeyCode(ActionType.MOVE_LEFT));
        assertEquals(KeyCode.ESCAPE, store.getInputSettingData().getKeyCode(ActionType.PAUSE));
    }

    @Test
    void keepsCurrentValueWhenSetterReceivesNullOrUnknownPreset(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir.resolve("settings.properties"));
        store.setResolutionPreset("1200x675");
        store.setResolutionPreset("999x999");
        store.setResolutionPreset(null);
        store.setColorBlindMode(ColorBlindMode.BLUE_YELLOW);
        store.setColorBlindMode(null);
        store.setDifficulty(Difficulty.HARD);
        store.setDifficulty(null);
        store.setGameMode(GameMode.ITEM);
        store.setGameMode(null);

        assertEquals("1200x675", store.getResolutionPreset());
        assertEquals(1200, store.getScreenWidth());
        assertEquals(675, store.getScreenHeight());
        assertEquals(ColorBlindMode.BLUE_YELLOW, store.getColorBlindMode());
        assertEquals(Difficulty.HARD, store.getDifficulty());
        assertEquals(GameMode.ITEM, store.getGameMode());
        assertFalse(Files.exists(tempDir.resolve("settings.properties")));
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) {
        Path file = tempDir.resolve("settings.properties");
        SettingsStore store = new SettingsStore(file);
        store.setResolutionPreset("1120x630");
        store.setColorBlindMode(ColorBlindMode.RED_GREEN);
        store.setDifficulty(Difficulty.EASY);
        store.setGameMode(GameMode.ITEM);
        store.getInputSettingData().setKeyCode(ActionType.HARD_DROP, KeyCode.Q);

        GameSettings saved = store.save();

        assertNotNull(saved);
        assertEquals("1120x630", saved.resolutionPreset());
        assertEquals(1120, saved.screenWidth());
        assertEquals(630, saved.screenHeight());
        assertEquals(1, saved.colorMode());
        assertEquals(Difficulty.EASY, saved.difficulty());
        assertEquals(GameMode.ITEM, saved.gameMode());
        assertEquals(KeyCode.Q, saved.keyBindings().get(ActionType.HARD_DROP).get(0));

        SettingsStore loaded = new SettingsStore(file);
        loaded.load();

        assertEquals("1120x630", loaded.getResolutionPreset());
        assertEquals(ColorBlindMode.RED_GREEN, loaded.getColorBlindMode());
        assertEquals(Difficulty.EASY, loaded.getDifficulty());
        assertEquals(GameMode.ITEM, loaded.getGameMode());
        assertEquals(KeyCode.Q, loaded.getInputSettingData().getKeyCode(ActionType.HARD_DROP));

        GameSettings reset = loaded.resetToDefaults();

        assertNotNull(reset);
        assertEquals("960x540", loaded.getResolutionPreset());
        assertEquals(ColorBlindMode.NORMAL, loaded.getColorBlindMode());
        assertEquals(Difficulty.NORMAL, loaded.getDifficulty());
        assertEquals(GameMode.NORMAL, loaded.getGameMode());
        assertEquals(KeyCode.SPACE, loaded.getInputSettingData().getKeyCode(ActionType.HARD_DROP));
        assertEquals(Difficulty.NORMAL, loaded.getGameSettings().difficulty());
    }

    @Test
    void saveReturnsNullWhenFileCannotBeWritten(@TempDir Path tempDir) throws Exception {
        Path blockedParent = tempDir.resolve("not-a-directory");
        Files.writeString(blockedParent, "x", StandardCharsets.UTF_8);
        SettingsStore store = new SettingsStore(blockedParent.resolve("settings.properties"));

        assertNull(store.save());

        GameSettings fallback = store.saveGameMode(GameMode.ITEM);

        assertNotNull(fallback);
        assertEquals(GameMode.ITEM, fallback.gameMode());
        assertEquals(GameMode.ITEM, store.getGameMode());
    }

    @Test
    void localModeUsesAdditionalPlayerKeys(@TempDir Path tempDir) {
        Path file = tempDir.resolve("settings.properties");
        SettingsStore store = new SettingsStore(file);
        store.getInputSettingData().setKeyCode(ActionType.MOVE_LEFT, KeyCode.G);
        store.localCommonKeys().setKeyCode(ActionType.PAUSE, KeyCode.P);
        store.localPlayerKeys().get(1).setKeyCode(ActionType.MOVE_LEFT, KeyCode.J);
        store.save();

        GameSettings solo = store.gameSettingsFor(PlayMode.SOLO);
        GameSettings online = store.gameSettingsFor(PlayMode.ONLINE);
        GameSettings local = store.gameSettingsFor(PlayMode.LOCAL);

        assertEquals(KeyCode.G, solo.keyBindings().get(ActionType.MOVE_LEFT).get(0));
        assertEquals(KeyCode.G, online.keyBindings().get(ActionType.MOVE_LEFT).get(0));
        assertEquals(1, solo.playerKeyBindings().size());
        assertEquals(KeyCode.LEFT, local.playerKeyBindings().get(0).get(ActionType.MOVE_LEFT).get(0));
        assertEquals(KeyCode.J, local.playerKeyBindings().get(1).get(ActionType.MOVE_LEFT).get(0));
        assertEquals(KeyCode.P, local.playerKeyBindings().get(0).get(ActionType.PAUSE).get(0));
        assertEquals(KeyCode.P, local.playerKeyBindings().get(1).get(ActionType.PAUSE).get(0));
        assertEquals(solo.resolutionPreset(), local.resolutionPreset());

        SettingsStore loaded = new SettingsStore(file);
        loaded.load();
        assertEquals(KeyCode.J, loaded.localPlayerKeys().get(1).getKeyCode(ActionType.MOVE_LEFT));
        assertEquals(KeyCode.P, loaded.localCommonKeys().getKeyCode(ActionType.PAUSE));
    }
}

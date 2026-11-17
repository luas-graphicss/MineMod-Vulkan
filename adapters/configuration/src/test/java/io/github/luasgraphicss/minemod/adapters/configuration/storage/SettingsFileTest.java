package io.github.luasgraphicss.minemod.adapters.configuration.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsFileTest {
    @TempDir
    Path gameDirectory;

    @Test
    void storesTheSettingsSortedAndReadsThemBack() throws IOException {
        ConfigLocation location = ConfigLocation.inGameDirectory(gameDirectory);
        SettingsFile file = new SettingsFile(location.settingsFile());
        Map<String, String> stored = new HashMap<>();
        stored.put("shaders.pack", "Sunlit\\Valleys.zip");
        stored.put("compatibility.mod-opengl", "false");

        file.write(stored);

        assertEquals(
                Arrays.asList("compatibility.mod-opengl=false", "shaders.pack=Sunlit\\\\Valleys.zip"),
                Files.readAllLines(location.settingsFile(), StandardCharsets.UTF_8));
        assertEquals(stored, file.read());
        assertEquals(gameDirectory.resolve("config").resolve("minemod-vulkan.properties"), location.settingsFile());
        assertFalse(Files.exists(location.settingsFile().resolveSibling("minemod-vulkan.properties.staged")));
    }

    @Test
    void aMissingFileHoldsNoSetting() throws IOException {
        SettingsFile file = new SettingsFile(ConfigLocation.inGameDirectory(gameDirectory).settingsFile());

        assertTrue(file.read().isEmpty());
        assertFalse(Files.exists(gameDirectory.resolve("config")));
    }

    @Test
    void refusesToStoreWhatWouldNotReadBackAsOneSetting() throws IOException {
        SettingsFile file = new SettingsFile(ConfigLocation.inGameDirectory(gameDirectory).settingsFile());

        assertThrows(
                IOException.class,
                () -> file.write(Collections.singletonMap("shaders.pack", "pack.zip\ncompatibility.mod-opengl=false")));
        assertThrows(IOException.class, () -> file.write(Collections.singletonMap("Shaders Pack", "pack.zip")));
    }

    @Test
    void refusesAFileLargerThanAnySettingsFile() throws IOException {
        Path settings = ConfigLocation.inGameDirectory(gameDirectory).settingsFile();
        Files.createDirectories(settings.getParent());
        Files.write(settings, new byte[64 * 1024 + 1]);

        assertThrows(IOException.class, () -> new SettingsFile(settings).read());
    }

    @Test
    void neverPlacesItsFilesAmongTheWorlds() throws IOException {
        Path worlds = Files.createDirectories(gameDirectory.resolve("saves").resolve("New World"));
        Path config = gameDirectory.resolve("config");
        try {
            Files.createSymbolicLink(config, worlds);
        } catch (UnsupportedOperationException | IOException unsupported) {
            return;
        }

        IOException refusal = assertThrows(IOException.class, () -> ConfigLocation.inGameDirectory(gameDirectory));

        assertTrue(refusal.getMessage().endsWith("resolves inside the worlds of the game, where nothing is written"));
    }
}

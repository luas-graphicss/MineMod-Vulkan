package io.github.luasgraphicss.minemod.adapters.configuration.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLocation {
    private static final String CONFIG_DIRECTORY = "config";
    private static final String WORLDS_DIRECTORY = "saves";
    private static final String SETTINGS_FILE = "minemod-vulkan.properties";
    private static final String STATE_DIRECTORY = "minemod-vulkan";
    private static final String LAUNCH_JOURNAL = "launches.log";

    private final Path configDirectory;

    private ConfigLocation(Path configDirectory) {
        this.configDirectory = configDirectory;
    }

    public static ConfigLocation inGameDirectory(Path gameDirectory) throws IOException {
        Path game = gameDirectory.toAbsolutePath().normalize();
        Path config = game.resolve(CONFIG_DIRECTORY);
        if (resolved(config).startsWith(resolved(game.resolve(WORLDS_DIRECTORY)))) {
            throw new IOException(config + " resolves inside the worlds of the game, where nothing is written");
        }
        return new ConfigLocation(config);
    }

    public Path settingsFile() {
        return configDirectory.resolve(SETTINGS_FILE);
    }

    public Path launchJournal() {
        return configDirectory.resolve(STATE_DIRECTORY).resolve(LAUNCH_JOURNAL);
    }

    private static Path resolved(Path path) throws IOException {
        return Files.exists(path) ? path.toRealPath() : path;
    }
}

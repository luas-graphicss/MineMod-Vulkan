package io.github.luasgraphicss.minemod.journey.record;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JourneyRecordFile {
    private static final String NAME = "record.json";
    private static final String STAGED_NAME = "record.json.staged";
    private static final Gson GSON =
            new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path directory;

    public JourneyRecordFile(Path directory) {
        this.directory = directory;
    }

    public void write(JourneyRecord record) {
        try {
            Files.createDirectories(directory);
            Path staged = directory.resolve(STAGED_NAME);
            Files.writeString(staged, GSON.toJson(record) + "\n");
            Files.move(
                    staged,
                    directory.resolve(NAME),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException unwritable) {
            throw new UncheckedIOException("the journey record cannot be written in " + directory, unwritable);
        }
    }
}

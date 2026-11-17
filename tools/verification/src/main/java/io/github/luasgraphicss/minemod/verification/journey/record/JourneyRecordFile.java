package io.github.luasgraphicss.minemod.verification.journey.record;

import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class JourneyRecordFile {
    public static final String NAME = "record.json";

    private static final long MAX_BYTES = 16L * 1024 * 1024;

    private JourneyRecordFile() {}

    public static Optional<JourneyRecord> readFrom(Path instanceDirectory) throws IOException {
        Path file = instanceDirectory.resolve(NAME);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(RepositoryJson.read(file, JourneyRecord.class, MAX_BYTES));
    }
}

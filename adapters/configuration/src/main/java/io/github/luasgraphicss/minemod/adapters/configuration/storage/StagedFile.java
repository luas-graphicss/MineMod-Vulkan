package io.github.luasgraphicss.minemod.adapters.configuration.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

final class StagedFile {
    private static final String STAGED_SUFFIX = ".staged";

    private StagedFile() {}

    static List<String> readLines(Path file, long maxBytes) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return Collections.emptyList();
        }
        if (Files.size(file) > maxBytes) {
            throw new IOException(file + " is larger than " + maxBytes + " bytes");
        }
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    static void replace(Path file, List<String> lines) throws IOException {
        Files.createDirectories(file.getParent());
        Path staged = file.resolveSibling(file.getFileName() + STAGED_SUFFIX);
        try {
            Files.write(staged, lines, StandardCharsets.UTF_8);
            Files.move(staged, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
    }
}

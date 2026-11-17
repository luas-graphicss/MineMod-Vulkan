package io.github.luasgraphicss.minemod.verification.json;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class RepositoryJson {
    private static final String STAGED_SUFFIX = ".staged";
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();

    private RepositoryJson() {}

    public static <T> T read(Path file, Class<T> type, long maxBytes) throws IOException {
        if (Files.size(file) > maxBytes) {
            throw new IOException(file + " exceeds " + maxBytes + " bytes");
        }
        return MAPPER.readValue(file.toFile(), type);
    }

    public static void write(Path file, Object document) throws IOException {
        Files.createDirectories(file.getParent());
        Path staged = file.resolveSibling(file.getFileName() + STAGED_SUFFIX);
        try {
            Files.writeString(staged, MAPPER.writer(prettyPrinter()).writeValueAsString(document) + "\n");
            Files.move(staged, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        Separators separators = Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withArrayEmptySeparator("")
                .withObjectEmptySeparator("");
        return new DefaultPrettyPrinter(separators).withArrayIndenter(indenter).withObjectIndenter(indenter);
    }
}

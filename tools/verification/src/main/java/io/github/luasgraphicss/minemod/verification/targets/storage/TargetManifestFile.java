package io.github.luasgraphicss.minemod.verification.targets.storage;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TargetManifestFile {
    private static final String REPOSITORY_PATH = "manifests/minecraft-targets.json";
    private static final long MAX_BYTES = 4L * 1024 * 1024;
    private static final String STAGED_SUFFIX = ".staged";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .build();
    private static final ObjectWriter WRITER = MAPPER.writer(prettyPrinter());

    private final Path path;

    private TargetManifestFile(Path path) {
        this.path = path;
    }

    public static TargetManifestFile inRepository(Path repositoryRoot) {
        return new TargetManifestFile(repositoryRoot.resolve(REPOSITORY_PATH));
    }

    public Path path() {
        return path;
    }

    public TargetManifest read() throws IOException {
        if (Files.size(path) > MAX_BYTES) {
            throw new IOException(path + " exceeds " + MAX_BYTES + " bytes");
        }
        return MAPPER.readValue(path.toFile(), TargetManifest.class);
    }

    public void write(TargetManifest manifest) throws IOException {
        Files.createDirectories(path.getParent());
        Path staged = path.resolveSibling(path.getFileName() + STAGED_SUFFIX);
        try {
            Files.writeString(staged, WRITER.writeValueAsString(manifest) + "\n");
            Files.move(staged, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
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

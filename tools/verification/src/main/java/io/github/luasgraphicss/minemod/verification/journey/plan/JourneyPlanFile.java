package io.github.luasgraphicss.minemod.verification.journey.plan;

import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class JourneyPlanFile {
    private static final String DIRECTORY = "tests/integration/vanilla-journey/plans";
    private static final String EXTENSION = ".json";
    private static final long MAX_BYTES = 1024L * 1024;

    private final Path path;

    private JourneyPlanFile(Path path) {
        this.path = path;
    }

    public static JourneyPlanFile inRepository(Path repositoryRoot, String name) {
        return new JourneyPlanFile(repositoryRoot.resolve(DIRECTORY).resolve(name + EXTENSION));
    }

    public Path path() {
        return path;
    }

    public JourneyPlan read() throws IOException {
        return RepositoryJson.read(path, JourneyPlan.class, MAX_BYTES);
    }

    public String sha256() throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("every Java runtime provides SHA-256", missing);
        }
    }
}

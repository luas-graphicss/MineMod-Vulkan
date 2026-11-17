package io.github.luasgraphicss.minemod.verification.journey.fixtures;

import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class JourneyRepository {
    private static final Path COMMITTED = Path.of(System.getProperty("minemod.repositoryRoot"));
    private static final String MANIFEST = "manifests/minecraft-targets.json";
    private static final String JOURNEYS = "tests/integration/vanilla-journey";
    private static final List<String> COPIED = List.of(JOURNEYS + "/plans", JOURNEYS + "/packs");
    private static final String DRIVER = JOURNEYS + "/fabric-1.21.1";
    private static final String CANDIDATE_MOD =
            "adapters/fabric/mc-1.21.1/build/libs/minemod-vulkan-fabric-1.21.1-0.1.0.jar";
    private static final String CACHE = "cache";

    private JourneyRepository() {}

    public static Path committed() {
        return COMMITTED;
    }

    public static void copyInto(Path root) throws IOException {
        Files.createDirectories(root.resolve(MANIFEST).getParent());
        Files.copy(COMMITTED.resolve(MANIFEST), root.resolve(MANIFEST));
        for (String directory : COPIED) {
            copy(COMMITTED.resolve(directory), root.resolve(directory));
        }
        Files.createDirectories(root.resolve(DRIVER));
    }

    public static void buildCandidateMod(Path root) throws IOException {
        Path mod = root.resolve(CANDIDATE_MOD);
        Files.createDirectories(mod.getParent());
        Files.createFile(mod);
    }

    public static ScenarioEnvironment environmentOf(Path root) {
        return new ScenarioEnvironment(root, root.resolve(CACHE));
    }

    private static void copy(Path source, Path target) throws IOException {
        List<Path> files;
        try (Stream<Path> walked = Files.walk(source)) {
            files = walked.toList();
        }
        for (Path file : files) {
            Path copied = target.resolve(source.relativize(file).toString());
            if (Files.isDirectory(file)) {
                Files.createDirectories(copied);
            } else {
                Files.copy(file, copied);
            }
        }
    }
}

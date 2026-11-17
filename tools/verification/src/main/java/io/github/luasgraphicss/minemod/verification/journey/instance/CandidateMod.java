package io.github.luasgraphicss.minemod.verification.journey.instance;

import io.github.luasgraphicss.minemod.verification.scenario.target.GameTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class CandidateMod {
    private static final String LIBRARIES = "build/libs";
    private static final String JAR = ".jar";
    private static final List<String> OTHER_ARTIFACTS = List.of("-sources.jar", "-dev.jar");

    private CandidateMod() {}

    public static Optional<Path> builtFor(Path repositoryRoot, GameTarget target) throws IOException {
        Path libraries = repositoryRoot.resolve(target.loaderAdapter()).resolve(LIBRARIES);
        if (!Files.isDirectory(libraries)) {
            return Optional.empty();
        }
        try (Stream<Path> built = Files.list(libraries)) {
            List<Path> mods = built.filter(CandidateMod::isTheMod).toList();
            return mods.size() == 1 ? Optional.of(mods.getFirst()) : Optional.empty();
        }
    }

    public static String buildCommand(GameTarget target) {
        return "sh gradlew -p " + target.loaderAdapter() + " build";
    }

    private static boolean isTheMod(Path file) {
        String name = file.getFileName().toString();
        return name.endsWith(JAR) && OTHER_ARTIFACTS.stream().noneMatch(name::endsWith);
    }
}

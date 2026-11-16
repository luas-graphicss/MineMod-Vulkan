package io.github.luasgraphicss.minemod.verification.matrix.modules;

import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class AdapterModules {
    private static final String BUILD_FILE = "build.gradle.kts";

    private AdapterModules() {}

    public static List<String> findMissing(Path repositoryRoot, AdapterBuild build) {
        return build.modules().stream()
                .filter(module -> !Files.isRegularFile(repositoryRoot.resolve(module).resolve(BUILD_FILE)))
                .toList();
    }
}

package io.github.luasgraphicss.minemod.verification.cli;

import java.nio.file.Path;

final class CacheOption {
    static final String NAME = "cache";
    private static final String DEFAULT_DIRECTORY = "build/verification-cache";

    private CacheOption() {}

    static Path directoryOf(Invocation invocation, Path repositoryRoot) {
        return invocation
                .option(NAME)
                .map(Path::of)
                .orElse(repositoryRoot.resolve(DEFAULT_DIRECTORY))
                .toAbsolutePath()
                .normalize();
    }
}

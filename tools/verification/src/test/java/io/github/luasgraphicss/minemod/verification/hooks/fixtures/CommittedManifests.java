package io.github.luasgraphicss.minemod.verification.hooks.fixtures;

import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;

public final class CommittedManifests {
    private static final Path REPOSITORY_ROOT = Path.of(System.getProperty("minemod.repositoryRoot"));

    private CommittedManifests() {}

    public static PresentationHooks hooks() throws IOException {
        return PresentationHooksFile.inRepository(REPOSITORY_ROOT).read();
    }

    public static TargetManifest targets() throws IOException {
        return TargetManifestFile.inRepository(REPOSITORY_ROOT).read();
    }
}

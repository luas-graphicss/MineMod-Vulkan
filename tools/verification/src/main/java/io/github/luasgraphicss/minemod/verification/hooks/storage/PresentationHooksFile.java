package io.github.luasgraphicss.minemod.verification.hooks.storage;

import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.json.RepositoryJson;
import java.io.IOException;
import java.nio.file.Path;

public final class PresentationHooksFile {
    private static final String REPOSITORY_PATH = "manifests/presentation-hooks.json";
    private static final long MAX_BYTES = 4L * 1024 * 1024;

    private final Path path;

    private PresentationHooksFile(Path path) {
        this.path = path;
    }

    public static PresentationHooksFile inRepository(Path repositoryRoot) {
        return new PresentationHooksFile(repositoryRoot.resolve(REPOSITORY_PATH));
    }

    public Path path() {
        return path;
    }

    public PresentationHooks read() throws IOException {
        return RepositoryJson.read(path, PresentationHooks.class, MAX_BYTES);
    }

    public void write(PresentationHooks hooks) throws IOException {
        RepositoryJson.write(path, hooks);
    }
}

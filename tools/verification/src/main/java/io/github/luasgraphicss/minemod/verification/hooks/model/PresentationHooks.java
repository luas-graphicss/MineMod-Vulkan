package io.github.luasgraphicss.minemod.verification.hooks.model;

import java.util.List;
import java.util.Optional;

public record PresentationHooks(int schemaVersion, List<ReleaseHooks> releases) {
    public static final int SCHEMA_VERSION = 1;

    public PresentationHooks {
        releases = List.copyOf(releases);
    }

    public Optional<ReleaseHooks> of(String release) {
        return releases.stream().filter(hooks -> hooks.release().equals(release)).findFirst();
    }
}

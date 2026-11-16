package io.github.luasgraphicss.minemod.verification.targets.model;

import java.util.List;

public record TargetManifest(int schemaVersion, List<String> sources, List<ReleaseTarget> releases) {
    public static final int SCHEMA_VERSION = 1;
}

package io.github.luasgraphicss.minemod.verification.compatibility.model;

import java.util.List;

public record CompatibilityManifest(int schemaVersion, List<CompatibilityCase> cases) {
    public static final int SCHEMA_VERSION = 1;
}

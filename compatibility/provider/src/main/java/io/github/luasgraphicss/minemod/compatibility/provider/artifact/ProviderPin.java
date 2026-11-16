package io.github.luasgraphicss.minemod.compatibility.provider.artifact;

import java.util.Objects;

public final class ProviderPin {
    private final String mesaVersion;
    private final ArtifactDigest libraryDigest;

    public ProviderPin(String mesaVersion, ArtifactDigest libraryDigest) {
        if (mesaVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("a pin names the exact Mesa release");
        }
        this.mesaVersion = mesaVersion;
        this.libraryDigest = Objects.requireNonNull(libraryDigest, "libraryDigest");
    }

    public String mesaVersion() {
        return mesaVersion;
    }

    public ArtifactDigest libraryDigest() {
        return libraryDigest;
    }
}

package io.github.luasgraphicss.minemod.adapters.ownership.claim;

import java.util.Objects;

public final class RendererIdentity {
    private final String modId;
    private final String version;

    public RendererIdentity(String modId, String version) {
        if (modId.trim().isEmpty() || version.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "a renderer is identified by its mod id and exact version: '" + modId + "' '" + version + "'");
        }
        this.modId = modId;
        this.version = version;
    }

    public String modId() {
        return modId;
    }

    public String version() {
        return version;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof RendererIdentity)) {
            return false;
        }
        RendererIdentity renderer = (RendererIdentity) other;
        return modId.equals(renderer.modId) && version.equals(renderer.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, version);
    }

    @Override
    public String toString() {
        return modId + " " + version;
    }
}

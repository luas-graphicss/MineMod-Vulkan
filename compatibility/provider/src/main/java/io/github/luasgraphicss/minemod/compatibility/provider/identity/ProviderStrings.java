package io.github.luasgraphicss.minemod.compatibility.provider.identity;

import java.util.Objects;

public final class ProviderStrings {
    private final String renderer;
    private final String version;

    public ProviderStrings(String renderer, String version) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.version = Objects.requireNonNull(version, "version");
    }

    public String renderer() {
        return renderer;
    }

    public String version() {
        return version;
    }
}

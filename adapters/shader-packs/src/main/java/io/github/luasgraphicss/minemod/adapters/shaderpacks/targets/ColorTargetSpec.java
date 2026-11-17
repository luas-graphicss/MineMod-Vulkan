package io.github.luasgraphicss.minemod.adapters.shaderpacks.targets;

import java.util.Objects;

public final class ColorTargetSpec {
    private final TextureFormat format;
    private final boolean clearedEachFrame;

    public ColorTargetSpec(TextureFormat format, boolean clearedEachFrame) {
        this.format = Objects.requireNonNull(format, "format");
        this.clearedEachFrame = clearedEachFrame;
    }

    public TextureFormat format() {
        return format;
    }

    public boolean clearedEachFrame() {
        return clearedEachFrame;
    }
}

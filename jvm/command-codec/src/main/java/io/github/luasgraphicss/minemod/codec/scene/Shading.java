package io.github.luasgraphicss.minemod.codec.scene;

import java.util.Set;

public enum Shading {
    TEXTURE(0),
    LIGHTMAP(1),
    OVERLAY(2),
    NORMAL(3),
    FOG(4),
    WIDE_LINES(5);

    private final int mask;

    Shading(int bit) {
        this.mask = 1 << bit;
    }

    static int maskOf(Set<Shading> steps) {
        int mask = 0;
        for (Shading step : steps) {
            mask |= step.mask;
        }
        return mask;
    }
}

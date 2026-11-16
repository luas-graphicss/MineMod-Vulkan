package io.github.luasgraphicss.minemod.codec.scene;

import java.util.Set;

public enum TextureSampling {
    LINEAR(0),
    BLEND_LEVELS(1),
    REPEAT(2);

    private final int mask;

    TextureSampling(int bit) {
        this.mask = 1 << bit;
    }

    static int maskOf(Set<TextureSampling> choices) {
        int mask = 0;
        for (TextureSampling choice : choices) {
            mask |= choice.mask;
        }
        return mask;
    }
}

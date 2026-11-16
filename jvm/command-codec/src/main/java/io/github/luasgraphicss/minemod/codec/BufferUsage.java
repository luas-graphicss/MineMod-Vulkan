package io.github.luasgraphicss.minemod.codec;

import java.util.Set;

public enum BufferUsage {
    VERTEX(0),
    INDEX(1),
    UNIFORM(2);

    private final int mask;

    BufferUsage(int bit) {
        this.mask = 1 << bit;
    }

    static int maskOf(Set<BufferUsage> usages) {
        int mask = 0;
        for (BufferUsage usage : usages) {
            mask |= usage.mask;
        }
        return mask;
    }

    static int knownMask() {
        int mask = 0;
        for (BufferUsage usage : values()) {
            mask |= usage.mask;
        }
        return mask;
    }
}

package io.github.luasgraphicss.minemod.codec.scene;

import java.util.Set;

public enum VertexElement {
    POSITION(0, 12),
    COLOR(1, 4),
    TEXTURE(2, 8),
    OVERLAY(3, 4),
    LIGHT(4, 4),
    NORMAL(5, 4);

    private final int mask;
    private final int bytes;

    VertexElement(int bit, int bytes) {
        this.mask = 1 << bit;
        this.bytes = bytes;
    }

    static int maskOf(Set<VertexElement> elements) {
        int mask = 0;
        for (VertexElement element : elements) {
            mask |= element.mask;
        }
        return mask;
    }

    static int strideOf(Set<VertexElement> elements) {
        int stride = 0;
        for (VertexElement element : elements) {
            stride += element.bytes;
        }
        return stride;
    }
}

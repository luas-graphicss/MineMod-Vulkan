package io.github.luasgraphicss.minemod.client.terrain;

public enum SectionLayer {
    SOLID(0),
    CUTOUT_MIPPED(1),
    CUTOUT(2),
    TRANSLUCENT(3);

    private final int index;

    SectionLayer(int index) {
        this.index = index;
    }

    int index() {
        return index;
    }
}

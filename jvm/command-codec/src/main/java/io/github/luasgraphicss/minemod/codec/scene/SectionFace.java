package io.github.luasgraphicss.minemod.codec.scene;

public enum SectionFace {
    WEST(0),
    EAST(1),
    DOWN(2),
    UP(3),
    NORTH(4),
    SOUTH(5);

    private final int code;

    SectionFace(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

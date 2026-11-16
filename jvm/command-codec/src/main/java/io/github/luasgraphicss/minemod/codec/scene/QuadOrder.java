package io.github.luasgraphicss.minemod.codec.scene;

public enum QuadOrder {
    SUBMITTED(0),
    FARTHEST_FIRST(1),
    RISING_DEPTH(2);

    private final int code;

    QuadOrder(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

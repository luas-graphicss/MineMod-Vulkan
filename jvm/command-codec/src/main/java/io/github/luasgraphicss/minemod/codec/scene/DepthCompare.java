package io.github.luasgraphicss.minemod.codec.scene;

public enum DepthCompare {
    LESS_OR_EQUAL(0),
    ALWAYS(1),
    EQUAL(2),
    GREATER(3);

    private final int code;

    DepthCompare(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

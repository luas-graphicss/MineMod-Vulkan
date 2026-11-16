package io.github.luasgraphicss.minemod.codec.scene;

public enum PrimitiveMode {
    TRIANGLES(0),
    TRIANGLE_STRIP(1),
    TRIANGLE_FAN(2),
    QUADS(3),
    LINES(4);

    private final int code;

    PrimitiveMode(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

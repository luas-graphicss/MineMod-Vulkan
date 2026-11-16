package io.github.luasgraphicss.minemod.codec.scene;

public enum Blending {
    DISABLED(0),
    ALPHA(1),
    ADDITIVE(2),
    LIGHTNING(3),
    GLINT(4),
    CRUMBLING(5),
    INVERSION(6),
    DARKENING(7);

    private final int code;

    Blending(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

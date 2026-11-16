package io.github.luasgraphicss.minemod.codec.scene;

public enum FogShape {
    SPHERE(0),
    CYLINDER(1);

    private final int code;

    FogShape(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

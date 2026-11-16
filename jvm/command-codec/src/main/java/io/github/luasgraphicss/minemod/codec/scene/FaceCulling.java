package io.github.luasgraphicss.minemod.codec.scene;

public enum FaceCulling {
    BACK_FACES(0),
    DISABLED(1);

    private final int code;

    FaceCulling(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

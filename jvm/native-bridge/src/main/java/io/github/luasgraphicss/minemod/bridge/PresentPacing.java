package io.github.luasgraphicss.minemod.bridge;

public enum PresentPacing {
    VSYNC(0),
    IMMEDIATE(1);

    private final int code;

    PresentPacing(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

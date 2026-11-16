package io.github.luasgraphicss.minemod.bridge.sharing;

public enum SharedAttachment {
    COLOR(0),
    DEPTH_STENCIL(1);

    private final int code;

    SharedAttachment(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}

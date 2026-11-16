package io.github.luasgraphicss.minemod.codec.scene;

public enum BatchSlot {
    SKY(0),
    SOLID(1),
    BLENDED(2),
    HAND(3),
    INTERFACE(4);

    private final int code;

    BatchSlot(int code) {
        this.code = code;
    }

    int code() {
        return code;
    }
}

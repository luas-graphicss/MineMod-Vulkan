package io.github.luasgraphicss.minemod.bridge.sharing;

public enum HandoffSynchronization {
    BLOCKING(0),
    SEMAPHORES(1);

    private final int code;

    HandoffSynchronization(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}

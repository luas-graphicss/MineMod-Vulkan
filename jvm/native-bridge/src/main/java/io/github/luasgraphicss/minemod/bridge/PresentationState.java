package io.github.luasgraphicss.minemod.bridge;

public enum PresentationState {
    OFFSCREEN,
    AWAITING_SWAPCHAIN,
    RUNNING,
    SUSPENDED,
    DEVICE_LOST,
    STOPPED;

    private static final PresentationState[] BY_CODE = values();

    static PresentationState fromCode(long code) {
        if (code < 0 || code >= BY_CODE.length) {
            throw new IllegalArgumentException("unknown presentation state code: " + code);
        }
        return BY_CODE[(int) code];
    }
}

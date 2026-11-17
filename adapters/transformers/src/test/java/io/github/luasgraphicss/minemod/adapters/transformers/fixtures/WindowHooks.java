package io.github.luasgraphicss.minemod.adapters.transformers.fixtures;

public final class WindowHooks {
    public static final long HOOKED_WINDOW = 42L;

    private static int presentedFrames;

    private WindowHooks() {}

    public static long createWindow(int width, int height) {
        return HOOKED_WINDOW;
    }

    public static void swapBuffers(long window) {
        presentedFrames++;
    }

    public static int presentedFrames() {
        return presentedFrames;
    }

    public static void forgetFrames() {
        presentedFrames = 0;
    }
}

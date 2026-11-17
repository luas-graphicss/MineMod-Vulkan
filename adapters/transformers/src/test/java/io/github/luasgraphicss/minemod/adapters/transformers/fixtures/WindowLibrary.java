package io.github.luasgraphicss.minemod.adapters.transformers.fixtures;

public final class WindowLibrary {
    public static final long LIBRARY_WINDOW = 7L;

    private static int swaps;

    private WindowLibrary() {}

    public static long createWindow(int width, int height) {
        return LIBRARY_WINDOW;
    }

    public static void swapBuffers(long window) {
        swaps++;
    }

    public static void pollEvents() {}

    public static int swaps() {
        return swaps;
    }

    public static void forgetSwaps() {
        swaps = 0;
    }
}

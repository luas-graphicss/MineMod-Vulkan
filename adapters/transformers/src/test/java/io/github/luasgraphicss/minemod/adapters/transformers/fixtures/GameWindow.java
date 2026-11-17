package io.github.luasgraphicss.minemod.adapters.transformers.fixtures;

public final class GameWindow {
    private static final int WIDTH = 854;
    private static final int HEIGHT = 480;

    private long handle;

    public long open() {
        handle = WindowLibrary.createWindow(WIDTH, HEIGHT);
        return handle;
    }

    public long reopen() {
        handle = WindowLibrary.createWindow(WIDTH, HEIGHT);
        return handle;
    }

    public void flipTwice() {
        WindowLibrary.swapBuffers(handle);
        WindowLibrary.pollEvents();
        WindowLibrary.swapBuffers(handle);
    }
}

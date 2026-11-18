package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;

public final class ClearCalls {
    private ClearCalls() {}

    public static void glClearColor(float red, float green, float blue, float alpha) {
        GameFrames.run(frame -> frame.pipeline().surface().clearColor(red, green, blue, alpha));
    }

    public static void glClear(int buffers) {
        GameFrames.run(frame -> frame.clear(buffers));
    }

    public static void glViewport(int left, int bottom, int width, int height) {
        GameFrames.run(frame -> frame.viewport(left, bottom, width, height));
    }
}

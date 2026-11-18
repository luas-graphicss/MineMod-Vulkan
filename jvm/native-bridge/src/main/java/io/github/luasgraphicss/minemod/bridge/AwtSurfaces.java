package io.github.luasgraphicss.minemod.bridge;

import java.awt.Canvas;

public final class AwtSurfaces {
    private static final String AWT_NATIVE_INTERFACE = "jawt";

    private AwtSurfaces() {}

    public static long create(long vulkanInstance, Canvas canvas) {
        System.loadLibrary(AWT_NATIVE_INTERFACE);
        long[] surface = new long[1];
        NativeResults.requireOk(
                nativeCreate(vulkanInstance, canvas, surface), "creating a Vulkan surface on the AWT canvas " + canvas);
        return surface[0];
    }

    private static native int nativeCreate(long vulkanInstance, Canvas canvas, long[] surface);
}

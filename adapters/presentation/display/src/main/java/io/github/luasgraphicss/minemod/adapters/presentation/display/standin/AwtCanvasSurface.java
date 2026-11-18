package io.github.luasgraphicss.minemod.adapters.presentation.display.standin;

import io.github.luasgraphicss.minemod.bridge.AwtSurfaces;
import io.github.luasgraphicss.minemod.client.window.FramebufferSize;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;
import java.awt.Canvas;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class AwtCanvasSurface implements WindowSurface {
    private static final String SURFACE_EXTENSION = "VK_KHR_surface";
    private static final String WINDOWS_SURFACE_EXTENSION = "VK_KHR_win32_surface";
    private static final String XLIB_SURFACE_EXTENSION = "VK_KHR_xlib_surface";
    private static final String SYSTEM_NAME = "os.name";
    private static final String WINDOWS = "windows";

    private final Canvas canvas;

    AwtCanvasSurface(Canvas canvas) {
        this.canvas = canvas;
    }

    @Override
    public List<String> requiredInstanceExtensions() {
        return extensionsOn(System.getProperty(SYSTEM_NAME));
    }

    static List<String> extensionsOn(String systemName) {
        boolean isWindows = systemName.toLowerCase(Locale.ROOT).startsWith(WINDOWS);
        return Arrays.asList(SURFACE_EXTENSION, isWindows ? WINDOWS_SURFACE_EXTENSION : XLIB_SURFACE_EXTENSION);
    }

    @Override
    public long createSurface(long vulkanInstance) {
        return AwtSurfaces.create(vulkanInstance, canvas);
    }

    @Override
    public FramebufferSize framebufferSize() {
        AffineTransform toPixels = canvas.getGraphicsConfiguration().getDefaultTransform();
        return new FramebufferSize(
                (int) Math.round(canvas.getWidth() * toPixels.getScaleX()),
                (int) Math.round(canvas.getHeight() * toPixels.getScaleY()));
    }
}

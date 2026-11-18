package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.pipeline.SurfaceState;
import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;
import java.util.function.Consumer;

public final class SurfaceCalls {
    private SurfaceCalls() {}

    public static void glBlendFunc(int source, int destination) {
        change(surface -> surface.blendFactors(source, destination));
    }

    public static void glBlendFuncSeparate(int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha) {
        glBlendFunc(sourceColor, destinationColor);
    }

    public static void glBlendFuncSeparateEXT(
            int sourceColor, int destinationColor, int sourceAlpha, int destinationAlpha) {
        glBlendFunc(sourceColor, destinationColor);
    }

    public static void glDepthFunc(int function) {
        change(surface -> surface.depthFunction(function));
    }

    public static void glDepthMask(boolean isWritten) {
        change(surface -> surface.depthWrite(isWritten));
    }

    public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        change(surface -> surface.colorWrite(red || green || blue || alpha));
    }

    public static void glPolygonOffset(float factor, float units) {
        change(surface -> surface.depthOffset(factor, units));
    }

    public static void glAlphaFunc(int comparison, float reference) {
        GameFrames.run(frame -> frame.pipeline().alphaTest().compare(comparison, reference));
    }

    public static void glColor4f(float red, float green, float blue, float alpha) {
        GameFrames.run(frame -> frame.pipeline().color(red, green, blue, alpha));
    }

    public static void glLineWidth(float pixels) {
        GameFrames.run(frame -> frame.pipeline().lineWidth(pixels));
    }

    private static void change(Consumer<SurfaceState> change) {
        GameFrames.run(frame -> change.accept(frame.pipeline().surface()));
    }
}

package io.github.luasgraphicss.minemod.adapters.frame.calls;

import io.github.luasgraphicss.minemod.adapters.frame.session.GameFrames;

public final class TextureUnitCalls {
    private TextureUnitCalls() {}

    public static void glActiveTexture(int unit) {
        GameFrames.run(frame -> frame.textures().activate(unit));
    }

    public static void glActiveTextureARB(int unit) {
        glActiveTexture(unit);
    }

    public static void glClientActiveTexture(int unit) {
        GameFrames.current().arrays().activateUnit(unit);
    }

    public static void glClientActiveTextureARB(int unit) {
        glClientActiveTexture(unit);
    }

    public static void glMultiTexCoord2f(int unit, float horizontal, float vertical) {
        GameFrames.run(frame -> frame.pipeline().units().coordinates(unit, horizontal, vertical));
    }

    public static void glMultiTexCoord2fARB(int unit, float horizontal, float vertical) {
        glMultiTexCoord2f(unit, horizontal, vertical);
    }
}

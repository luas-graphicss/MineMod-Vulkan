package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import net.minecraft.client.renderer.ShaderInstance;

final class GlintAlpha {
    private static final float VISIBLE = 1.0f;

    private GlintAlpha() {}

    static float of(ShaderInstance shader) {
        return VISIBLE;
    }
}

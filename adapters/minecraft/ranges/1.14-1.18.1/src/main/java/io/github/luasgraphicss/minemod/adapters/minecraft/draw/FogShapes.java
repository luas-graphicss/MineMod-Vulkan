package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import io.github.luasgraphicss.minemod.codec.scene.FogShape;

public final class FogShapes {
    private FogShapes() {}

    public static FogShape current() {
        return FogShape.SPHERE;
    }
}

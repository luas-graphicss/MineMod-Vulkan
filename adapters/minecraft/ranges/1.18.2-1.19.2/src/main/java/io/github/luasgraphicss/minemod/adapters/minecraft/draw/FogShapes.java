package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.luasgraphicss.minemod.codec.scene.FogShape;

public final class FogShapes {
    private FogShapes() {}

    public static FogShape current() {
        return RenderSystem.getShaderFogShape() == com.mojang.blaze3d.shaders.FogShape.CYLINDER
                ? FogShape.CYLINDER
                : FogShape.SPHERE;
    }
}

package io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.shaders;

import io.github.luasgraphicss.minemod.adapters.minecraft.v1_21_1.bootstrap.ClientRendering;
import net.minecraft.client.renderer.ShaderInstance;

public final class ReplacedShaders {
    private ReplacedShaders() {}

    public static int programOf(ShaderInstance shader) {
        return ClientRendering.renderer().reload().programs().programOf(shader.getName());
    }
}

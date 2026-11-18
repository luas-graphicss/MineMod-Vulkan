package io.github.luasgraphicss.minemod.adapters.minecraft.shaders;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import net.minecraft.client.renderer.ShaderInstance;

public final class ReplacedShaders {
    private ReplacedShaders() {}

    public static int programOf(ShaderInstance shader) {
        return ClientRendering.renderer().reload().programs().programOf(shader.getName());
    }
}

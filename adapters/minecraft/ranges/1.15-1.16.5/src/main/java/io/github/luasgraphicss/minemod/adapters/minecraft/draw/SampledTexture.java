package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.TextureUnits;
import io.github.luasgraphicss.minemod.adapters.minecraft.frame.GameFrame;
import io.github.luasgraphicss.minemod.client.opengl.textures.TextureObjects;
import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.util.Optional;

final class SampledTexture {
    private SampledTexture() {}

    static Optional<ResourceHandle> bound() {
        TextureObjects textures = ClientRendering.openGl().textures();
        int active = textures.activeUnit();
        textures.activate(TextureUnits.FIRST_NAME + TextureUnits.SAMPLED);
        int sampled = textures.bound();
        textures.activate(active);
        return GameFrame.textures().find(sampled);
    }
}

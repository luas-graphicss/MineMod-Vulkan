package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

public final class OwnedSceneTextures implements SceneSource {
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_DEPTH24_STENCIL8 = 0x88F0;
    private static final int ONE_LEVEL = 1;
    private static final int NO_TEXTURE = 0;
    private final int width;
    private final int height;

    public OwnedSceneTextures(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public SceneTextures attach(ProviderCalls provider, ScratchInts scratch) {
        int color = allocate(provider, scratch, GL_RGBA8);
        int depthStencil = allocate(provider, scratch, GL_DEPTH24_STENCIL8);
        provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, NO_TEXTURE});
        return new SceneTextures(color, depthStencil);
    }

    private int allocate(ProviderCalls provider, ScratchInts scratch, int internalFormat) {
        provider.invoke("glGenTextures", new long[] {1, scratch.address()});
        int texture = scratch.first();
        provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, texture});
        provider.invoke("glTexStorage2D", new long[] {GL_TEXTURE_2D, ONE_LEVEL, internalFormat, width, height});
        return texture;
    }
}

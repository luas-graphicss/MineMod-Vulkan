package io.github.luasgraphicss.minemod.compatibility.runtime;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.layer.scene.SceneFramebuffer;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class SceneTarget {
    private final ProviderCalls provider;
    private final NativeMemory memory;
    private final int framebuffer;

    SceneTarget(ProviderCalls provider, NativeMemory memory, int framebuffer) {
        this.provider = provider;
        this.memory = memory;
        this.framebuffer = framebuffer;
    }

    int framebuffer() {
        return framebuffer;
    }

    void attach(SceneTextures textures) {
        try (ScratchInts scratch = new ScratchInts(memory, 1)) {
            SceneFramebuffer.reattach(provider, scratch, framebuffer, textures.color(), textures.depthStencil());
        }
    }
}

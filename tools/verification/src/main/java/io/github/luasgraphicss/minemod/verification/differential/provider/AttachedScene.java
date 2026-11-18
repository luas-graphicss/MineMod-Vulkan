package io.github.luasgraphicss.minemod.verification.differential.provider;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneSource;
import io.github.luasgraphicss.minemod.compatibility.runtime.scene.SceneTextures;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class AttachedScene implements SceneSource {
    private final SceneSource textures;
    private ProviderCalls provider;

    AttachedScene(SceneSource textures) {
        this.textures = textures;
    }

    @Override
    public SceneTextures attach(ProviderCalls calls, ScratchInts scratch) {
        provider = calls;
        return textures.attach(calls, scratch);
    }

    ProviderCalls provider() {
        if (provider == null) {
            throw new IllegalStateException("no context has attached the scene, so its provider is unknown");
        }
        return provider;
    }
}

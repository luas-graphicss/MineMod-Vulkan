package io.github.luasgraphicss.minemod.compatibility.runtime.scene;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

public interface SceneSource {
    SceneTextures attach(ProviderCalls provider, ScratchInts scratch);
}

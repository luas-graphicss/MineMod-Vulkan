package io.github.luasgraphicss.minemod.client.draw.texture;

import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import java.util.Optional;

public interface SceneTextures {
    SceneTextures NONE = gameTexture -> Optional.empty();

    Optional<ResourceHandle> find(int gameTexture);
}

package io.github.luasgraphicss.minemod.client.compatibility.textures;

import java.util.OptionalInt;

public interface TextureAliases {
    int publish(long resource, int providerTexture);

    boolean withdraw(long resource);

    OptionalInt nameOf(long resource);
}

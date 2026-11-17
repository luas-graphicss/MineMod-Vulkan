package io.github.luasgraphicss.minemod.client.compatibility.textures;

import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import java.util.Objects;
import java.util.OptionalInt;

public final class LayerAliases implements TextureAliases {
    private final CompatibilityLayer layer;

    public LayerAliases(CompatibilityLayer layer) {
        this.layer = Objects.requireNonNull(layer, "layer");
    }

    @Override
    public int publish(long resource, int providerTexture) {
        return layer.publishVanillaTexture(resource, providerTexture);
    }

    @Override
    public boolean withdraw(long resource) {
        return layer.withdrawVanillaTexture(resource);
    }

    @Override
    public OptionalInt nameOf(long resource) {
        return layer.vanillaTextureName(resource);
    }
}

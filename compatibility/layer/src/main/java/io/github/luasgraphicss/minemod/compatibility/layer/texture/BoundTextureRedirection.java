package io.github.luasgraphicss.minemod.compatibility.layer.texture;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.redefine.RedefinitionBackend;
import io.github.luasgraphicss.minemod.compatibility.translation.names.ObjectNames;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderObjects;
import io.github.luasgraphicss.minemod.compatibility.translation.texture.TextureBindings;
import java.util.Objects;

public final class BoundTextureRedirection implements RedefinitionBackend {
    private static final String ACTIVE_TEXTURE = "glActiveTexture";
    private static final String BIND_TEXTURE = "glBindTexture";
    private static final int GL_TEXTURE0 = 0x84C0;
    private static final int NO_PARAMETER = 0;
    private final TextureBindings bindings;
    private final ObjectNames names;
    private final ProviderObjects objects;
    private final ProviderCalls provider;

    public BoundTextureRedirection(
            TextureBindings bindings, ObjectNames names, ProviderObjects objects, ProviderCalls provider) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.names = Objects.requireNonNull(names, "names");
        this.objects = Objects.requireNonNull(objects, "objects");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public int allocatePrivateStorage() {
        return objects.create(ObjectKind.TEXTURE, NO_PARAMETER);
    }

    @Override
    public void redirectReferences(int fromBacking, int toBacking) {
        int name = names.nameOfBacking(ObjectKind.TEXTURE, fromBacking);
        bindings.forEachBindingOf(name, (unit, target) -> {
            provider.invoke(ACTIVE_TEXTURE, new long[] {GL_TEXTURE0 + unit});
            provider.invoke(BIND_TEXTURE, new long[] {target, toBacking});
        });
        provider.invoke(ACTIVE_TEXTURE, new long[] {GL_TEXTURE0 + bindings.activeUnit()});
    }

    @Override
    public void discard(int backing) {
        objects.dispose(ObjectKind.TEXTURE, backing);
    }
}

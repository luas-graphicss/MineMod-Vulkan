package io.github.luasgraphicss.minemod.compatibility.resources.redefine;

import io.github.luasgraphicss.minemod.compatibility.resources.alias.VanillaAliases;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameTable;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ObjectDisposer;
import java.util.Objects;

public final class TextureRedefinition {
    private final NameTable textures;
    private final VanillaAliases aliases;
    private final RedefinitionBackend backend;
    private final ObjectDisposer disposer;

    public TextureRedefinition(
            NameTable textures, VanillaAliases aliases, RedefinitionBackend backend, ObjectDisposer disposer) {
        this.textures = Objects.requireNonNull(textures, "textures");
        this.aliases = Objects.requireNonNull(aliases, "aliases");
        this.backend = Objects.requireNonNull(backend, "backend");
        this.disposer = Objects.requireNonNull(disposer, "disposer");
    }

    public int redefine(int name) {
        if (!aliases.isAlias(name)) {
            throw new IllegalArgumentException("texture " + name + " does not alias a vanilla resource");
        }
        int shared = textures.backingOf(name);
        int replacement = backend.allocatePrivateStorage();
        try {
            backend.redirectReferences(shared, replacement);
        } catch (RuntimeException failure) {
            backend.discard(replacement);
            throw failure;
        }
        textures.rebind(name, replacement, disposer);
        aliases.detach(name);
        return replacement;
    }
}

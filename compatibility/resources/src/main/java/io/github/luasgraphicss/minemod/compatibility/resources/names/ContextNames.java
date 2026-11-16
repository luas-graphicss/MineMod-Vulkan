package io.github.luasgraphicss.minemod.compatibility.resources.names;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectScope;

public final class ContextNames {
    private final NameSpace shared;
    private final NameSpace local;

    public ContextNames(NameSpace shared, NameSpace local) {
        if (shared.scope() != ObjectScope.SHARE_GROUP) {
            throw new IllegalArgumentException("shared names must be kept per share group, not per " + shared.scope());
        }
        if (local.scope() != ObjectScope.CONTEXT) {
            throw new IllegalArgumentException("local names must be kept per context, not per " + local.scope());
        }
        this.shared = shared;
        this.local = local;
    }

    public NameTable table(ObjectKind kind) {
        return kind.scope() == ObjectScope.SHARE_GROUP ? shared.table(kind) : local.table(kind);
    }
}

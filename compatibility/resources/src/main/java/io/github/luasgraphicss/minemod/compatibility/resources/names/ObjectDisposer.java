package io.github.luasgraphicss.minemod.compatibility.resources.names;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;

public interface ObjectDisposer {
    void dispose(ObjectKind kind, int backing);
}

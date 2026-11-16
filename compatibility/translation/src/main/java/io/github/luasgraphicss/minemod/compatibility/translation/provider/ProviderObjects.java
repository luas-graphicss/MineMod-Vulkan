package io.github.luasgraphicss.minemod.compatibility.translation.provider;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ObjectDisposer;

public interface ProviderObjects extends ObjectDisposer {
    int create(ObjectKind kind, int parameter);
}

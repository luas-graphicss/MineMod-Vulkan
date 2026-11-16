package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;

public interface RendererResources {
    long create(PlannedShare share);

    long allocationSize(long resource);

    long exportHandle(long resource);

    void destroy(long resource);
}

package io.github.luasgraphicss.minemod.compatibility.interop.resource;

import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;

public interface SharedResourceBackend {
    long createInRenderer(PlannedShare share);

    long exportHandle(long rendererResource);

    int importIntoProvider(PlannedShare share, long handle);

    void destroyInProvider(int providerObject);

    void destroyInRenderer(long rendererResource);
}

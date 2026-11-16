package io.github.luasgraphicss.minemod.compatibility.interop.resource;

import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;

public final class SharedResource {
    private final PlannedShare share;
    private final long rendererResource;
    private final int providerObject;

    SharedResource(PlannedShare share, long rendererResource, int providerObject) {
        this.share = share;
        this.rendererResource = rendererResource;
        this.providerObject = providerObject;
    }

    public PlannedShare share() {
        return share;
    }

    public long rendererResource() {
        return rendererResource;
    }

    public int providerObject() {
        return providerObject;
    }
}

package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import io.github.luasgraphicss.minemod.compatibility.interop.resource.SharedResourceBackend;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SplitResourceBackend implements SharedResourceBackend {
    private final Map<Long, Long> allocationSizesByHandle = new HashMap<>();
    private final RendererResources renderer;
    private final ProviderMemoryImports provider;

    public SplitResourceBackend(RendererResources renderer, ProviderMemoryImports provider) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public long createInRenderer(PlannedShare share) {
        return renderer.create(share);
    }

    @Override
    public long exportHandle(long rendererResource) {
        long handle = renderer.exportHandle(rendererResource);
        allocationSizesByHandle.put(handle, renderer.allocationSize(rendererResource));
        return handle;
    }

    @Override
    public int importIntoProvider(PlannedShare share, long handle) {
        Long allocationSize = allocationSizesByHandle.remove(handle);
        if (allocationSize == null) {
            throw new IllegalArgumentException("handle " + handle + " was not exported by the renderer");
        }
        return provider.importMemory(share.request(), allocationSize, handle);
    }

    @Override
    public void destroyInProvider(int providerObject) {
        provider.release(providerObject);
    }

    @Override
    public void destroyInRenderer(long rendererResource) {
        renderer.destroy(rendererResource);
    }
}

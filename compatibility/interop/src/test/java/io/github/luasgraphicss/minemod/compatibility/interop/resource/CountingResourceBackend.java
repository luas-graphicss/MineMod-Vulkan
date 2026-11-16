package io.github.luasgraphicss.minemod.compatibility.interop.resource;

import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleCloser;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import java.util.HashSet;
import java.util.Set;

final class CountingResourceBackend implements SharedResourceBackend, HandleCloser {
    private final boolean importTakesOwnership;
    private final Set<Long> rendererResources = new HashSet<>();
    private final Set<Integer> providerObjects = new HashSet<>();
    private final Set<Long> openHandles = new HashSet<>();
    private long nextRendererResource = 1;
    private long nextHandle = 1000;
    private int nextProviderObject = 1;
    private boolean failingExport;
    private boolean failingImport;

    CountingResourceBackend(boolean importTakesOwnership) {
        this.importTakesOwnership = importTakesOwnership;
    }

    @Override
    public long createInRenderer(PlannedShare share) {
        long resource = nextRendererResource++;
        rendererResources.add(resource);
        return resource;
    }

    @Override
    public long exportHandle(long rendererResource) {
        if (failingExport) {
            throw new IllegalStateException("the driver refused to export the memory");
        }
        long handle = nextHandle++;
        openHandles.add(handle);
        return handle;
    }

    @Override
    public int importIntoProvider(PlannedShare share, long handle) {
        if (failingImport) {
            throw new IllegalStateException("the provider refused to import the memory");
        }
        if (importTakesOwnership) {
            openHandles.remove(handle);
        }
        int object = nextProviderObject++;
        providerObjects.add(object);
        return object;
    }

    @Override
    public void destroyInProvider(int providerObject) {
        if (!providerObjects.remove(providerObject)) {
            throw new IllegalStateException("provider object destroyed twice: " + providerObject);
        }
    }

    @Override
    public void destroyInRenderer(long rendererResource) {
        if (!rendererResources.remove(rendererResource)) {
            throw new IllegalStateException("renderer resource destroyed twice: " + rendererResource);
        }
    }

    @Override
    public void close(long handle) {
        if (!openHandles.remove(handle)) {
            throw new IllegalStateException("handle closed twice: " + handle);
        }
    }

    void failExports(boolean failing) {
        failingExport = failing;
    }

    void failImports(boolean failing) {
        failingImport = failing;
    }

    int rendererResourceCount() {
        return rendererResources.size();
    }

    int providerObjectCount() {
        return providerObjects.size();
    }

    int openHandleCount() {
        return openHandles.size();
    }
}

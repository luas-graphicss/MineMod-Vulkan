package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.interop.handle.HandleCloser;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupport;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ExternalSupportQuery;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.PlannedShare;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.interop.resource.SharedResourceBackend;
import java.util.HashSet;
import java.util.Set;

public final class SimulatedResources implements SharedResourceBackend, HandleCloser, ExternalSupportQuery {
    private static final int MAX_EXTENT = 16384;
    private static final int MAX_MIP_LEVELS = 15;
    private static final long MAX_BUFFER_BYTES = 1L << 30;
    private static final int SINGLE_SAMPLE_ONLY = 1;

    private final HandlePlatform platform;
    private final Set<Long> rendererResources = new HashSet<>();
    private final Set<Integer> providerObjects = new HashSet<>();
    private final Set<Long> openHandles = new HashSet<>();
    private long nextRendererResource = 1;
    private long nextHandle = 1;
    private int nextProviderObject = 1;
    private boolean failingImports;

    public SimulatedResources(HandlePlatform platform) {
        this.platform = platform;
    }

    @Override
    public long createInRenderer(PlannedShare share) {
        long resource = nextRendererResource++;
        rendererResources.add(resource);
        return resource;
    }

    @Override
    public long exportHandle(long rendererResource) {
        long handle = nextHandle++;
        openHandles.add(handle);
        return handle;
    }

    @Override
    public int importIntoProvider(PlannedShare share, long handle) {
        if (failingImports) {
            throw new IllegalStateException("the provider refused to import the memory");
        }
        if (platform.importTakesOwnership()) {
            openHandles.remove(handle);
        }
        int object = nextProviderObject++;
        providerObjects.add(object);
        return object;
    }

    @Override
    public void destroyInProvider(int providerObject) {
        requireRemoved(providerObjects.remove(providerObject), "provider object " + providerObject);
    }

    @Override
    public void destroyInRenderer(long rendererResource) {
        requireRemoved(rendererResources.remove(rendererResource), "renderer resource " + rendererResource);
    }

    @Override
    public void close(long handle) {
        requireRemoved(openHandles.remove(handle), "handle " + handle);
    }

    @Override
    public ExternalSupport supportFor(SharingRequest request) {
        return request.resource() == ShareableResource.BUFFER
                ? ExternalSupport.shareable(false, MAX_BUFFER_BYTES, 1, 1, SINGLE_SAMPLE_ONLY)
                : ExternalSupport.shareable(true, MAX_EXTENT, MAX_EXTENT, MAX_MIP_LEVELS, SINGLE_SAMPLE_ONLY);
    }

    @Override
    public boolean binarySemaphoresShareable() {
        return true;
    }

    public void failImports(boolean failing) {
        failingImports = failing;
    }

    public int leftoverCount() {
        return rendererResources.size() + providerObjects.size() + openHandles.size();
    }

    private static void requireRemoved(boolean removed, String what) {
        if (!removed) {
            throw new IllegalStateException(what + " was released twice");
        }
    }
}

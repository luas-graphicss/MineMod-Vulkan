package io.github.luasgraphicss.minemod.compatibility.interop.handoff;

import io.github.luasgraphicss.minemod.compatibility.interop.semaphore.HandoffSemaphore;
import io.github.luasgraphicss.minemod.compatibility.state.submission.AttachmentHandoff;
import java.util.Objects;

public final class SharedAttachmentHandoff implements AttachmentHandoff {
    private final SharingBackend backend;
    private final HandoffSemaphore toProvider = new HandoffSemaphore("renderer-to-provider");
    private final HandoffSemaphore toRenderer = new HandoffSemaphore("provider-to-renderer");

    public SharedAttachmentHandoff(SharingBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    @Override
    public void releaseToProvider() {
        if (!toProvider.isSignalPending()) {
            backend.submitRendererWorkAndSignal();
            toProvider.signaled();
        }
        backend.waitInProvider();
        toProvider.waited();
    }

    @Override
    public void reclaimForRenderer() {
        if (!toRenderer.isSignalPending()) {
            backend.signalAndFlushProvider();
            toRenderer.signaled();
        }
        backend.waitAndReacquireInRenderer();
        toRenderer.waited();
    }

    @Override
    public void finishProviderWork() {
        backend.finishProvider();
    }

    public long completedRoundTrips() {
        return toRenderer.completedCycles();
    }

    public boolean hasPendingSignal() {
        return toProvider.isSignalPending() || toRenderer.isSignalPending();
    }
}

package io.github.luasgraphicss.minemod.client.compatibility.handoff;

import io.github.luasgraphicss.minemod.bridge.SceneSharing;
import io.github.luasgraphicss.minemod.bridge.sharing.HandoffSynchronization;
import io.github.luasgraphicss.minemod.compatibility.layer.sharing.RendererHandoff;
import java.util.Objects;

public final class SessionHandoff implements RendererHandoff {
    private final SceneSharing sharing;
    private final HandoffSynchronization synchronization;

    public SessionHandoff(SceneSharing sharing, HandoffSynchronization synchronization) {
        this.sharing = Objects.requireNonNull(sharing, "sharing");
        this.synchronization = Objects.requireNonNull(synchronization, "synchronization");
    }

    @Override
    public void submitWorkAndSignal() {
        sharing.releaseToProvider(synchronization);
    }

    @Override
    public void waitAndReacquire() {
        sharing.reclaimForRenderer();
    }
}

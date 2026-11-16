package io.github.luasgraphicss.minemod.compatibility.bridges.session;

import io.github.luasgraphicss.minemod.compatibility.spi.BridgeContext;
import io.github.luasgraphicss.minemod.compatibility.spi.SceneAttachments;
import java.nio.ByteBuffer;

final class ScopedBridgeContext implements BridgeContext {
    private final String bridgeId;
    private final BridgeHost host;
    private boolean revoked;

    ScopedBridgeContext(String bridgeId, BridgeHost host) {
        this.bridgeId = bridgeId;
        this.host = host;
    }

    @Override
    public synchronized SceneAttachments attachments() {
        requireActive();
        return host.attachments();
    }

    @Override
    public synchronized void submit(ByteBuffer commandStream) {
        requireActive();
        host.submit(bridgeId, commandStream.asReadOnlyBuffer());
    }

    synchronized void revoke() {
        revoked = true;
    }

    private void requireActive() {
        if (revoked) {
            throw new IllegalStateException("bridge " + bridgeId + " was detached and can no longer reach the renderer");
        }
    }
}

package io.github.luasgraphicss.minemod.bridge.sharing;

public final class HandoffSemaphores {
    private final long rendererDone;
    private final long providerDone;

    public HandoffSemaphores(long rendererDone, long providerDone) {
        if (rendererDone == providerDone) {
            throw new IllegalArgumentException("a handoff needs one semaphore for each direction, found " + rendererDone);
        }
        this.rendererDone = rendererDone;
        this.providerDone = providerDone;
    }

    public long rendererDone() {
        return rendererDone;
    }

    public long providerDone() {
        return providerDone;
    }
}

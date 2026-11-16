package io.github.luasgraphicss.minemod.compatibility.interop.handle;

import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class HandleLedger {
    private final HandlePlatform platform;
    private final HandleCloser closer;
    private final Set<Long> owned = new HashSet<>();

    public HandleLedger(HandlePlatform platform, HandleCloser closer) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.closer = Objects.requireNonNull(closer, "closer");
    }

    public synchronized void exported(long handle) {
        if (!owned.add(handle)) {
            throw new IllegalStateException("handle " + handle + " was already exported and is still open");
        }
    }

    public void imported(long handle) {
        release(handle);
        if (!platform.importTakesOwnership()) {
            closer.close(handle);
        }
    }

    public void abandoned(long handle) {
        release(handle);
        closer.close(handle);
    }

    public synchronized int ownedCount() {
        return owned.size();
    }

    private synchronized void release(long handle) {
        if (!owned.remove(handle)) {
            throw new IllegalStateException("handle " + handle + " is not owned here, closing it would close it twice");
        }
    }
}

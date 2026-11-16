package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.contexts.ContextBackend;
import java.util.HashSet;
import java.util.Set;

public final class SimulatedContexts implements ContextBackend {
    private final Set<Long> live = new HashSet<>();
    private long nextHandle = 1;

    @Override
    public synchronized long create(long sharedWith) {
        if (sharedWith != NO_SHARED_CONTEXT && !live.contains(sharedWith)) {
            throw new IllegalArgumentException("cannot share with a context that is not live: " + sharedWith);
        }
        long handle = nextHandle++;
        live.add(handle);
        return handle;
    }

    @Override
    public synchronized void makeCurrent(long handle) {
        if (!live.contains(handle)) {
            throw new IllegalArgumentException("cannot make a context current that is not live: " + handle);
        }
    }

    @Override
    public void releaseCurrent() {
    }

    @Override
    public synchronized void destroy(long handle) {
        if (!live.remove(handle)) {
            throw new IllegalStateException("context destroyed twice: " + handle);
        }
    }

    public synchronized int liveCount() {
        return live.size();
    }
}

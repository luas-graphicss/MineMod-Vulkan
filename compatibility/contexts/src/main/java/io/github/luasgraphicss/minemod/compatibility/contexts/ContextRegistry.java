package io.github.luasgraphicss.minemod.compatibility.contexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ContextRegistry {
    private final ContextBackend backend;
    private final int nameCapacityPerKind;
    private final List<CompatibilityContext> contexts = new ArrayList<>();
    private final Map<Thread, CompatibilityContext> currentByThread = new HashMap<>();
    private int nextContextId = 1;
    private int nextShareGroupId = 1;
    private int liveShareGroupCount;

    public ContextRegistry(ContextBackend backend, int nameCapacityPerKind) {
        if (nameCapacityPerKind < 1) {
            throw new IllegalArgumentException("a context needs room for at least one name: " + nameCapacityPerKind);
        }
        this.backend = Objects.requireNonNull(backend, "backend");
        this.nameCapacityPerKind = nameCapacityPerKind;
    }

    public synchronized CompatibilityContext create() {
        long handle = backend.create(ContextBackend.NO_SHARED_CONTEXT);
        ShareGroup shareGroup = new ShareGroup(nextShareGroupId++, nameCapacityPerKind);
        liveShareGroupCount++;
        return register(handle, shareGroup);
    }

    public synchronized CompatibilityContext createSharingWith(CompatibilityContext existing) {
        requireUsable(existing);
        long handle = backend.create(existing.handle());
        return register(handle, existing.shareGroup());
    }

    public synchronized void makeCurrent(CompatibilityContext context) {
        requireUsable(context);
        Thread caller = Thread.currentThread();
        Thread owner = context.currentThread();
        if (owner == caller) {
            return;
        }
        if (owner != null) {
            throw new IllegalStateException(context + " is current on thread " + owner.getName());
        }
        CompatibilityContext previous = currentByThread.get(caller);
        backend.makeCurrent(context.handle());
        context.attachTo(caller);
        currentByThread.put(caller, context);
        if (previous != null) {
            released(previous);
        }
    }

    public synchronized void releaseCurrent() {
        Thread caller = Thread.currentThread();
        CompatibilityContext current = currentByThread.get(caller);
        if (current == null) {
            return;
        }
        backend.releaseCurrent();
        currentByThread.remove(caller);
        released(current);
    }

    public synchronized Optional<CompatibilityContext> current() {
        return Optional.ofNullable(currentByThread.get(Thread.currentThread()));
    }

    public synchronized CompatibilityContext requireCurrent() {
        Thread caller = Thread.currentThread();
        CompatibilityContext current = currentByThread.get(caller);
        if (current == null) {
            throw new IllegalStateException("no compatibility context is current on thread " + caller.getName());
        }
        return current;
    }

    public synchronized void destroy(CompatibilityContext context) {
        requireUsable(context);
        Thread caller = Thread.currentThread();
        if (context.currentThread() == caller) {
            backend.releaseCurrent();
            currentByThread.remove(caller);
            context.detach();
        }
        if (context.currentThread() != null) {
            context.requestDestroy();
            return;
        }
        finishDestroying(context);
    }

    public synchronized void shutdown() {
        Thread caller = Thread.currentThread();
        List<CompatibilityContext> heldElsewhere = new ArrayList<>();
        for (CompatibilityContext context : contexts) {
            if (context.currentThread() != null && context.currentThread() != caller) {
                heldElsewhere.add(context);
            }
        }
        if (!heldElsewhere.isEmpty()) {
            throw new IllegalStateException("contexts are still current on other threads: " + heldElsewhere);
        }
        for (CompatibilityContext context : new ArrayList<>(contexts)) {
            if (context.currentThread() == caller) {
                backend.releaseCurrent();
                currentByThread.remove(caller);
                context.detach();
            }
            finishDestroying(context);
        }
    }

    public synchronized boolean isLive(CompatibilityContext context) {
        return !context.isDestroyed();
    }

    public synchronized int liveContextCount() {
        return contexts.size();
    }

    public synchronized int liveShareGroupCount() {
        return liveShareGroupCount;
    }

    public synchronized int liveObjectCount() {
        Map<ShareGroup, Boolean> countedGroups = new IdentityHashMap<>();
        int count = 0;
        for (CompatibilityContext context : contexts) {
            count += context.localNames().liveCount();
            if (countedGroups.put(context.shareGroup(), Boolean.TRUE) == null) {
                count += context.shareGroup().sharedNames().liveCount();
            }
        }
        return count;
    }

    private CompatibilityContext register(long handle, ShareGroup shareGroup) {
        CompatibilityContext context =
                new CompatibilityContext(nextContextId++, handle, shareGroup, nameCapacityPerKind);
        shareGroup.join();
        contexts.add(context);
        return context;
    }

    private void released(CompatibilityContext context) {
        context.detach();
        if (context.isDestroyRequested()) {
            finishDestroying(context);
        }
    }

    private void finishDestroying(CompatibilityContext context) {
        backend.destroy(context.handle());
        context.markDestroyed();
        contexts.remove(context);
        if (context.shareGroup().leave()) {
            liveShareGroupCount--;
        }
    }

    private static void requireUsable(CompatibilityContext context) {
        if (context.isDestroyed() || context.isDestroyRequested()) {
            throw new IllegalStateException(context + " was destroyed");
        }
    }
}

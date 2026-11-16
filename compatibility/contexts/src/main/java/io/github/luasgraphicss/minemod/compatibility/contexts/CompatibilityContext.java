package io.github.luasgraphicss.minemod.compatibility.contexts;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectScope;
import io.github.luasgraphicss.minemod.compatibility.resources.names.ContextNames;
import io.github.luasgraphicss.minemod.compatibility.resources.names.NameSpace;

public final class CompatibilityContext {
    private final int id;
    private final long handle;
    private final ShareGroup shareGroup;
    private final NameSpace localNames;
    private final ContextNames names;
    private Thread currentThread;
    private boolean destroyRequested;
    private boolean destroyed;

    CompatibilityContext(int id, long handle, ShareGroup shareGroup, int nameCapacityPerKind) {
        this.id = id;
        this.handle = handle;
        this.shareGroup = shareGroup;
        this.localNames = new NameSpace(ObjectScope.CONTEXT, nameCapacityPerKind);
        this.names = new ContextNames(shareGroup.sharedNames(), localNames);
    }

    public int id() {
        return id;
    }

    public ShareGroup shareGroup() {
        return shareGroup;
    }

    public ContextNames names() {
        return names;
    }

    long handle() {
        return handle;
    }

    NameSpace localNames() {
        return localNames;
    }

    Thread currentThread() {
        return currentThread;
    }

    void attachTo(Thread thread) {
        currentThread = thread;
    }

    void detach() {
        currentThread = null;
    }

    boolean isDestroyRequested() {
        return destroyRequested;
    }

    void requestDestroy() {
        destroyRequested = true;
    }

    boolean isDestroyed() {
        return destroyed;
    }

    void markDestroyed() {
        destroyed = true;
        localNames.discardAll();
    }

    @Override
    public String toString() {
        return "context " + id;
    }
}

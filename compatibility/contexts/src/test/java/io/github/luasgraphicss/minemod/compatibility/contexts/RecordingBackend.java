package io.github.luasgraphicss.minemod.compatibility.contexts;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

final class RecordingBackend implements ContextBackend {
    private final Map<Long, Long> shareSources = new HashMap<>();
    private final Map<Thread, Long> currentByThread = new HashMap<>();
    private long nextHandle = 1;
    private boolean refusingCreation;

    @Override
    public synchronized long create(long sharedWith) {
        if (refusingCreation) {
            throw new IllegalStateException("the provider refused to create a context");
        }
        if (sharedWith != NO_SHARED_CONTEXT && !shareSources.containsKey(sharedWith)) {
            throw new IllegalArgumentException("cannot share with a context that is not live: " + sharedWith);
        }
        long handle = nextHandle++;
        shareSources.put(handle, sharedWith);
        return handle;
    }

    @Override
    public synchronized void makeCurrent(long handle) {
        if (!shareSources.containsKey(handle)) {
            throw new IllegalArgumentException("cannot make a context current that is not live: " + handle);
        }
        currentByThread.put(Thread.currentThread(), handle);
    }

    @Override
    public synchronized void releaseCurrent() {
        currentByThread.remove(Thread.currentThread());
    }

    @Override
    public synchronized void destroy(long handle) {
        if (currentByThread.containsValue(handle)) {
            throw new IllegalStateException("a context was destroyed while current: " + handle);
        }
        if (shareSources.remove(handle) == null) {
            throw new IllegalArgumentException("cannot destroy a context that is not live: " + handle);
        }
    }

    synchronized void refuseCreation() {
        refusingCreation = true;
    }

    synchronized int liveCount() {
        return shareSources.size();
    }

    synchronized long shareSourceOf(long handle) {
        return shareSources.get(handle);
    }

    synchronized OptionalLong currentOnCallingThread() {
        Long handle = currentByThread.get(Thread.currentThread());
        return handle == null ? OptionalLong.empty() : OptionalLong.of(handle);
    }
}

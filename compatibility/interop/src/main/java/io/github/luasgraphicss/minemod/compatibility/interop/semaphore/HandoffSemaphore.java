package io.github.luasgraphicss.minemod.compatibility.interop.semaphore;

import java.util.Objects;

public final class HandoffSemaphore {
    private final String name;
    private boolean signalPending;
    private long completedCycles;

    public HandoffSemaphore(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public void signaled() {
        if (signalPending) {
            throw new IllegalStateException("the " + name + " semaphore was signaled twice without a wait");
        }
        signalPending = true;
    }

    public void waited() {
        if (!signalPending) {
            throw new IllegalStateException("nothing signals the " + name + " semaphore, the wait would not return");
        }
        signalPending = false;
        completedCycles++;
    }

    public boolean isSignalPending() {
        return signalPending;
    }

    public long completedCycles() {
        return completedCycles;
    }
}

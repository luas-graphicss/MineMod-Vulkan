package io.github.luasgraphicss.minemod.contract.handle;

public final class HandleAllocator {
    private final ResourceKind kind;
    private final int[] generations;
    private final boolean[] live;
    private final int[] recycledSlots;
    private int recycledCount;
    private int untouchedSlot;
    private int liveCount;

    public HandleAllocator(ResourceKind kind, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("handle capacity must be positive: " + capacity);
        }
        this.kind = kind;
        this.generations = new int[capacity];
        this.live = new boolean[capacity];
        this.recycledSlots = new int[capacity];
    }

    public ResourceHandle allocate() {
        int slot;
        if (recycledCount > 0) {
            slot = recycledSlots[--recycledCount];
        } else if (untouchedSlot < generations.length) {
            slot = untouchedSlot++;
        } else {
            throw new IllegalStateException("no free " + kind + " handle among " + generations.length + " slots");
        }
        generations[slot]++;
        live[slot] = true;
        liveCount++;
        return ResourceHandle.of(kind, generations[slot], slot);
    }

    public void release(ResourceHandle handle) {
        if (!isLive(handle)) {
            throw new IllegalArgumentException("handle is not live in this allocator: " + handle);
        }
        int slot = handle.index();
        live[slot] = false;
        liveCount--;
        if (generations[slot] < ResourceHandle.MAX_GENERATION) {
            recycledSlots[recycledCount++] = slot;
        }
    }

    public boolean isLive(ResourceHandle handle) {
        int slot = handle.index();
        return handle.kind() == kind
                && slot < generations.length
                && live[slot]
                && generations[slot] == handle.generation();
    }

    public int liveCount() {
        return liveCount;
    }
}

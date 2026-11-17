package io.github.luasgraphicss.minemod.compatibility.layer.memory;

import java.util.Objects;

public final class ScratchInts implements AutoCloseable {
    private final NativeMemory memory;
    private final int capacity;
    private long address;

    public ScratchInts(NativeMemory memory, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("a scratch block holds at least one value");
        }
        this.memory = Objects.requireNonNull(memory, "memory");
        this.capacity = capacity;
        this.address = memory.allocate(capacity * Integer.BYTES);
    }

    public long address() {
        if (address == 0) {
            throw new IllegalStateException("the scratch block was already released");
        }
        return address;
    }

    public int capacity() {
        return capacity;
    }

    public long holding(int value) {
        memory.writeInt(address(), value);
        return address;
    }

    public int first() {
        return memory.readInt(address());
    }

    @Override
    public void close() {
        if (address != 0) {
            memory.free(address);
            address = 0;
        }
    }
}

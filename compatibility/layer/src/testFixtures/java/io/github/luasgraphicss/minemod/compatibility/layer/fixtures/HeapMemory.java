package io.github.luasgraphicss.minemod.compatibility.layer.fixtures;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

public final class HeapMemory implements NativeMemory {
    private static final long FIRST_ADDRESS = 0x1000;
    private static final int CAPACITY = 1 << 20;
    private static final int ALIGNMENT = Long.BYTES;

    private final ByteBuffer bytes = ByteBuffer.allocate(CAPACITY).order(ByteOrder.nativeOrder());
    private final Set<Long> live = new HashSet<>();
    private int used;

    @Override
    public long allocate(int size) {
        int aligned = (size + ALIGNMENT - 1) / ALIGNMENT * ALIGNMENT;
        if (size < 1 || aligned > CAPACITY - used) {
            throw new IllegalArgumentException("the test heap cannot provide " + size + " bytes");
        }
        long address = FIRST_ADDRESS + used;
        used += aligned;
        live.add(address);
        return address;
    }

    @Override
    public void free(long address) {
        if (!live.remove(address)) {
            throw new IllegalArgumentException("address " + address + " is not a live allocation");
        }
    }

    @Override
    public int readInt(long address) {
        return bytes.getInt(offsetOf(address));
    }

    @Override
    public void writeInt(long address, int value) {
        bytes.putInt(offsetOf(address), value);
    }

    @Override
    public long readLong(long address) {
        return bytes.getLong(offsetOf(address));
    }

    @Override
    public void writeLong(long address, long value) {
        bytes.putLong(offsetOf(address), value);
    }

    @Override
    public byte readByte(long address) {
        return bytes.get(offsetOf(address));
    }

    @Override
    public void writeByte(long address, byte value) {
        bytes.put(offsetOf(address), value);
    }

    @Override
    public void writeBytes(long address, byte[] source, int offset, int count) {
        ByteBuffer destination = bytes.duplicate();
        destination.position(offsetOf(address));
        destination.put(source, offset, count);
    }

    public byte[] readBytes(long address, int count) {
        byte[] copy = new byte[count];
        ByteBuffer source = bytes.duplicate();
        source.position(offsetOf(address));
        source.get(copy);
        return copy;
    }

    public int liveAllocations() {
        return live.size();
    }

    private int offsetOf(long address) {
        long offset = address - FIRST_ADDRESS;
        if (offset < 0 || offset >= used) {
            throw new IllegalArgumentException("address " + address + " lies outside every allocation");
        }
        return (int) offset;
    }
}

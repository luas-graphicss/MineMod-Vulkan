package io.github.luasgraphicss.minemod.compatibility.runtime.fixtures;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.natives.memory.JniMemory;
import java.nio.charset.StandardCharsets;

public final class NativeBlock implements AutoCloseable {
    private final NativeMemory memory = new JniMemory();
    private final long address;
    private final int size;

    public NativeBlock(int size) {
        this.size = size;
        this.address = memory.allocate(size);
    }

    public static NativeBlock ofFloats(float... values) {
        NativeBlock block = new NativeBlock(values.length * Float.BYTES);
        for (int index = 0; index < values.length; index++) {
            block.memory.writeInt(block.address + (long) index * Float.BYTES, Float.floatToRawIntBits(values[index]));
        }
        return block;
    }

    public static NativeBlock ofText(String text) {
        byte[] characters = text.getBytes(StandardCharsets.US_ASCII);
        NativeBlock block = new NativeBlock(characters.length + 1);
        block.setBytes(characters);
        return block;
    }

    public long address() {
        return address;
    }

    public int size() {
        return size;
    }

    public void fill(int value) {
        for (int index = 0; index < size; index++) {
            memory.writeByte(address + index, (byte) value);
        }
    }

    public void setBytes(byte[] values) {
        for (int index = 0; index < values.length; index++) {
            memory.writeByte(address + index, values[index]);
        }
    }

    public void setInt(int index, int value) {
        memory.writeInt(address + (long) index * Integer.BYTES, value);
    }

    public void setLong(int index, long value) {
        memory.writeLong(address + (long) index * Long.BYTES, value);
    }

    public int intAt(int index) {
        return memory.readInt(address + (long) index * Integer.BYTES);
    }

    public int byteAt(int index) {
        return memory.readByte(address + index) & 0xFF;
    }

    public byte[] bytes() {
        byte[] values = new byte[size];
        for (int index = 0; index < size; index++) {
            values[index] = memory.readByte(address + index);
        }
        return values;
    }

    @Override
    public void close() {
        memory.free(address);
    }
}

package io.github.luasgraphicss.minemod.compatibility.natives.memory;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.natives.binding.NativeBinding;

public final class JniMemory implements NativeMemory {
    @Override
    public long allocate(int bytes) {
        if (bytes < 1) {
            throw new IllegalArgumentException("an allocation holds at least one byte");
        }
        long address = NativeBinding.allocate(bytes);
        if (address == 0) {
            throw new OutOfMemoryError("the native heap could not provide " + bytes + " bytes");
        }
        return address;
    }

    @Override
    public void free(long address) {
        NativeBinding.free(address);
    }

    @Override
    public int readInt(long address) {
        return NativeBinding.readInt(address);
    }

    @Override
    public void writeInt(long address, int value) {
        NativeBinding.writeInt(address, value);
    }

    @Override
    public long readLong(long address) {
        return NativeBinding.readLong(address);
    }

    @Override
    public void writeLong(long address, long value) {
        NativeBinding.writeLong(address, value);
    }

    @Override
    public byte readByte(long address) {
        return NativeBinding.readByte(address);
    }

    @Override
    public void writeByte(long address, byte value) {
        NativeBinding.writeByte(address, value);
    }

    @Override
    public void writeBytes(long address, byte[] source, int offset, int count) {
        NativeBinding.writeBytes(address, source, offset, count);
    }
}

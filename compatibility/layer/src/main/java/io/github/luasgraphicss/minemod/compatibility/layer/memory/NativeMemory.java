package io.github.luasgraphicss.minemod.compatibility.layer.memory;

public interface NativeMemory {
    long allocate(int bytes);

    void free(long address);

    int readInt(long address);

    void writeInt(long address, int value);

    long readLong(long address);

    void writeLong(long address, long value);

    byte readByte(long address);

    void writeByte(long address, byte value);
}

package io.github.luasgraphicss.minemod.adapters.frame.arrays;

import java.nio.ByteBuffer;

public final class ArrayPointer {
    private static final int NO_BUFFER_OBJECT = 0;

    private final int size;
    private final int type;
    private final int stride;
    private final ByteBuffer memory;
    private final int bufferObject;
    private final long offset;

    private ArrayPointer(int size, int type, int stride, ByteBuffer memory, int bufferObject, long offset) {
        this.size = size;
        this.type = type;
        this.stride = stride;
        this.memory = memory;
        this.bufferObject = bufferObject;
        this.offset = offset;
    }

    public static ArrayPointer inMemory(int size, int type, int stride, ByteBuffer memory) {
        return new ArrayPointer(size, type, stride, memory, NO_BUFFER_OBJECT, memory.position());
    }

    public static ArrayPointer inBufferObject(int size, int type, int stride, int bufferObject, long offset) {
        return new ArrayPointer(size, type, stride, null, bufferObject, offset);
    }

    int size() {
        return size;
    }

    int type() {
        return type;
    }

    int stride() {
        return stride;
    }

    ByteBuffer memory() {
        return memory;
    }

    int bufferObject() {
        return bufferObject;
    }

    long offset() {
        return offset;
    }

    boolean sharesSourceWith(ArrayPointer other) {
        return memory == other.memory && bufferObject == other.bufferObject;
    }
}

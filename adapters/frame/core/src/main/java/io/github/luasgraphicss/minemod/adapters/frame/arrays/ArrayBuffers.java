package io.github.luasgraphicss.minemod.adapters.frame.arrays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ArrayBuffers {
    private static final int ARRAY_BUFFER = 0x8892;
    private static final int NO_BUFFER = 0;
    private static final long MAX_BYTES = 256L * 1024 * 1024;

    private final Map<Integer, ByteBuffer> contents = new HashMap<>();
    private int lastName;
    private int boundArray;

    public int generate() {
        return ++lastName;
    }

    public void bind(int target, int buffer) {
        if (target == ARRAY_BUFFER) {
            boundArray = Math.max(NO_BUFFER, buffer);
        }
    }

    public int boundArray() {
        return boundArray;
    }

    public void store(int target, ByteBuffer source) {
        if (target != ARRAY_BUFFER || boundArray == NO_BUFFER) {
            return;
        }
        if (source.remaining() > MAX_BYTES) {
            contents.remove(boundArray);
            return;
        }
        ByteBuffer kept = ByteBuffer.allocateDirect(source.remaining()).order(ByteOrder.nativeOrder());
        kept.put(source.duplicate());
        kept.flip();
        contents.put(boundArray, kept);
    }

    public Optional<ByteBuffer> contentsOf(int buffer) {
        ByteBuffer kept = contents.get(buffer);
        return kept == null ? Optional.empty() : Optional.of(kept.duplicate().order(ByteOrder.nativeOrder()));
    }

    public void delete(int buffer) {
        contents.remove(buffer);
        if (boundArray == buffer) {
            boundArray = NO_BUFFER;
        }
    }
}

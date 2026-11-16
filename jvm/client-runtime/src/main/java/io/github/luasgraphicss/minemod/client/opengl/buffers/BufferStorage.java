package io.github.luasgraphicss.minemod.client.opengl.buffers;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BufferStorage {
    private static final long MAX_MAPPED_BYTES = 256L * 1024 * 1024;

    private final Map<Integer, Long> sizes = new HashMap<>();
    private int lastName;

    public int generate() {
        return ++lastName;
    }

    public void resize(int target, long bytes) {
        sizes.put(target, bytes);
    }

    public Optional<ByteBuffer> map(int target) {
        Long bytes = sizes.get(target);
        if (bytes == null || bytes < 0 || bytes > MAX_MAPPED_BYTES) {
            return Optional.empty();
        }
        return Optional.of(ByteBuffer.allocateDirect(bytes.intValue()).order(ByteOrder.nativeOrder()));
    }
}

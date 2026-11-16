package io.github.luasgraphicss.minemod.client.opengl.buffers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class BufferStorageTest {
    private static final int ELEMENT_ARRAY_BUFFER = 0x8893;
    private static final int ARRAY_BUFFER = 0x8892;

    private final BufferStorage buffers = new BufferStorage();

    @Test
    void mapsTheBytesTheGameSized() {
        buffers.resize(ELEMENT_ARRAY_BUFFER, 24);

        ByteBuffer mapped = buffers.map(ELEMENT_ARRAY_BUFFER).get();

        assertEquals(24, mapped.remaining());
        assertEquals(ByteOrder.nativeOrder(), mapped.order());
    }

    @Test
    void mapsNothingForATargetWithoutStorage() {
        buffers.resize(ELEMENT_ARRAY_BUFFER, 24);

        assertFalse(buffers.map(ARRAY_BUFFER).isPresent());
    }

    @Test
    void mapsNothingForStorageBeyondItsLimit() {
        buffers.resize(ELEMENT_ARRAY_BUFFER, Long.MAX_VALUE);

        assertFalse(buffers.map(ELEMENT_ARRAY_BUFFER).isPresent());
    }

    @Test
    void namesEveryBufferDifferently() {
        assertNotEquals(buffers.generate(), buffers.generate());
    }
}

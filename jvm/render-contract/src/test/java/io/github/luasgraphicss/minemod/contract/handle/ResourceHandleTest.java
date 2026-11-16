package io.github.luasgraphicss.minemod.contract.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ResourceHandleTest {
    @Test
    void packsKindGenerationAndIndexLikeTheNativeHeader() {
        ResourceHandle handle = ResourceHandle.of(ResourceKind.BUFFER, 7, 42);

        assertEquals(0x010000070000002AL, handle.bits());
        assertEquals(ResourceKind.BUFFER, handle.kind());
        assertEquals(7, handle.generation());
        assertEquals(42, handle.index());
    }

    @Test
    void roundTripsThroughItsBits() {
        ResourceHandle handle = ResourceHandle.of(ResourceKind.BUFFER, ResourceHandle.MAX_GENERATION, Integer.MAX_VALUE);

        assertEquals(handle, ResourceHandle.fromBits(handle.bits()));
    }

    @Test
    void rejectsBitsThatNameNoResource() {
        assertThrows(IllegalArgumentException.class, () -> ResourceHandle.fromBits(0L));
        assertThrows(IllegalArgumentException.class, () -> ResourceHandle.fromBits(0x0100000000000001L));
        assertThrows(IllegalArgumentException.class, () -> ResourceHandle.fromBits(0x7F00000100000001L));
    }

    @Test
    void rejectsGenerationsOutsideTheLayout() {
        assertThrows(IllegalArgumentException.class, () -> ResourceHandle.of(ResourceKind.BUFFER, 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> ResourceHandle.of(ResourceKind.BUFFER, ResourceHandle.MAX_GENERATION + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> ResourceHandle.of(ResourceKind.BUFFER, 1, -1));
    }
}

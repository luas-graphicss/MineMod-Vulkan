package io.github.luasgraphicss.minemod.contract.handle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HandleAllocatorTest {
    @Test
    void allocatesDistinctLiveHandles() {
        HandleAllocator allocator = new HandleAllocator(ResourceKind.BUFFER, 4);

        ResourceHandle first = allocator.allocate();
        ResourceHandle second = allocator.allocate();

        assertNotEquals(first, second);
        assertTrue(allocator.isLive(first));
        assertTrue(allocator.isLive(second));
        assertEquals(2, allocator.liveCount());
    }

    @Test
    void aRecycledSlotGetsANewGenerationAndRetiresTheOldHandle() {
        HandleAllocator allocator = new HandleAllocator(ResourceKind.BUFFER, 1);
        ResourceHandle first = allocator.allocate();
        allocator.release(first);

        ResourceHandle second = allocator.allocate();

        assertEquals(first.index(), second.index());
        assertEquals(first.generation() + 1, second.generation());
        assertFalse(allocator.isLive(first));
        assertTrue(allocator.isLive(second));
    }

    @Test
    void rejectsReleasingAHandleTwice() {
        HandleAllocator allocator = new HandleAllocator(ResourceKind.BUFFER, 2);
        ResourceHandle handle = allocator.allocate();
        allocator.release(handle);

        assertThrows(IllegalArgumentException.class, () -> allocator.release(handle));
    }

    @Test
    void rejectsHandlesItNeverIssued() {
        HandleAllocator allocator = new HandleAllocator(ResourceKind.BUFFER, 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> allocator.release(ResourceHandle.of(ResourceKind.BUFFER, 1, 5)));
    }

    @Test
    void failsWhenEverySlotIsLive() {
        HandleAllocator allocator = new HandleAllocator(ResourceKind.BUFFER, 1);
        allocator.allocate();

        assertThrows(IllegalStateException.class, allocator::allocate);
    }

    @Test
    void retiresASlotWhoseGenerationsRanOut() {
        HandleAllocator allocator = new HandleAllocator(ResourceKind.BUFFER, 1);
        for (int generation = 1; generation <= ResourceHandle.MAX_GENERATION; generation++) {
            allocator.release(allocator.allocate());
        }

        assertThrows(IllegalStateException.class, allocator::allocate);
    }
}

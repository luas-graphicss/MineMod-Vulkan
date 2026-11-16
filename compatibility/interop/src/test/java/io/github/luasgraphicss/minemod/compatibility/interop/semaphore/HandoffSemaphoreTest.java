package io.github.luasgraphicss.minemod.compatibility.interop.semaphore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HandoffSemaphoreTest {
    private final HandoffSemaphore semaphore = new HandoffSemaphore("renderer-to-provider");

    @Test
    void eachSignalIsConsumedByOneWait() {
        semaphore.signaled();
        assertTrue(semaphore.isSignalPending());

        semaphore.waited();

        assertFalse(semaphore.isSignalPending());
        assertEquals(1, semaphore.completedCycles());
    }

    @Test
    void aBinarySemaphoreIsNotSignaledTwice() {
        semaphore.signaled();

        assertThrows(IllegalStateException.class, semaphore::signaled);
    }

    @Test
    void aWaitThatNothingSignalsIsRefusedInsteadOfBlocking() {
        assertThrows(IllegalStateException.class, semaphore::waited);

        semaphore.signaled();
        semaphore.waited();

        assertThrows(IllegalStateException.class, semaphore::waited);
    }
}

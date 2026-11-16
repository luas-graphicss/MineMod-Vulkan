package io.github.luasgraphicss.minemod.contract.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SessionLimitsTest {
    @Test
    void keepsTheRequestedBudgets() {
        SessionLimits limits = new SessionLimits(8, 4096);

        assertEquals(8, limits.maxBuffers());
        assertEquals(4096, limits.maxBufferBytes());
    }

    @Test
    void rejectsBudgetsThatLeaveNoRoomForABuffer() {
        assertThrows(IllegalArgumentException.class, () -> new SessionLimits(0, 4096));
        assertThrows(IllegalArgumentException.class, () -> new SessionLimits(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new SessionLimits(-1, -1));
    }
}

package io.github.luasgraphicss.minemod.client.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StreamBudgetTest {
    private static final int FRAMING_BYTES = 16;

    @Test
    void tellsTheRoomOfTheNextRecord() {
        StreamBudget budget = new StreamBudget(100, 2);

        assertEquals(100 - FRAMING_BYTES, budget.room());
        assertTrue(budget.take(budget.room()));
        assertEquals(0, budget.room());
    }

    @Test
    void hasNoRoomWithoutRecords() {
        assertEquals(0, new StreamBudget(100, 0).room());
    }

    @Test
    void holdsRecordsOfOnePayloadSize() {
        StreamBudget budget = StreamBudget.holding(2, 24);

        assertTrue(budget.take(24));
        assertTrue(budget.take(24));
        assertFalse(budget.take(0));
    }

    @Test
    void leavesWhatAnotherBudgetCarved() {
        StreamBudget rest = new StreamBudget(100, 3).without(new StreamBudget(60, 2));

        assertEquals(40 - FRAMING_BYTES, rest.room());
        assertTrue(rest.take(0));
        assertFalse(rest.take(0));
    }

    @Test
    void refusesToCarveMoreThanItHolds() {
        StreamBudget budget = new StreamBudget(100, 1);

        assertThrows(IllegalArgumentException.class, () -> budget.without(new StreamBudget(101, 1)));
    }
}

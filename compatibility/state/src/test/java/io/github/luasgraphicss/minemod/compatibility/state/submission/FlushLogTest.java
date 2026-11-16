package io.github.luasgraphicss.minemod.compatibility.state.submission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FlushLogTest {
    @Test
    void keepsTheMostRecentRecordsAndCountsEveryFlush() {
        FlushLog log = new FlushLog(2);

        log.record(1, 0, FlushReason.CALLBACK_NEEDS_ATTACHMENTS);
        log.record(1, 1, FlushReason.PRESENTATION);
        log.record(2, 0, FlushReason.CALLBACK_NEEDS_ATTACHMENTS);

        assertEquals(
                Arrays.asList(
                        new FlushRecord(1, 1, FlushReason.PRESENTATION),
                        new FlushRecord(2, 0, FlushReason.CALLBACK_NEEDS_ATTACHMENTS)),
                log.recent());
        assertEquals(2, log.count(FlushReason.CALLBACK_NEEDS_ATTACHMENTS));
        assertEquals(0, log.count(FlushReason.SYNCHRONOUS_READ));
        assertEquals(3, log.recordedCount());
    }

    @Test
    void needsRoomForAtLeastOneRecord() {
        assertThrows(IllegalArgumentException.class, () -> new FlushLog(0));
    }
}

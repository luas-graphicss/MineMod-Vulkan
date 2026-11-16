package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PresentationStateTest {
    @Test
    void decodesTheCodesOfTheNativeSession() {
        assertEquals(PresentationState.OFFSCREEN, PresentationState.fromCode(0));
        assertEquals(PresentationState.AWAITING_SWAPCHAIN, PresentationState.fromCode(1));
        assertEquals(PresentationState.RUNNING, PresentationState.fromCode(2));
        assertEquals(PresentationState.SUSPENDED, PresentationState.fromCode(3));
        assertEquals(PresentationState.DEVICE_LOST, PresentationState.fromCode(4));
        assertEquals(PresentationState.STOPPED, PresentationState.fromCode(5));
    }

    @Test
    void refusesACodeTheNativeSessionNeverReports() {
        assertThrows(IllegalArgumentException.class, () -> PresentationState.fromCode(6));
        assertThrows(IllegalArgumentException.class, () -> PresentationState.fromCode(-1));
    }

    @Test
    void pacesWithTheCodesOfTheNativeSession() {
        assertEquals(0, PresentPacing.VSYNC.code());
        assertEquals(1, PresentPacing.IMMEDIATE.code());
    }
}

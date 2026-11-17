package io.github.luasgraphicss.minemod.adapters.events.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class LoaderEventContractTest {
    @Test
    void aPointThatCancelsOrCarriesElementsIsAlsoNotified() {
        LoaderEventContract contract = LoaderEventContract.of("callback-loader")
                .notifies(FramePoint.WORLD_LAST)
                .cancels(FramePoint.BEFORE_BLOCK_OUTLINE)
                .notifiesElementsAt(FramePoint.HUD_ELEMENTS)
                .build();

        assertEquals(
                EnumSet.of(FramePoint.BEFORE_BLOCK_OUTLINE, FramePoint.WORLD_LAST, FramePoint.HUD_ELEMENTS),
                contract.notified());
        assertTrue(contract.cancelable(FramePoint.BEFORE_BLOCK_OUTLINE));
        assertFalse(contract.cancelable(FramePoint.WORLD_LAST));
        assertTrue(contract.notifiesElementsAt(FramePoint.HUD_ELEMENTS));
        assertFalse(contract.notifies(FramePoint.AFTER_SKY));
    }

    @Test
    void aLoaderWithoutRenderEventsNotifiesNothing() {
        LoaderEventContract contract = LoaderEventContract.of("client-bootstrap").build();

        assertTrue(contract.notified().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> LoaderEventContract.of(" "));
    }
}

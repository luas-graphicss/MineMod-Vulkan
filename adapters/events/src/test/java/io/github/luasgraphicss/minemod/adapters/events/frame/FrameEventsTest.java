package io.github.luasgraphicss.minemod.adapters.events.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.events.contract.LoaderEventContract;
import io.github.luasgraphicss.minemod.adapters.events.frame.FramePoint.Scope;
import io.github.luasgraphicss.minemod.adapters.events.support.ScriptedLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class FrameEventsTest {
    private static final LoaderEventContract CONTRACT = LoaderEventContract.of("staged-bus")
            .notifies(FramePoint.AFTER_SKY, FramePoint.AFTER_ENTITIES, FramePoint.WORLD_LAST, FramePoint.AFTER_HUD)
            .cancels(FramePoint.BEFORE_BLOCK_OUTLINE, FramePoint.BEFORE_HAND, FramePoint.BEFORE_HUD)
            .notifiesElementsAt(FramePoint.HUD_ELEMENTS)
            .build();

    private final ScriptedLoader loader = new ScriptedLoader();

    @Test
    void postsOnlyThePointsTheLoaderHasAnEventFor() {
        FrameEvents frame = worldFrame();

        frame.announce(FramePoint.FRAME_START);
        frame.announce(FramePoint.AFTER_SKY);
        frame.announce(FramePoint.AFTER_SOLID_TERRAIN);
        frame.announce(FramePoint.AFTER_ENTITIES);

        assertEquals(Arrays.asList("AFTER_SKY", "AFTER_ENTITIES"), loader.posted());
    }

    @Test
    void aCanceledPointWithholdsTheNativeDrawItGuards() {
        loader.cancel(FramePoint.BEFORE_BLOCK_OUTLINE);
        FrameEvents frame = worldFrame();

        assertFalse(frame.permits(FramePoint.BEFORE_BLOCK_OUTLINE));
        assertTrue(frame.permits(FramePoint.BEFORE_HAND));
    }

    @Test
    void aCancelFromAnEventThatCannotCancelChangesNothing() {
        loader.cancel(FramePoint.WORLD_LAST);
        FrameEvents frame = worldFrame();

        assertTrue(frame.permits(FramePoint.WORLD_LAST));
    }

    @Test
    void refusesToAnnounceAPointWhoseCancellationWouldBeLost() {
        FrameEvents frame = worldFrame();

        IllegalStateException refusal =
                assertThrows(IllegalStateException.class, () -> frame.announce(FramePoint.BEFORE_HAND));

        assertEquals(
                "BEFORE_HAND can cancel a native draw on staged-bus, ask whether it permits the draw",
                refusal.getMessage());
        assertTrue(loader.posted().isEmpty());
    }

    @Test
    void aCanceledElementIsNotDrawnAndGetsNoAfterEvent() {
        loader.cancelElement("hotbar");
        FrameEvents frame = worldFrame();
        frame.announce(FramePoint.HUD_ELEMENTS);

        boolean hotbarDrawn = frame.permitsElement(FramePoint.HUD_ELEMENTS, "hotbar");
        frame.completeElement(FramePoint.HUD_ELEMENTS, "hotbar");
        boolean crosshairDrawn = frame.permitsElement(FramePoint.HUD_ELEMENTS, "crosshair");
        frame.completeElement(FramePoint.HUD_ELEMENTS, "crosshair");

        assertFalse(hotbarDrawn);
        assertTrue(crosshairDrawn);
        assertEquals(
                Arrays.asList(
                        "HUD_ELEMENTS",
                        "HUD_ELEMENTS hotbar BEFORE",
                        "HUD_ELEMENTS crosshair BEFORE",
                        "HUD_ELEMENTS crosshair AFTER"),
                loader.posted());
    }

    @Test
    void aCanceledHudOwesNoneOfItsInnerEvents() {
        loader.cancel(FramePoint.BEFORE_HUD);
        FrameEvents frame = new FrameEvents(CONTRACT, loader, EnumSet.of(Scope.HUD));

        boolean hudDrawn = frame.permits(FramePoint.BEFORE_HUD);
        IllegalStateException refusal =
                assertThrows(IllegalStateException.class, () -> frame.announce(FramePoint.AFTER_HUD));

        assertFalse(hudDrawn);
        assertEquals("AFTER_HUD belongs to what BEFORE_HUD canceled", refusal.getMessage());
        assertEquals(Collections.emptyList(), frame.close());
    }

    @Test
    void refusesAPointReachedOutOfFrameOrder() {
        FrameEvents frame = worldFrame();
        frame.announce(FramePoint.AFTER_ENTITIES);

        assertThrows(IllegalStateException.class, () -> frame.announce(FramePoint.AFTER_SKY));
        assertThrows(IllegalStateException.class, () -> frame.announce(FramePoint.AFTER_ENTITIES));
        assertThrows(IllegalStateException.class, () -> frame.permitsElement(FramePoint.HUD_ELEMENTS, "hotbar"));
    }

    @Test
    void closingTellsWhichEventsTheFrameNeverPosted() {
        FrameEvents frame = worldFrame();
        frame.announce(FramePoint.AFTER_SKY);
        frame.permits(FramePoint.BEFORE_BLOCK_OUTLINE);
        frame.announce(FramePoint.WORLD_LAST);
        frame.permits(FramePoint.BEFORE_HAND);
        frame.announce(FramePoint.HUD_ELEMENTS);

        assertEquals(
                Arrays.asList(FramePoint.AFTER_ENTITIES, FramePoint.BEFORE_HUD, FramePoint.AFTER_HUD), frame.close());
        assertThrows(IllegalStateException.class, () -> frame.announce(FramePoint.FRAME_END));
    }

    @Test
    void aFrameWithoutAWorldOwesNoWorldEvent() {
        FrameEvents frame = new FrameEvents(CONTRACT, loader, EnumSet.of(Scope.FRAME, Scope.SCREEN));
        frame.announce(FramePoint.BEFORE_SCREEN);

        assertEquals(Collections.emptyList(), frame.close());
        assertThrows(
                IllegalStateException.class,
                () -> new FrameEvents(CONTRACT, loader, EnumSet.of(Scope.FRAME)).announce(FramePoint.AFTER_SKY));
    }

    private FrameEvents worldFrame() {
        return new FrameEvents(CONTRACT, loader, EnumSet.allOf(Scope.class));
    }
}

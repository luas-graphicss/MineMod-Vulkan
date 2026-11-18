package io.github.luasgraphicss.minemod.adapters.loaders.launchwrapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.display.DisplayPresentationHooks;
import org.junit.jupiter.api.Test;

class PinnedCallsTransformerTest {
    private static final String WINDOW_NAME = "fixture.Window";

    private final PinnedCallsTransformer transformer =
            new PinnedCallsTransformer(PinnedCallsTransformer.applicationsOf(PinnedCallsTransformerTest.class));

    @Test
    void sendsThePinnedDisplayCallToTheHooksAndThePinnedOpenGlCallToItsStandIn() {
        byte[] window = FixtureGame.windowCallingEach(FixtureGame.DISPLAY_UPDATE, FixtureGame.MATRIX_MODE);

        byte[] rewritten = transformer.transform(WINDOW_NAME, WINDOW_NAME, window);

        assertEquals(1, FixtureGame.callsTo(rewritten, DisplayPresentationHooks.INTERNAL_NAME));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.DISPLAY_UPDATE.owner()));
        assertEquals(1, FixtureGame.callsTo(rewritten, FixtureGame.STAND_IN));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.MATRIX_MODE.owner()));
    }

    @Test
    void leavesTheOtherClassesAsTheyAre() {
        byte[] screen = FixtureGame.windowCalling(FixtureGame.DISPLAY_UPDATE, 1);

        assertArrayEquals(screen, transformer.transform("fixture.Screen", "fixture.Screen", screen));
        assertNull(transformer.transform("fixture.Absent", "fixture.Absent", null));
    }

    @Test
    void failsTheLoadOfAClassThatAnotherTransformerChanged() {
        byte[] twoUpdates = FixtureGame.windowCallingEach(
                FixtureGame.DISPLAY_UPDATE, FixtureGame.DISPLAY_UPDATE, FixtureGame.MATRIX_MODE);

        assertThrows(
                PlanRefusedException.class, () -> transformer.transform(WINDOW_NAME, WINDOW_NAME, twoUpdates));
    }

    @Test
    void failsTheLoadOfAClassThatLostAPinnedOpenGlCall() {
        byte[] withoutTheMatrixCall = FixtureGame.windowCalling(FixtureGame.DISPLAY_UPDATE, 1);

        assertThrows(
                PlanRefusedException.class,
                () -> transformer.transform(WINDOW_NAME, WINDOW_NAME, withoutTheMatrixCall));
    }
}

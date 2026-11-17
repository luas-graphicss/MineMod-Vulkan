package io.github.luasgraphicss.minemod.adapters.loaders.launchwrapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplication;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.presentation.display.DisplayPresentationHooks;
import org.junit.jupiter.api.Test;

class PinnedCallsTransformerTest {
    private static final String WINDOW_NAME = "fixture.Window";

    private final PinnedCallsTransformer transformer = new PinnedCallsTransformer(new ClassFileApplication(
            FixtureGame.plan(FixtureGame.DISPLAY_UPDATE), DisplayPresentationHooks.INTERNAL_NAME));

    @Test
    void sendsThePinnedDisplayCallToTheHooks() {
        byte[] rewritten =
                transformer.transform(WINDOW_NAME, WINDOW_NAME, FixtureGame.windowCalling(FixtureGame.DISPLAY_UPDATE, 1));

        assertEquals(1, FixtureGame.callsTo(rewritten, DisplayPresentationHooks.INTERNAL_NAME));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.DISPLAY_UPDATE.owner()));
    }

    @Test
    void leavesTheOtherClassesAsTheyAre() {
        byte[] screen = FixtureGame.windowCalling(FixtureGame.DISPLAY_UPDATE, 1);

        assertArrayEquals(screen, transformer.transform("fixture.Screen", "fixture.Screen", screen));
        assertNull(transformer.transform("fixture.Absent", "fixture.Absent", null));
    }

    @Test
    void failsTheLoadOfAClassThatAnotherTransformerChanged() {
        byte[] twoUpdates = FixtureGame.windowCalling(FixtureGame.DISPLAY_UPDATE, 2);

        assertThrows(
                PlanRefusedException.class, () -> transformer.transform(WINDOW_NAME, WINDOW_NAME, twoUpdates));
    }
}

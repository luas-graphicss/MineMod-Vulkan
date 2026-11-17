package io.github.luasgraphicss.minemod.adapters.loaders.launch.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ClassFileApplicationTest {
    private static final String HOOKS = "fixture/Hooks";

    private final ClassFileApplication application =
            new ClassFileApplication(FixtureGame.plan(FixtureGame.SWAP_BUFFERS), HOOKS);

    @Test
    void sendsThePinnedCallToTheHookOwner() {
        byte[] rewritten =
                application.rewrite(FixtureGame.WINDOW, FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 1));

        assertEquals(1, FixtureGame.callsTo(rewritten, HOOKS));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.SWAP_BUFFERS.owner()));
    }

    @Test
    void rewritesOnlyTheClassesOfThePlan() {
        assertTrue(application.rewrites(FixtureGame.WINDOW));
        assertFalse(application.rewrites("fixture/Screen"));
    }

    @Test
    void refusesAClassThatMakesTheCallOnceMore() {
        byte[] twoSwaps = FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 2);

        PlanRefusedException refusal =
                assertThrows(PlanRefusedException.class, () -> application.rewrite(FixtureGame.WINDOW, twoSwaps));

        assertTrue(refusal.getMessage().contains("matched 2 times"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("Minecraft " + FixtureGame.RELEASE), refusal.getMessage());
    }

    @Test
    void refusesAClassThatNeverMakesTheCall() {
        byte[] noSwap = FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 0);

        PlanRefusedException refusal =
                assertThrows(PlanRefusedException.class, () -> application.rewrite(FixtureGame.WINDOW, noSwap));

        assertTrue(refusal.getMessage().contains("matched nothing"), refusal.getMessage());
    }

    @Test
    void acceptsTheClassesThePlanWasPinnedOn() throws IOException {
        byte[] window = FixtureGame.windowCalling(FixtureGame.SWAP_BUFFERS, 1);

        application.requireApplicableTo(name -> Optional.of(window));
    }

    @Test
    void refusesClassesWithoutThePinnedOne() {
        PlanRefusedException refusal = assertThrows(
                PlanRefusedException.class, () -> application.requireApplicableTo(name -> Optional.empty()));

        assertTrue(refusal.getMessage().contains("was never applied"), refusal.getMessage());
    }
}

package io.github.luasgraphicss.minemod.adapters.loaders.launch.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.apply.ClassFileApplications;
import io.github.luasgraphicss.minemod.adapters.loaders.launch.fixtures.FixtureGame;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class FramePlanTest {
    private static final String HOOKS = "fixture/Hooks";
    private static final String OTHER_CLIENT = "89abcdef0123456789abcdef0123456789abcdef";

    @Test
    void groupsThePinnedCallsByTheClassThatAnswersThem() throws IOException {
        FramePlan frame = pinnedOn(FixtureGame.UNPINNED_CLIENT);

        assertEquals(Collections.singleton(FixtureGame.STAND_IN), frame.plansByStandIn().keySet());
        ReleasePlan answered = frame.plansByStandIn().get(FixtureGame.STAND_IN);
        assertEquals(FixtureGame.MATRIX_MODE, answered.redirects().get(0).callee());
    }

    @Test
    void rewritesAClassForThePresentationAndThenForEachStandIn() throws IOException {
        ClassFileApplications applications = pinnedOn(FixtureGame.UNPINNED_CLIENT)
                .classFilesAfter(FixtureGame.plan(FixtureGame.DISPLAY_UPDATE), HOOKS);

        byte[] rewritten = applications.rewrite(
                FixtureGame.WINDOW, FixtureGame.windowCallingEach(FixtureGame.DISPLAY_UPDATE, FixtureGame.MATRIX_MODE));

        assertEquals(1, FixtureGame.callsTo(rewritten, HOOKS));
        assertEquals(1, FixtureGame.callsTo(rewritten, FixtureGame.STAND_IN));
        assertEquals(0, FixtureGame.callsTo(rewritten, FixtureGame.MATRIX_MODE.owner()));
    }

    @Test
    void refusesAFramePinnedOnAnotherClientThanThePresentation() throws IOException {
        FramePlan frame = pinnedOn(OTHER_CLIENT);

        IllegalStateException refusal = assertThrows(
                IllegalStateException.class,
                () -> frame.classFilesAfter(FixtureGame.plan(FixtureGame.DISPLAY_UPDATE), HOOKS));

        assertTrue(refusal.getMessage().contains(OTHER_CLIENT), refusal.getMessage());
    }

    @Test
    void refusesAPointThatNamesNoStandIn() {
        String withoutStandIn = FixtureGame.planText(FixtureGame.MATRIX_MODE, FixtureGame.UNPINNED_CLIENT);

        IllegalArgumentException refusal = assertThrows(IllegalArgumentException.class, () -> read(withoutStandIn));

        assertTrue(refusal.getMessage().contains("point.present.hook"), refusal.getMessage());
    }

    private static FramePlan pinnedOn(String clientSha1) throws IOException {
        return read(FixtureGame.framePlanText(FixtureGame.MATRIX_MODE, clientSha1, FixtureGame.STAND_IN));
    }

    private static FramePlan read(String text) throws IOException {
        return FramePlan.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }
}

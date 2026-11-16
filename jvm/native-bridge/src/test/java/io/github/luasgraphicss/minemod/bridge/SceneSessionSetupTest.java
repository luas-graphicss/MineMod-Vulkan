package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SceneSessionSetupTest {
    private static final int EDGE = 64;
    private static final long ARENA_BYTES = 4096;
    private static final int RESOURCES = 16;

    @Test
    void keepsItsAttachmentsToItselfUnlessAsked() {
        SceneSessionSetup setup = SceneSessionSetup.offscreen(EDGE, EDGE);

        assertFalse(setup.sharesAttachments());
        assertTrue(setup.sharingAttachments().sharesAttachments());
    }

    @Test
    void keepsSharingWhenItsLimitsChange() {
        SceneSessionSetup shared = SceneSessionSetup.offscreen(EDGE, EDGE).sharingAttachments();

        assertTrue(shared.withArenas(ARENA_BYTES, ARENA_BYTES).sharesAttachments());
        assertTrue(shared.withLimits(RESOURCES, RESOURCES).sharesAttachments());
    }

    @Test
    void namesTheSharingInItsDescription() {
        SceneSessionSetup setup = SceneSessionSetup.offscreen(EDGE, EDGE);

        assertFalse(setup.toString().contains("sharing"), setup.toString());
        assertTrue(setup.sharingAttachments().toString().contains("sharing its attachments"));
    }
}

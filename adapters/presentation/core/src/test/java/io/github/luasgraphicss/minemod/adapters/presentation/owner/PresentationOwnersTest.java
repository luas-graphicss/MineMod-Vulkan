package io.github.luasgraphicss.minemod.adapters.presentation.owner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.RecordingOwner;
import org.junit.jupiter.api.Test;

class PresentationOwnersTest {
    @Test
    void leavesPresentationToTheLibraryUntilAnOwnerIsInstalled() {
        assertFalse(PresentationOwners.isInstalled());
        assertTrue(PresentationOwners.current().keepsOpenGlContext());
    }

    @Test
    void refusesASecondOwnerOfTheMainWindow() {
        RecordingOwner first = new RecordingOwner(false, null);
        PresentationOwners.install(first);
        try {
            IllegalStateException refused = assertThrows(
                    IllegalStateException.class, () -> PresentationOwners.install(new RecordingOwner(false, null)));
            assertTrue(refused.getMessage().contains("cannot present it too"), refused.getMessage());
            assertSame(first, PresentationOwners.current());
        } finally {
            PresentationOwners.release(first);
        }
        assertFalse(PresentationOwners.isInstalled());
    }

    @Test
    void releasesOnlyTheInstalledOwner() {
        assertThrows(IllegalStateException.class, () -> PresentationOwners.release(new RecordingOwner(true, null)));
    }
}

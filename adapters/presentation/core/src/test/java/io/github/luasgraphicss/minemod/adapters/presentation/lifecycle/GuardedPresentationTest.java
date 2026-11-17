package io.github.luasgraphicss.minemod.adapters.presentation.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.RecordingOwner;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class GuardedPresentationTest {
    private static final Runnable UNUSED = () -> {
        throw new AssertionError("the library call was not expected to run");
    };
    private final RecordingOwner owner = new RecordingOwner(false, "capabilities");
    private final GuardedPresentation guarded = new GuardedPresentation(owner);

    @Test
    void followsAWindowFromItsOpeningToItsClose() {
        long window = guarded.openWindow(WindowRequest.ofSize(854, 480, "Minecraft"), () -> 0L);
        guarded.attachContext(window, UNUSED);
        assertEquals("capabilities", guarded.resolveCapabilities(() -> null));
        guarded.pace(1, UNUSED);
        guarded.presentFrame(window, UNUSED);
        guarded.presentFrame(window, UNUSED);
        guarded.closeWindow(window, UNUSED);

        assertEquals(PresentationPhase.CLOSED, guarded.phase());
        assertEquals(2, guarded.framesPresented());
        assertEquals(
                Arrays.asList(
                        "open window \"Minecraft\" 854x480",
                        "attach 81",
                        "capabilities",
                        "pace 1",
                        "present 81",
                        "present 81",
                        "close 81"),
                owner.steps());
    }

    @Test
    void readiesTheDisplayOfLwjgl2AsSoonAsItOpens() {
        guarded.openWindow(WindowRequest.ofDisplay(), () -> 0L);
        guarded.presentFrame(WindowRequest.DISPLAY_WINDOW, UNUSED);

        assertEquals(PresentationPhase.READY, guarded.phase());
        assertEquals(1, guarded.framesPresented());
    }

    @Test
    void refusesAFramePresentedBeforeTheWindowIsReady() {
        long window = guarded.openWindow(WindowRequest.ofSize(1, 1, "Minecraft"), () -> 0L);

        PresentationOrderException refused =
                assertThrows(PresentationOrderException.class, () -> guarded.presentFrame(window, UNUSED));

        assertTrue(refused.getMessage().endsWith("present a frame while the main window was OPENED"));
        assertEquals(0, guarded.framesPresented());
    }

    @Test
    void refusesAFrameAfterTheWindowCloses() {
        long window = guarded.openWindow(WindowRequest.ofSize(1, 1, "Minecraft"), () -> 0L);
        guarded.closeWindow(window, UNUSED);

        assertThrows(PresentationOrderException.class, () -> guarded.presentFrame(window, UNUSED));
        assertThrows(PresentationOrderException.class, () -> guarded.closeWindow(window, UNUSED));
    }

    @Test
    void staysUnopenedWhenTheWindowCouldNotBeCreated() {
        GuardedPresentation failing = new GuardedPresentation(new io.github.luasgraphicss.minemod.adapters.presentation.owner.LibraryPresentation());

        assertEquals(0L, failing.openWindow(WindowRequest.ofSize(1, 1, "Minecraft"), () -> 0L));
        assertEquals(PresentationPhase.UNOPENED, failing.phase());
    }
}

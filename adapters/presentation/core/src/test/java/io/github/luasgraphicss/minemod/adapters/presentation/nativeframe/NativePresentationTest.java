package io.github.luasgraphicss.minemod.adapters.presentation.nativeframe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.RecordingPresenter;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class NativePresentationTest {
    private static final long WINDOW = 0x77L;
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final NativePresentation presentation = new NativePresentation(presenter);
    private final List<String> libraryCalls = new ArrayList<>();

    @Test
    void presentsThroughTheRendererAndNeverSwapsTheWindow() {
        GuardedPresentation guarded = new GuardedPresentation(presentation);

        assertFalse(guarded.keepsOpenGlContext());
        long window = guarded.openWindow(WindowRequest.ofSize(854, 480, "Minecraft"), () -> WINDOW);
        guarded.attachContext(window, library("make current"));
        assertSame(RecordingPresenter.CAPABILITIES, guarded.resolveCapabilities(() -> "library capabilities"));
        guarded.pace(0, library("swap interval"));
        guarded.pace(1, library("swap interval"));
        guarded.presentFrame(window, library("swap buffers"));
        guarded.closeWindow(window, library("destroy"));

        assertEquals(
                Arrays.asList("start 119", "capabilities", "pace immediate", "pace vsync", "present", "stop"),
                presenter.steps());
        assertEquals(Arrays.asList("destroy"), libraryCalls);
    }

    @Test
    void stopsTheRendererBeforeTheWindowIsDestroyed() {
        presentation.openWindow(WindowRequest.ofSize(1, 1, "Minecraft"), () -> WINDOW);
        presentation.attachContext(WINDOW, library("make current"));

        presentation.closeWindow(WINDOW, () -> libraryCalls.add("destroy after " + presenter.steps()));

        assertEquals(Arrays.asList("destroy after [start 119, stop]"), libraryCalls);
    }

    @Test
    void destroysAWindowTheRendererNeverStartedOn() {
        presentation.openWindow(WindowRequest.ofSize(1, 1, "Minecraft"), () -> WINDOW);

        presentation.closeWindow(WINDOW, library("destroy"));

        assertEquals(Arrays.asList(), presenter.steps());
        assertEquals(Arrays.asList("destroy"), libraryCalls);
    }

    @Test
    void refusesAnotherWindowThanTheOneItOpened() {
        presentation.openWindow(WindowRequest.ofSize(1, 1, "Minecraft"), () -> WINDOW);

        assertThrows(IllegalArgumentException.class, () -> presentation.attachContext(WINDOW + 1, library("unused")));
    }

    @Test
    void refusesTheDisplayOfLwjgl2() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> presentation.openWindow(WindowRequest.ofDisplay(), () -> WindowRequest.DISPLAY_WINDOW));
        assertEquals(Arrays.asList(), presenter.steps());
    }

    private Runnable library(String call) {
        return () -> libraryCalls.add(call);
    }
}

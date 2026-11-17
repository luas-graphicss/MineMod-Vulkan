package io.github.luasgraphicss.minemod.adapters.presentation.nativeframe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.RecordingPresenter;
import io.github.luasgraphicss.minemod.adapters.presentation.fixtures.RecordingStandInWindow;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.GuardedPresentation;
import io.github.luasgraphicss.minemod.adapters.presentation.lifecycle.PresentationPhase;
import io.github.luasgraphicss.minemod.adapters.presentation.owner.WindowRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class NativeDisplayPresentationTest {
    private final RecordingPresenter presenter = new RecordingPresenter();
    private final NativeDisplayPresentation presentation =
            new NativeDisplayPresentation(presenter, new RecordingStandInWindow(presenter.steps()));
    private final List<String> libraryCalls = new ArrayList<>();

    @Test
    void presentsTheDisplayWithoutEverReachingTheLibrary() {
        GuardedPresentation guarded = new GuardedPresentation(presentation);

        assertFalse(guarded.keepsOpenGlContext());
        long window = guarded.openWindow(WindowRequest.ofDisplay(), () -> {
            libraryCalls.add("create");
            return WindowRequest.DISPLAY_WINDOW;
        });
        guarded.pace(1, library("vsync"));
        guarded.pace(0, library("vsync"));
        guarded.presentFrame(window, library("update"));
        guarded.closeWindow(window, library("destroy"));

        assertEquals(WindowRequest.DISPLAY_WINDOW, window);
        assertEquals(
                Arrays.asList(
                        "open stand-in", "start 0", "pace vsync", "pace immediate", "present", "stop", "close stand-in"),
                presenter.steps());
        assertEquals(Arrays.asList(), libraryCalls);
        assertEquals(PresentationPhase.CLOSED, guarded.phase());
    }

    @Test
    void closesTheStandInWhenTheRendererCannotStartOnIt() {
        IllegalStateException noDevice = new IllegalStateException("no Vulkan device");
        List<String> steps = new ArrayList<>();
        NativeDisplayPresentation failing = new NativeDisplayPresentation(
                new FailingStart(noDevice), new RecordingStandInWindow(steps));

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> failing.openWindow(WindowRequest.ofDisplay(), () -> WindowRequest.DISPLAY_WINDOW));

        assertSame(noDevice, thrown);
        assertEquals(Arrays.asList("open stand-in", "close stand-in"), steps);
    }

    @Test
    void stopsTheRendererAndClosesTheStandInOnlyOnce() {
        presentation.openWindow(WindowRequest.ofDisplay(), () -> WindowRequest.DISPLAY_WINDOW);
        presentation.closeWindow(WindowRequest.DISPLAY_WINDOW, library("destroy"));
        presentation.closeWindow(WindowRequest.DISPLAY_WINDOW, library("destroy"));

        assertEquals(Arrays.asList("open stand-in", "start 0", "stop", "close stand-in"), presenter.steps());
        assertEquals(Arrays.asList(), libraryCalls);
    }

    @Test
    void refusesAWindowOfTheGameLibrary() {
        assertThrows(
                IllegalArgumentException.class,
                () -> presentation.openWindow(WindowRequest.ofSize(854, 480, "Minecraft"), () -> 0x77L));
        assertEquals(Arrays.asList(), presenter.steps());
    }

    @Test
    void hasNoOpenGlContextToAttachOrResolve() {
        assertThrows(UnsupportedOperationException.class, () -> presentation.attachContext(0L, library("unused")));
        assertThrows(UnsupportedOperationException.class, () -> presentation.resolveCapabilities(() -> "unused"));
    }

    private Runnable library(String call) {
        return () -> libraryCalls.add(call);
    }

    private static final class FailingStart implements FramePresenter {
        private final RuntimeException failure;

        FailingStart(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void start(long window) {
            throw failure;
        }

        @Override
        public Object capabilities() {
            throw new AssertionError("a renderer that never started resolves nothing");
        }

        @Override
        public void present() {
            throw new AssertionError("a renderer that never started presents nothing");
        }

        @Override
        public void pace(boolean synchronised) {
            throw new AssertionError("a renderer that never started is not paced");
        }

        @Override
        public void stop() {
            throw new AssertionError("a renderer that never started is not stopped");
        }
    }
}

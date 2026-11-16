package io.github.luasgraphicss.minemod.client.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.bridge.PresentationState;
import io.github.luasgraphicss.minemod.client.failure.RendererFailure;
import io.github.luasgraphicss.minemod.client.window.FramebufferSize;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PresentationControlTest {
    private static final FramebufferSize OPENED = new FramebufferSize(854, 480);
    private static final FramebufferSize MAXIMISED = new FramebufferSize(1920, 1080);
    private static final FramebufferSize MINIMISED = new FramebufferSize(0, 0);

    private final RecordingTarget target = new RecordingTarget();
    private final ResizableWindow window = new ResizableWindow();
    private final PresentationControl control = new PresentationControl(target, window, OPENED);

    @Test
    void drawsAtTheSizeTheSessionWasOpenedWith() {
        assertTrue(control.beginFrame());

        assertEquals(Collections.emptyList(), target.resizes);
    }

    @Test
    void resizesTheSessionOnceWhenTheFramebufferChanges() {
        window.size = MAXIMISED;

        assertTrue(control.beginFrame());
        assertTrue(control.beginFrame());

        assertEquals(Collections.singletonList(MAXIMISED), target.resizes);
    }

    @Test
    void skipsDrawingWhileTheWindowHasNoArea() {
        window.size = MINIMISED;

        assertFalse(control.beginFrame());

        assertEquals(Collections.singletonList(MINIMISED), target.resizes);
    }

    @Test
    void drawsAgainOnceTheWindowHasAreaAgain() {
        window.size = MINIMISED;
        control.beginFrame();
        window.size = OPENED;

        assertTrue(control.beginFrame());

        assertEquals(Arrays.asList(MINIMISED, OPENED), target.resizes);
    }

    @Test
    void keepsTheSizeOfTheLastFrameThatHadAnArea() {
        window.size = MAXIMISED;
        control.beginFrame();
        window.size = MINIMISED;
        control.beginFrame();

        assertEquals(MAXIMISED, control.frameSize());
    }

    @Test
    void appliesARequestedPacingAtTheNextFrame() {
        control.pace(PresentPacing.IMMEDIATE);
        control.beginFrame();
        control.beginFrame();

        assertEquals(Collections.singletonList(PresentPacing.IMMEDIATE), target.pacings);
    }

    @Test
    void keepsThePacingTheSessionAlreadyUses() {
        control.pace(PresentPacing.VSYNC);
        control.beginFrame();

        assertEquals(Collections.emptyList(), target.pacings);
    }

    @Test
    void stopsWhenTheDeviceWasLost() {
        target.state = PresentationState.DEVICE_LOST;

        assertThrows(RendererFailure.class, control::endFrame);
    }

    @Test
    void carriesOnWhileTheSessionAwaitsASwapchain() {
        target.state = PresentationState.AWAITING_SWAPCHAIN;

        control.endFrame();
    }

    private static final class RecordingTarget implements PresentationTarget {
        private final List<FramebufferSize> resizes = new ArrayList<>();
        private final List<PresentPacing> pacings = new ArrayList<>();
        private PresentationState state = PresentationState.RUNNING;

        @Override
        public void resize(int width, int height) {
            resizes.add(new FramebufferSize(width, height));
        }

        @Override
        public void setPacing(PresentPacing pacing) {
            pacings.add(pacing);
        }

        @Override
        public PresentationState presentationState() {
            return state;
        }
    }

    private static final class ResizableWindow implements WindowSurface {
        private FramebufferSize size = OPENED;

        @Override
        public List<String> requiredInstanceExtensions() {
            return Collections.emptyList();
        }

        @Override
        public long createSurface(long vulkanInstance) {
            throw new UnsupportedOperationException("the presentation control never creates a surface");
        }

        @Override
        public FramebufferSize framebufferSize() {
            return size;
        }
    }
}

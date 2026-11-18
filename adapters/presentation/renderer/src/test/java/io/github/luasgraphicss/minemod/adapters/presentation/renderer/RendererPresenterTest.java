package io.github.luasgraphicss.minemod.adapters.presentation.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.bridge.PresentPacing;
import io.github.luasgraphicss.minemod.client.window.FramebufferSize;
import io.github.luasgraphicss.minemod.client.window.WindowSurface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class RendererPresenterTest {
    private static final long WINDOW = 0x77L;
    private final List<String> steps = new ArrayList<>();
    private final RendererPresenter presenter =
            new RendererPresenter(this::startRecording, window -> new NamedSurface("surface of " + window));

    @Test
    void startsOneRendererOnTheSurfaceOfTheWindowAndDrivesIt() {
        presenter.start(WINDOW);
        presenter.pace(false);
        presenter.pace(true);
        presenter.present();
        presenter.present();
        presenter.stop();

        assertEquals(
                Arrays.asList(
                        "start on surface of 119", "pace IMMEDIATE", "pace VSYNC", "present", "present", "close"),
                steps);
    }

    @Test
    void offersTheGameNoOpenGlCapabilities() {
        presenter.start(WINDOW);

        assertNull(presenter.capabilities());
    }

    @Test
    void presentsASecondWindowOnlyAfterTheFirstStopped() {
        presenter.start(WINDOW);

        assertThrows(IllegalStateException.class, () -> presenter.start(WINDOW + 1));
        presenter.stop();
        presenter.start(WINDOW + 1);

        assertEquals(Arrays.asList("start on surface of 119", "close", "start on surface of 120"), steps);
    }

    @Test
    void refusesToPresentPaceOrStopBeforeItStarted() {
        assertThrows(IllegalStateException.class, presenter::present);
        assertThrows(IllegalStateException.class, () -> presenter.pace(true));
        assertThrows(IllegalStateException.class, presenter::stop);
        assertEquals(Collections.emptyList(), steps);
    }

    @Test
    void staysStoppedWhenTheRendererCannotStart() {
        RendererPresenter failing = new RendererPresenter(
                surface -> {
                    throw new IllegalStateException("no Vulkan device");
                },
                window -> new NamedSurface("unused"));

        assertThrows(IllegalStateException.class, () -> failing.start(WINDOW));
        assertThrows(IllegalStateException.class, failing::present);
    }

    private WindowRenderer startRecording(WindowSurface surface) {
        steps.add("start on " + surface);
        return new WindowRenderer() {
            @Override
            public void presentFrame() {
                steps.add("present");
            }

            @Override
            public void pace(PresentPacing pacing) {
                steps.add("pace " + pacing);
            }

            @Override
            public void close() {
                steps.add("close");
            }
        };
    }

    private static final class NamedSurface implements WindowSurface {
        private final String name;

        NamedSurface(String name) {
            this.name = name;
        }

        @Override
        public List<String> requiredInstanceExtensions() {
            return Collections.emptyList();
        }

        @Override
        public long createSurface(long vulkanInstance) {
            return 0L;
        }

        @Override
        public FramebufferSize framebufferSize() {
            return new FramebufferSize(1, 1);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

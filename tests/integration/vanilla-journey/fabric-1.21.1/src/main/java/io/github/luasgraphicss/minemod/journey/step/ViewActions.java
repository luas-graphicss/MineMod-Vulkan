package io.github.luasgraphicss.minemod.journey.step;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

final class ViewActions {
    private static final String HIDDEN = "hidden";
    private static final String PAUSE = "pause";
    private static final String CAPTURES = "captures";
    private static final String IMAGE_EXTENSION = ".png";
    private static final Pattern CAPTURE_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");
    private static final int FRAMES_BEFORE_CAPTURE = 2;
    private static final long NANOSECONDS_PER_TICK = 50_000_000L;

    private ViewActions() {}

    static StepAction hud(String state) {
        return StepAction.immediate(minecraft -> minecraft.options.hideGui = state.equals(HIDDEN));
    }

    static StepAction screen(String screen) {
        return StepAction.immediate(minecraft -> {
            if (screen.equals(PAUSE)) {
                minecraft.pauseGame(false);
            } else {
                minecraft.setScreen(null);
            }
        });
    }

    static StepAction waitFor(long ticks) {
        long[] started = new long[1];
        return StepAction.until(
                minecraft -> started[0] = System.nanoTime(),
                minecraft -> System.nanoTime() - started[0] >= ticks * NANOSECONDS_PER_TICK);
    }

    static StepAction settle(long ticks) {
        return new Settle(ticks);
    }

    static StepAction capture(Path output, String name) {
        if (!CAPTURE_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("capture " + name + " is not named in lowercase letters and digits");
        }
        return new Capture(output.resolve(CAPTURES).resolve(name + IMAGE_EXTENSION));
    }

    private static final class Settle implements StepAction {
        private final long requiredPolls;
        private long settledPolls;

        private Settle(long requiredPolls) {
            this.requiredPolls = requiredPolls;
        }

        @Override
        public void begin(Minecraft minecraft) {
            WorldActions.requireWorld(minecraft);
        }

        @Override
        public boolean isDone(Minecraft minecraft) {
            settledPolls = minecraft.levelRenderer.hasRenderedAllSections() ? settledPolls + 1 : 0;
            return settledPolls >= requiredPolls;
        }
    }

    private static final class Capture implements StepAction {
        private final Path image;
        private int framesSeen;

        private Capture(Path image) {
            this.image = image;
        }

        @Override
        public void begin(Minecraft minecraft) {}

        @Override
        public boolean isDone(Minecraft minecraft) {
            if (framesSeen++ < FRAMES_BEFORE_CAPTURE) {
                return false;
            }
            try (NativeImage frame = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                Files.createDirectories(image.getParent());
                frame.writeToFile(image);
            } catch (IOException unwritable) {
                throw new UncheckedIOException("capture " + image + " cannot be written", unwritable);
            }
            return true;
        }
    }
}

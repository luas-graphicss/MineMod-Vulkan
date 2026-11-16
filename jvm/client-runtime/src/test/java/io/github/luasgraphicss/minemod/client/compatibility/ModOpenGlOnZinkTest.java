package io.github.luasgraphicss.minemod.client.compatibility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.client.compatibility.support.SharedSession;
import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.DispatchInstaller;
import io.github.luasgraphicss.minemod.compatibility.layer.state.TrackedCapability;
import io.github.luasgraphicss.minemod.compatibility.lwjgl3.Lwjgl3Dispatch;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushReason;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushRecord;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
@EnabledIfSystemProperty(named = SharedSession.LIBRARY, matches = ".+")
class ModOpenGlOnZinkTest {
    private static final String PROVIDER_NATIVES = "minemod.providerNatives";
    private static final int CONTEXT_MAJOR = 4;
    private static final int CONTEXT_MINOR = 5;
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final int LEFT = WIDTH / 4;
    private static final int RIGHT = 3 * WIDTH / 4;
    private static final int ROW = HEIGHT / 2;
    private static final int[] BLUE = {0, 0, 255, 255};
    private static final int[] RED = {255, 0, 0, 255};
    private static final int[] GREEN = {0, 255, 0, 255};
    private static final int[] MAGENTA = {255, 0, 255, 255};

    @TempDir
    Path providerDirectory;

    private final Lwjgl3Dispatch binding = new Lwjgl3Dispatch();
    private SharedSession renderer;
    private ModOpenGl mods;

    @BeforeEach
    void installTheDispatchOverASceneSession() throws IOException {
        renderer = SharedSession.open(WIDTH, HEIGHT);
        mods = ModOpenGl.open(
                setupOver(ProviderTestEnvironment.stage(providerDirectory)),
                renderer.sharing(),
                renderer::submitRecordedPart);
        mods.install(new DispatchInstaller(binding, binding), BindingGeneration.LWJGL3);
        GL.createCapabilities();
    }

    @AfterEach
    void removeTheDispatchAndCloseTheSessions() {
        binding.uninstall();
        mods.close();
        renderer.close();
    }

    @Test
    void aCallThroughLwjglDrawsOverTheFirstPartOfTheFrame() {
        renderer.recordFrameOver(BLUE);

        clearThroughLwjgl(0, WIDTH / 2, GREEN);
        int error = GL11.glGetError();
        presentFrame();

        assertArrayEquals(GREEN, renderer.pixelAt(LEFT, ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(RIGHT, ROW));
        assertEquals(GL11.GL_NO_ERROR, error);
        assertEquals("", renderer.validationText());
    }

    @Test
    void nativeWorkBetweenTwoDrawingsOfAModTakesTheFrameBackOnce() {
        renderer.recordFrameOver(BLUE);

        clearThroughLwjgl(0, WIDTH / 2, GREEN);
        mods.beforeNativeWork();
        mods.beforeNativeWork();
        clearThroughLwjgl(WIDTH / 2, WIDTH / 2, MAGENTA);
        presentFrame();

        assertEquals(
                Arrays.asList(
                        FlushReason.CALLBACK_NEEDS_ATTACHMENTS,
                        FlushReason.NATIVE_PASS_NEEDS_ATTACHMENTS,
                        FlushReason.CALLBACK_NEEDS_ATTACHMENTS,
                        FlushReason.PRESENTATION),
                reasonsOfTheFlushes());
        assertArrayEquals(GREEN, renderer.pixelAt(LEFT, ROW));
        assertArrayEquals(MAGENTA, renderer.pixelAt(RIGHT, ROW));
        assertEquals("", renderer.validationText());
    }

    @Test
    void aFrameInWhichNoModDrawsIsNeverHandedOver() {
        renderer.recordFrameOver(RED);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_BLEND);
        presentFrame();

        assertEquals(Collections.emptyList(), reasonsOfTheFlushes());
        assertArrayEquals(RED, renderer.pixelAt(LEFT, ROW));
        assertEquals("", renderer.validationText());
    }

    @Test
    void lwjglDrawsIntoTheAttachmentsOfAResizedSession() {
        renderer.recordFrameOver(BLUE);
        presentFrame();
        renderer.resize(2 * WIDTH, HEIGHT);
        mods.followSession();
        renderer.recordFrameOver(BLUE);

        GL11.glViewport(0, 0, 2 * WIDTH, HEIGHT);
        clearThroughLwjgl(WIDTH, WIDTH, GREEN);
        int error = GL11.glGetError();
        presentFrame();

        assertArrayEquals(GREEN, renderer.pixelAt(WIDTH + RIGHT, ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(LEFT, ROW));
        assertEquals(GL11.GL_NO_ERROR, error);
        assertEquals("", renderer.validationText());
    }

    private void presentFrame() {
        mods.present();
        renderer.endFrame();
    }

    private List<FlushReason> reasonsOfTheFlushes() {
        List<FlushReason> reasons = new ArrayList<>();
        for (FlushRecord flush : mods.layer().flushes().recent()) {
            reasons.add(flush.reason());
        }
        return reasons;
    }

    private static void clearThroughLwjgl(int left, int width, int[] color) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, 0, width, HEIGHT);
        GL11.glClearColor(color[0] / 255.0F, color[1] / 255.0F, color[2] / 255.0F, color[3] / 255.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static ModOpenGlSetup setupOver(ProviderBundle bundle) {
        return new ModOpenGlSetup(
                Paths.get(System.getProperty(PROVIDER_NATIVES)),
                bundle,
                new ContextRequest(CONTEXT_MAJOR, CONTEXT_MINOR, ContextProfile.CORE, true),
                className -> Optional.empty(),
                Collections.<TrackedCapability>emptyList());
    }
}

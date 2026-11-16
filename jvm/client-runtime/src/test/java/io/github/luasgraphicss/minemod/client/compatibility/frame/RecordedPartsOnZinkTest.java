package io.github.luasgraphicss.minemod.client.compatibility.frame;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.luasgraphicss.minemod.client.compatibility.ContextRequest;
import io.github.luasgraphicss.minemod.client.compatibility.ModOpenGl;
import io.github.luasgraphicss.minemod.client.compatibility.ModOpenGlSetup;
import io.github.luasgraphicss.minemod.client.compatibility.provider.ProviderManifest;
import io.github.luasgraphicss.minemod.client.compatibility.support.SharedSession;
import io.github.luasgraphicss.minemod.client.draw.frame.DrawRecorder;
import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.client.draw.frame.GameView;
import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.codec.scene.PrimitiveMode;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import io.github.luasgraphicss.minemod.compatibility.dispatch.binding.BindingGeneration;
import io.github.luasgraphicss.minemod.compatibility.dispatch.install.DispatchInstaller;
import io.github.luasgraphicss.minemod.compatibility.layer.state.TrackedCapability;
import io.github.luasgraphicss.minemod.compatibility.lwjgl3.Lwjgl3Dispatch;
import io.github.luasgraphicss.minemod.compatibility.provider.artifact.ArtifactDigest;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.runtime.provider.ProviderBundle;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushReason;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushRecord;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
class RecordedPartsOnZinkTest {
    private static final String PROVIDER_NATIVES = "minemod.providerNatives";
    private static final int CONTEXT_MAJOR = 4;
    private static final int CONTEXT_MINOR = 5;
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final int ROW = HEIGHT / 2;
    private static final long STREAM_BYTES = 32 * 1024;
    private static final long STREAM_RECORDS = 64;
    private static final int VERTEX_BYTES = 16;
    private static final int QUAD_VERTICES = 6;
    private static final int RED_VERTEX = 0xFF0000FF;
    private static final float LEFT_EDGE = -1.0f;
    private static final float END_OF_THE_LEFT_QUARTER = -0.5f;
    private static final float[] IDENTITY = {
        1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
    };
    private static final GameVertexLayout POSITION_COLOR = GameVertexLayout.builder(VERTEX_BYTES)
            .element(VertexElement.POSITION, 0)
            .element(VertexElement.COLOR, 12)
            .build();
    private static final int[] BLUE = {0, 0, 255, 255};
    private static final int[] RED = {255, 0, 0, 255};
    private static final int[] GREEN = {0, 255, 0, 255};
    private static final int[] MAGENTA = {255, 0, 255, 255};

    @TempDir
    Path providerDirectory;

    private final Lwjgl3Dispatch binding = new Lwjgl3Dispatch();
    private final DrawRecorder recorder = new DrawRecorder(STREAM_BYTES, STREAM_RECORDS);
    private SharedSession renderer;
    private ModOpenGl mods;
    private int widthOfTheWindow = WIDTH;

    @BeforeEach
    void installTheDispatchOverTheRecorderOfTheGame() throws IOException {
        renderer = SharedSession.open(WIDTH, HEIGHT);
        mods = ModOpenGl.open(
                setupOver(describedByItsManifest(ProviderTestEnvironment.stage(providerDirectory))),
                renderer.sharing(),
                this::submitPartOnTheWindow);
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
    void drawsOfTheGameAndOfAModLandInTheOrderTheyWereMade() {
        recorder.clearColor(0.0f, 0.0f, 1.0f, 1.0f);
        clearThroughLwjgl(0, WIDTH / 2, GREEN);
        mods.beforeNativeWork();
        recorder.draw(redOverTheLeftQuarter());
        clearThroughLwjgl(0, WIDTH / 8, MAGENTA);
        presentFrame();

        assertEquals(
                Arrays.asList(
                        FlushReason.CALLBACK_NEEDS_ATTACHMENTS,
                        FlushReason.NATIVE_PASS_NEEDS_ATTACHMENTS,
                        FlushReason.CALLBACK_NEEDS_ATTACHMENTS,
                        FlushReason.PRESENTATION),
                reasonsOfTheFlushes());
        assertArrayEquals(MAGENTA, renderer.pixelAt(WIDTH / 16, ROW));
        assertArrayEquals(RED, renderer.pixelAt(3 * WIDTH / 16, ROW));
        assertArrayEquals(GREEN, renderer.pixelAt(3 * WIDTH / 8, ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(3 * WIDTH / 4, ROW));
        assertEquals("", renderer.validationText());
    }

    @Test
    void aFrameWithoutDrawsOfAModIsSubmittedWhole() {
        recorder.clearColor(0.0f, 0.0f, 1.0f, 1.0f);
        mods.beforeNativeWork();
        recorder.draw(redOverTheLeftQuarter());
        boolean wasPartlyWritten = recorder.isPartlyWritten();
        presentFrame();

        assertFalse(wasPartlyWritten);
        assertEquals(Collections.emptyList(), reasonsOfTheFlushes());
        assertArrayEquals(RED, renderer.pixelAt(WIDTH / 8, ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(3 * WIDTH / 4, ROW));
        assertEquals("", renderer.validationText());
    }

    @Test
    void aModDrawsOnTheAttachmentsOfASessionResizedByThePartItWaitsFor() {
        recorder.clearColor(0.0f, 0.0f, 1.0f, 1.0f);
        presentFrame();
        widthOfTheWindow = 2 * WIDTH;
        recorder.clearColor(0.0f, 0.0f, 1.0f, 1.0f);
        clearThroughLwjgl(WIDTH, WIDTH, GREEN);
        int error = GL11.glGetError();
        presentFrame();

        assertArrayEquals(GREEN, renderer.pixelAt(WIDTH + WIDTH / 2, ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(WIDTH / 2, ROW));
        assertEquals(GL11.GL_NO_ERROR, error);
        assertEquals("", renderer.validationText());
    }

    private boolean submitPartOnTheWindow() {
        if (renderer.width() != widthOfTheWindow) {
            renderer.resize(widthOfTheWindow, HEIGHT);
        }
        return renderer.submitPart(recorder);
    }

    private void presentFrame() {
        mods.present();
        renderer.present(recorder);
        recorder.beginFrame();
    }

    private List<FlushReason> reasonsOfTheFlushes() {
        List<FlushReason> reasons = new ArrayList<>();
        for (FlushRecord flush : mods.layer().flushes().recent()) {
            reasons.add(flush.reason());
        }
        return reasons;
    }

    private static GameDraw redOverTheLeftQuarter() {
        float[][] corners = {
            {LEFT_EDGE, -1.0f}, {END_OF_THE_LEFT_QUARTER, -1.0f}, {END_OF_THE_LEFT_QUARTER, 1.0f},
            {LEFT_EDGE, -1.0f}, {END_OF_THE_LEFT_QUARTER, 1.0f}, {LEFT_EDGE, 1.0f},
        };
        ByteBuffer vertices = ByteBuffer.allocate(QUAD_VERTICES * VERTEX_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (float[] corner : corners) {
            vertices.putFloat(corner[0]).putFloat(corner[1]).putFloat(0.0f).putInt(RED_VERTEX);
        }
        vertices.flip();
        return GameDraw.builder(POSITION_COLOR, PrimitiveMode.TRIANGLES, vertices, QUAD_VERTICES)
                .view(GameView.builder(IDENTITY).build())
                .build();
    }

    private static void clearThroughLwjgl(int left, int width, int[] color) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, 0, width, HEIGHT);
        GL11.glClearColor(color[0] / 255.0F, color[1] / 255.0F, color[2] / 255.0F, color[3] / 255.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static ProviderBundle describedByItsManifest(ProviderBundle staged) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("mesa=" + staged.pin().mesaVersion());
        lines.add("entry=" + staged.entryLibrary());
        lines.add("dependencies=" + String.join(" ", staged.dependencies().keySet()));
        lines.add("sha256." + staged.entryLibrary() + "=" + staged.pin().libraryDigest().hex());
        for (Map.Entry<String, ArtifactDigest> dependency : staged.dependencies().entrySet()) {
            lines.add("sha256." + dependency.getKey() + "=" + dependency.getValue().hex());
        }
        for (Map.Entry<String, String> setting : staged.driverSettings().entrySet()) {
            lines.add("driver." + setting.getKey() + "=" + setting.getValue());
        }
        Files.write(staged.directory().resolve(ProviderManifest.FILE_NAME), lines, StandardCharsets.UTF_8);
        return ProviderManifest.read(staged.directory());
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

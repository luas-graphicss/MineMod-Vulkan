package io.github.luasgraphicss.minemod.client.compatibility.scene;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.client.compatibility.support.SharedSession;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.CompatibilitySession;
import io.github.luasgraphicss.minemod.compatibility.runtime.SessionContext;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.NativeBlock;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.OpenDescriptors;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushReason;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
@EnabledIfSystemProperty(named = SharedSession.LIBRARY, matches = ".+")
class SharedSceneOnZinkTest {
    private static final int GL_NO_ERROR = 0;
    private static final int GL_SCISSOR_TEST = 0x0C11;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE_WIDTH = 0x1000;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int OWN_EDGE = 4;
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final int LEFT = WIDTH / 4;
    private static final int RIGHT = 3 * WIDTH / 4;
    private static final int MIDDLE_ROW = HEIGHT / 2;
    private static final int UPPER_ROW = HEIGHT / 4;
    private static final int LOWER_ROW = 3 * HEIGHT / 4;
    private static final int[] BLUE = {0, 0, 255, 255};
    private static final int[] RED = {255, 0, 0, 255};
    private static final int WARM_UP_CYCLES = 3;
    private static final int REPLACEMENT_CYCLES = 100;

    @TempDir
    Path providerDirectory;

    private CompatibilitySession compatibility;
    private SharedSession renderer;
    private SharedScene scene;
    private SessionContext context;
    private CompatibilityLayer layer;
    private GlScene gl;
    private long frame;

    @BeforeEach
    void openAFrameOnTheAttachmentsOfASceneSession() throws Exception {
        compatibility =
                ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        renderer = SharedSession.open(WIDTH, HEIGHT);
        scene = new SharedScene(renderer.sharing(), renderer::submitRecordedPart);
        context = compatibility.createContext(ProviderTestEnvironment.planFor(scene, scene::handoffThrough));
        layer = context.layer();
        gl = new GlScene(layer);
        layer.beginFrame();
    }

    @AfterEach
    void closeTheSceneBeforeItsSessions() {
        scene.close();
        context.close();
        renderer.close();
        compatibility.close();
    }

    @Test
    void theSessionReadsWhatAModDrewOverTheFirstPartOfItsFrame() {
        renderer.recordFrameOver(BLUE);

        layer.enterCallback();
        int[] partSeenByTheMod = gl.pixelAt(RIGHT, MIDDLE_ROW);
        gl.call("glEnable", GL_SCISSOR_TEST);
        gl.call("glScissor", 0, 0, WIDTH / 2, HEIGHT);
        gl.clear(GlScene.GREEN);
        gl.call("glDisable", GL_SCISSOR_TEST);
        long error = gl.call("glGetError");
        layer.exitCallback();
        presentFrame();

        assertArrayEquals(BLUE, partSeenByTheMod);
        assertArrayEquals(GlScene.GREEN, renderer.pixelAt(LEFT, MIDDLE_ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(RIGHT, MIDDLE_ROW));
        assertEquals(GL_NO_ERROR, error);
        assertEquals(1, renderer.framesDrawn());
        assertEquals("", renderer.validationText());
    }

    @Test
    void whatAModDrawsAtTheBottomOfItsFramebufferIsAtTheBottomOfTheFrame() {
        renderer.recordFrameOver(BLUE);

        layer.enterCallback();
        gl.call("glEnable", GL_SCISSOR_TEST);
        gl.call("glScissor", 0, 0, WIDTH, HEIGHT / 2);
        gl.clear(GlScene.GREEN);
        gl.call("glDisable", GL_SCISSOR_TEST);
        int[] bottomSeenByTheMod = gl.pixelAt(LEFT, HEIGHT / 4);
        layer.exitCallback();
        presentFrame();

        assertArrayEquals(GlScene.GREEN, bottomSeenByTheMod);
        assertArrayEquals(GlScene.GREEN, renderer.pixelAt(LEFT, LOWER_ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(LEFT, UPPER_ROW));
        assertEquals("", renderer.validationText());
    }

    @Test
    void aModDrawsTwiceInAFrameWithTheSessionInBetween() {
        renderer.recordFrameOver(RED);

        layer.enterCallback();
        gl.call("glEnable", GL_SCISSOR_TEST);
        gl.call("glScissor", 0, 0, WIDTH / 2, HEIGHT);
        gl.clear(GlScene.GREEN);
        layer.exitCallback();
        layer.beforeNativePass();
        layer.enterCallback();
        int[] ownEarlierDrawing = gl.pixelAt(LEFT, MIDDLE_ROW);
        gl.call("glScissor", WIDTH / 2, 0, WIDTH / 2, HEIGHT);
        gl.clear(GlScene.MAGENTA);
        gl.call("glDisable", GL_SCISSOR_TEST);
        layer.exitCallback();
        presentFrame();

        assertArrayEquals(GlScene.GREEN, ownEarlierDrawing);
        assertArrayEquals(GlScene.GREEN, renderer.pixelAt(LEFT, MIDDLE_ROW));
        assertArrayEquals(GlScene.MAGENTA, renderer.pixelAt(RIGHT, MIDDLE_ROW));
        assertEquals(2, layer.flushes().count(FlushReason.CALLBACK_NEEDS_ATTACHMENTS));
        assertEquals(1, layer.flushes().count(FlushReason.NATIVE_PASS_NEEDS_ATTACHMENTS));
        assertEquals(1, layer.flushes().count(FlushReason.PRESENTATION));
        assertEquals("", renderer.validationText());
    }

    @Test
    void aFrameWithoutModDrawingIsNeverHandedOver() {
        renderer.recordFrameOver(RED);

        layer.enterCallback();
        gl.call("glEnable", GL_SCISSOR_TEST);
        gl.call("glDisable", GL_SCISSOR_TEST);
        layer.exitCallback();
        presentFrame();

        assertEquals(0, layer.flushes().recordedCount());
        assertArrayEquals(RED, renderer.pixelAt(LEFT, MIDDLE_ROW));
        assertEquals("", renderer.validationText());
    }

    @Test
    void aModDrawsWhileTheSessionDrawsNoFrameAndNothingIsHandedOver() {
        layer.enterCallback();
        gl.clear(GlScene.GREEN);
        long error = gl.call("glGetError");
        layer.exitCallback();
        layer.present();

        assertEquals(GL_NO_ERROR, error);
        assertEquals(0, renderer.framesDrawn());
        assertEquals("", renderer.validationText());
    }

    @Test
    void theAttachmentsOfAResizedSessionAreWhatTheModDrawsInto() {
        renderer.recordFrameOver(BLUE);
        presentFrame();
        renderer.resize(2 * WIDTH, HEIGHT);
        followTheSession();
        renderer.recordFrameOver(BLUE);

        layer.enterCallback();
        gl.call("glViewport", 0, 0, 2 * WIDTH, HEIGHT);
        gl.call("glEnable", GL_SCISSOR_TEST);
        gl.call("glScissor", WIDTH, 0, WIDTH, HEIGHT);
        gl.clear(GlScene.GREEN);
        gl.call("glDisable", GL_SCISSOR_TEST);
        long error = gl.call("glGetError");
        layer.exitCallback();
        presentFrame();

        assertArrayEquals(GlScene.GREEN, renderer.pixelAt(WIDTH + RIGHT, MIDDLE_ROW));
        assertArrayEquals(BLUE, renderer.pixelAt(LEFT, MIDDLE_ROW));
        assertEquals(GL_NO_ERROR, error);
        assertEquals("", renderer.validationText());
    }

    @Test
    void replacingTheAttachmentsLeavesTheBindingsOfTheModInPlace() {
        renderer.recordFrameOver(BLUE);
        presentFrame();
        layer.enterCallback();
        int framebuffer = gl.generate("glGenFramebuffers");
        int texture = gl.generate("glGenTextures");
        gl.call("glBindTexture", GL_TEXTURE_2D, texture);
        gl.call("glTexStorage2D", GL_TEXTURE_2D, 1, GL_RGBA8, OWN_EDGE, OWN_EDGE);
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer);
        gl.call("glFramebufferTexture2D", GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        layer.exitCallback();
        renderer.resize(2 * WIDTH, HEIGHT);
        followTheSession();
        renderer.recordFrameOver(BLUE);

        layer.enterCallback();
        gl.clear(GlScene.GREEN);
        int[] ownFramebuffer = gl.pixelAt(1, 1);
        int widthOfTheBoundTexture;
        try (NativeBlock answer = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetTexLevelParameteriv", GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, answer.address());
            widthOfTheBoundTexture = answer.intAt(0);
        }
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, 0);
        int[] sceneSeenByTheMod = gl.pixelAt(LEFT, MIDDLE_ROW);
        layer.exitCallback();
        presentFrame();

        assertArrayEquals(GlScene.GREEN, ownFramebuffer);
        assertEquals(OWN_EDGE, widthOfTheBoundTexture);
        assertArrayEquals(BLUE, sceneSeenByTheMod);
        assertArrayEquals(BLUE, renderer.pixelAt(LEFT, MIDDLE_ROW));
        assertEquals("", renderer.validationText());
    }

    @Test
    void attachmentsReplacedByResizesLeaveNoImportResourceOrDescriptorBehind() throws IOException {
        replaceTheAttachments(WARM_UP_CYCLES);
        long descriptorsBefore = OpenDescriptors.count();

        replaceTheAttachments(REPLACEMENT_CYCLES);

        assertEquals(descriptorsBefore, OpenDescriptors.count());
        assertEquals(2, scene.imports().liveImports());
        assertEquals(2, scene.imports().liveAttachments());
        assertEquals(0, scene.imports().retiringCount());
        assertEquals("", renderer.validationText());
    }

    private void replaceTheAttachments(int cycles) {
        for (int cycle = 0; cycle < cycles; cycle++) {
            renderer.resize(WIDTH + (cycle % 2 == 0 ? WIDTH : 0), HEIGHT);
            followTheSession();
            renderer.recordFrameOver(BLUE);
            layer.enterCallback();
            gl.clear(GlScene.GREEN);
            layer.exitCallback();
            presentFrame();
        }
    }

    private void followTheSession() {
        scene.followSession(frame).ifPresent(context::attachScene);
    }

    private void presentFrame() {
        layer.present();
        renderer.endFrame();
        scene.imports().collect(frame);
        frame++;
        layer.beginFrame();
    }
}

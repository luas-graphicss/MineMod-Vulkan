package io.github.luasgraphicss.minemod.compatibility.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.interop.device.DeviceIdentity;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.ShareableResource;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingNegotiator;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingOutcome;
import io.github.luasgraphicss.minemod.compatibility.interop.negotiation.SharingRequest;
import io.github.luasgraphicss.minemod.compatibility.interop.platform.HandlePlatform;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.BlockingHandoff;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlShaders;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.NativeBlock;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.OpenDescriptors;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.renderer.ImportedScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.renderer.RendererStandIn;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.renderer.StandInAttachment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
@EnabledIfSystemProperty(named = RendererStandIn.LIBRARY, matches = ".+")
class SharedSceneOnZinkTest {
    private static final int GL_NO_ERROR = 0;
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_DEPTH_TEST = 0x0B71;
    private static final int GL_SCISSOR_TEST = 0x0C11;
    private static final int GL_FLOAT = 0x1406;
    private static final int GL_ARRAY_BUFFER = 0x8892;
    private static final int GL_STATIC_DRAW = 0x88E4;
    private static final int GL_FRAGMENT_SHADER = 0x8B30;
    private static final int GL_VERTEX_SHADER = 0x8B31;
    private static final int GL_LINK_STATUS = 0x8B82;
    private static final int VK_FORMAT_R8G8B8A8_UNORM = 37;
    private static final int VK_FORMAT_D32_SFLOAT_S8_UINT = 130;
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final int LEFT = WIDTH / 4;
    private static final int RIGHT = 3 * WIDTH / 4;
    private static final int ROW = HEIGHT / 2;
    private static final int[] BLUE = {0, 0, 255, 255};
    private static final int[] RED = {255, 0, 0, 255};
    private static final float IN_FRONT_OF_THE_TRIANGLE = 0.25F;
    private static final float BEHIND_THE_TRIANGLE = 1.0F;
    private static final int WARM_UP_CYCLES = 5;
    private static final int SHARING_CYCLES = 1000;
    private static final SharingRequest COLOR = SharingRequest.image(
            ShareableResource.COLOR_ATTACHMENT, VK_FORMAT_R8G8B8A8_UNORM, WIDTH, HEIGHT, 1, 1);
    private static final SharingRequest DEPTH_STENCIL = SharingRequest.image(
            ShareableResource.DEPTH_STENCIL_ATTACHMENT, VK_FORMAT_D32_SFLOAT_S8_UINT, WIDTH, HEIGHT, 1, 1);
    private static final String VERTEX_SOURCE =
            "#version 150\nin vec2 position;\nvoid main() { gl_Position = vec4(position, 0.0, 1.0); }\n";
    private static final String FRAGMENT_SOURCE =
            "#version 150\nout vec4 color;\nvoid main() { color = vec4(0.0, 1.0, 0.0, 1.0); }\n";

    @TempDir
    Path providerDirectory;

    private CompatibilitySession session;
    private RendererStandIn renderer;
    private StandInAttachment color;
    private StandInAttachment depthStencil;
    private ImportedScene scene;
    private SessionContext context;
    private CompatibilityLayer layer;
    private GlScene gl;

    @BeforeEach
    void openAFrameOnTheAttachmentsOfAStandInRenderer() throws Exception {
        session = ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        renderer = RendererStandIn.open();
        color = renderer.createAttachment(COLOR);
        depthStencil = renderer.createAttachment(DEPTH_STENCIL);
        scene = new ImportedScene(color, depthStencil);
        context = session.createContext(ProviderTestEnvironment.planFor(scene, BlockingHandoff::new));
        layer = context.layer();
        gl = new GlScene(layer);
        layer.beginFrame();
    }

    @AfterEach
    void closeTheSessionAndTheStandIn() {
        context.close();
        scene.close();
        renderer.close();
        session.close();
    }

    @Test
    void theRendererReadsWhatAModDrewOverItsPass() {
        color.drawColor(0, 0, WIDTH, HEIGHT, BLUE);

        layer.enterCallback();
        gl.call("glEnable", GL_SCISSOR_TEST);
        gl.call("glScissor", 0, 0, WIDTH / 2, HEIGHT);
        gl.clear(GlScene.GREEN);
        gl.call("glDisable", GL_SCISSOR_TEST);
        int[] passSeenByTheMod = gl.pixelAt(RIGHT, ROW);
        long error = gl.call("glGetError");
        layer.exitCallback();
        layer.beforeNativePass();

        assertArrayEquals(BLUE, passSeenByTheMod);
        assertArrayEquals(GlScene.GREEN, color.colorAt(LEFT, ROW));
        assertArrayEquals(BLUE, color.colorAt(RIGHT, ROW));
        assertEquals(GL_NO_ERROR, error);
        assertEquals("", renderer.validationText());
    }

    @Test
    void aModReadsThePassTheRendererDrewBetweenItsCallbacks() {
        layer.enterCallback();
        gl.clear(GlScene.GREEN);
        layer.exitCallback();
        layer.beforeNativePass();
        color.drawColor(WIDTH / 2, 0, WIDTH / 2, HEIGHT, RED);

        layer.enterCallback();
        int[] ownDrawing = gl.pixelAt(LEFT, ROW);
        int[] passOfTheRenderer = gl.pixelAt(RIGHT, ROW);
        long error = gl.call("glGetError");
        layer.exitCallback();
        layer.present();

        assertArrayEquals(GlScene.GREEN, ownDrawing);
        assertArrayEquals(RED, passOfTheRenderer);
        assertEquals(GL_NO_ERROR, error);
        assertEquals("", renderer.validationText());
    }

    @Test
    void depthTheRendererWroteHidesWhatAModDrawsBehindIt() {
        color.drawColor(0, 0, WIDTH, HEIGHT, BLUE);
        depthStencil.drawDepth(IN_FRONT_OF_THE_TRIANGLE);

        layer.enterCallback();
        int program = greenProgram();
        drawCoveringTriangle(program);
        int[] behindTheDepthOfTheRenderer = gl.pixelAt(LEFT, ROW);
        layer.exitCallback();
        layer.beforeNativePass();
        depthStencil.drawDepth(BEHIND_THE_TRIANGLE);

        layer.enterCallback();
        drawCoveringTriangle(program);
        int[] inFrontOfTheDepthOfTheRenderer = gl.pixelAt(LEFT, ROW);
        long error = gl.call("glGetError");
        layer.exitCallback();
        layer.present();

        assertArrayEquals(BLUE, behindTheDepthOfTheRenderer);
        assertArrayEquals(GlScene.GREEN, inFrontOfTheDepthOfTheRenderer);
        assertArrayEquals(GlScene.GREEN, color.colorAt(LEFT, ROW));
        assertEquals(GL_NO_ERROR, error);
        assertEquals("", renderer.validationText());
    }

    @Test
    void aNegotiationOnOneGpuRefusesOnlyTheSharingTheDriversLack() {
        DeviceIdentity providerDevice = scene.providerIdentity();
        List<String> providerExtensions = scene.providerExtensions();

        SharingOutcome outcome = SharingNegotiator.negotiate(
                renderer.identity(),
                providerDevice,
                providerExtensions,
                HandlePlatform.LINUX_FILE_DESCRIPTOR,
                Arrays.asList(COLOR, DEPTH_STENCIL),
                renderer);

        assertTrue(renderer.identity().isSameGpuAs(providerDevice));
        assertEquals(sharingTheDriversLack(providerExtensions), outcome.rejections());
    }

    @Test
    void attachmentsSharedAndReleasedInALoopLeaveNoImportOrDescriptorBehind() throws IOException {
        shareAndRelease(WARM_UP_CYCLES);
        long descriptorsBefore = OpenDescriptors.count();

        shareAndRelease(SHARING_CYCLES);

        assertEquals(descriptorsBefore, OpenDescriptors.count());
        assertEquals(2, scene.liveImports());
        assertEquals(0, scene.descriptorsStillOwned());
        assertEquals("", renderer.validationText());
    }

    private void shareAndRelease(int cycles) {
        for (int cycle = 0; cycle < cycles; cycle++) {
            StandInAttachment shared = renderer.createAttachment(COLOR);
            scene.release(scene.importOf(shared));
            shared.destroy();
        }
        layer.enterCallback();
        gl.call("glFinish");
        layer.exitCallback();
    }

    private List<String> sharingTheDriversLack(List<String> providerExtensions) {
        List<String> lacking = new ArrayList<>();
        for (String extension : HandlePlatform.LINUX_FILE_DESCRIPTOR.requiredExtensions()) {
            if (!providerExtensions.contains(extension)) {
                lacking.add("the provider lacks " + extension);
            }
        }
        if (!renderer.binarySemaphoresShareable()) {
            lacking.add("binary semaphores cannot be shared on this device");
        }
        return lacking;
    }

    private int greenProgram() {
        int program = (int) gl.call("glCreateProgram");
        gl.call("glAttachShader", program, GlShaders.compile(gl, GL_VERTEX_SHADER, VERTEX_SOURCE));
        gl.call("glAttachShader", program, GlShaders.compile(gl, GL_FRAGMENT_SHADER, FRAGMENT_SOURCE));
        GlShaders.bindAttribute(gl, program, 0, "position");
        gl.call("glLinkProgram", program);
        assertEquals(1, GlShaders.programParameter(gl, program, GL_LINK_STATUS));
        return program;
    }

    private void drawCoveringTriangle(int program) {
        int vertexArray = gl.generate("glGenVertexArrays");
        int buffer = gl.generate("glGenBuffers");
        gl.call("glBindVertexArray", vertexArray);
        gl.call("glBindBuffer", GL_ARRAY_BUFFER, buffer);
        try (NativeBlock coveringTriangle = NativeBlock.ofFloats(-1, -1, 3, -1, -1, 3)) {
            gl.call("glBufferData", GL_ARRAY_BUFFER, coveringTriangle.size(), coveringTriangle.address(), GL_STATIC_DRAW);
        }
        gl.call("glVertexAttribPointer", 0, 2, GL_FLOAT, 0, 0, 0);
        gl.call("glEnableVertexAttribArray", 0);
        gl.call("glViewport", 0, 0, WIDTH, HEIGHT);
        gl.call("glEnable", GL_DEPTH_TEST);
        gl.call("glUseProgram", program);
        gl.call("glDrawArrays", GL_TRIANGLES, 0, 3);
    }
}

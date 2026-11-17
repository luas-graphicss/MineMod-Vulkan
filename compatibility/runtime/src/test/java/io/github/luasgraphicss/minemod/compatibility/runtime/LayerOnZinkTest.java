package io.github.luasgraphicss.minemod.compatibility.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportedOperation;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlShaders;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlWords;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.NativeBlock;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class LayerOnZinkTest {
    private static final int GL_NO_ERROR = 0;
    private static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x0001;
    private static final int GL_BACK_LEFT = 0x0402;
    private static final int GL_BACK = 0x0405;
    private static final int GL_INVALID_ENUM = 0x0500;
    private static final int GL_INVALID_VALUE = 0x0501;
    private static final int GL_INVALID_OPERATION = 0x0502;
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_PACK_ALIGNMENT = 0x0D05;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_FLOAT = 0x1406;
    private static final int GL_RGB = 0x1907;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_NEAREST = 0x2600;
    private static final int GL_TEXTURE_MIN_FILTER = 0x2801;
    private static final int GL_COLOR_BUFFER_BIT = 0x4000;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int GL_FRAMEBUFFER_DEFAULT = 0x8218;
    private static final int GL_NUM_EXTENSIONS = 0x821D;
    private static final int GL_QUERY_RESULT = 0x8866;
    private static final int GL_ARRAY_BUFFER = 0x8892;
    private static final int GL_STATIC_DRAW = 0x88E4;
    private static final int GL_SAMPLES_PASSED = 0x8914;
    private static final int GL_SAMPLER_BINDING = 0x8919;
    private static final int GL_FRAGMENT_SHADER = 0x8B30;
    private static final int GL_VERTEX_SHADER = 0x8B31;
    private static final int GL_COMPILE_STATUS = 0x8B81;
    private static final int GL_LINK_STATUS = 0x8B82;
    private static final int GL_INFO_LOG_LENGTH = 0x8B84;
    private static final int GL_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    private static final int GL_ATTACHMENT_OBJECT_TYPE = 0x8CD0;
    private static final int GL_ATTACHMENT_OBJECT_NAME = 0x8CD1;
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    private static final int GL_ALREADY_SIGNALED = 0x911A;
    private static final int GL_CONDITION_SATISFIED = 0x911C;
    private static final int GL_EXTENSIONS = 0x1F03;
    private static final int WIDTH = ProviderTestEnvironment.SCENE_WIDTH;
    private static final int HEIGHT = ProviderTestEnvironment.SCENE_HEIGHT;
    private static final int UNANSWERED = 0x5A5A5A5A;
    private static final long ONE_SECOND_IN_NANOSECONDS = 1_000_000_000L;
    private static final String UNRESOLVED_VERTEX_SOURCE =
            "#version 150\nvoid missing();\nvoid main() { missing(); gl_Position = vec4(0.0); }\n";
    private static final String VERTEX_SOURCE =
            "#version 150\nin vec2 position;\nvoid main() { gl_Position = vec4(position, 0.0, 1.0); }\n";
    private static final String FRAGMENT_SOURCE =
            "#version 150\nuniform vec4 tint;\nout vec4 color;\nvoid main() { color = tint; }\n";

    @TempDir
    Path providerDirectory;

    private CompatibilitySession session;
    private SessionContext context;
    private CompatibilityLayer layer;
    private GlScene gl;

    @BeforeEach
    void openACoreContext() throws Exception {
        session = ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        context = session.createContext(ProviderTestEnvironment.standalonePlan());
        layer = context.layer();
        gl = new GlScene(layer);
        layer.beginFrame();
        layer.enterCallback();
        gl.call("glViewport", 0, 0, WIDTH, HEIGHT);
    }

    @AfterEach
    void closeTheSession() {
        context.close();
        session.close();
    }

    @Test
    void theDefaultFramebufferIsTheSceneAndReportsNameZero() {
        gl.clear(GlScene.MAGENTA);

        assertArrayEquals(GlScene.MAGENTA, gl.pixelAt(WIDTH / 2, HEIGHT / 2));
        assertEquals(0, gl.integer(GL_FRAMEBUFFER_BINDING));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aFramebufferOfTheModKeepsItsOwnPixelsAndItsVisibleNames() {
        gl.clear(GlScene.MAGENTA);
        int framebuffer = gl.generate("glGenFramebuffers");
        int texture = gl.generate("glGenTextures");
        gl.call("glBindTexture", GL_TEXTURE_2D, texture);
        gl.call("glTexImage2D", GL_TEXTURE_2D, 0, GL_RGBA8, 4, 4, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer);
        gl.call("glFramebufferTexture2D", GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);

        assertEquals(GL_FRAMEBUFFER_COMPLETE, gl.call("glCheckFramebufferStatus", GL_FRAMEBUFFER));
        gl.clear(GlScene.GREEN);
        assertArrayEquals(GlScene.GREEN, gl.pixelAt(1, 1));
        assertEquals(framebuffer, gl.integer(GL_FRAMEBUFFER_BINDING));
        assertEquals(texture, gl.integer(GL_TEXTURE_BINDING_2D));
        try (NativeBlock attached = new NativeBlock(Integer.BYTES)) {
            gl.call(
                    "glGetFramebufferAttachmentParameteriv",
                    GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_ATTACHMENT_OBJECT_NAME, attached.address());
            assertEquals(texture, attached.intAt(0));
        }

        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, 0);
        assertArrayEquals(GlScene.MAGENTA, gl.pixelAt(1, 1));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aPartialBlitReachesOnlyItsRectangleOfTheScene() {
        gl.clear(GlScene.MAGENTA);
        int framebuffer = gl.generate("glGenFramebuffers");
        int texture = gl.generate("glGenTextures");
        gl.call("glBindTexture", GL_TEXTURE_2D, texture);
        gl.call("glTexImage2D", GL_TEXTURE_2D, 0, GL_RGBA8, 4, 4, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, framebuffer);
        gl.call("glFramebufferTexture2D", GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0);
        gl.clear(GlScene.GREEN);

        gl.call("glBindFramebuffer", GL_READ_FRAMEBUFFER, framebuffer);
        gl.call("glBindFramebuffer", GL_DRAW_FRAMEBUFFER, 0);
        gl.call("glBlitFramebuffer", 0, 0, 4, 4, 8, 8, 12, 12, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        gl.call("glBindFramebuffer", GL_FRAMEBUFFER, 0);

        assertArrayEquals(GlScene.GREEN, gl.pixelAt(9, 9));
        assertArrayEquals(GlScene.MAGENTA, gl.pixelAt(13, 9));
        assertArrayEquals(GlScene.MAGENTA, gl.pixelAt(7, 9));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aReadbackFollowsThePackAlignmentOfTheCaller() {
        gl.clear(GlScene.MAGENTA);
        int rowOfThreePixels = 9;
        int paddedRow = 12;

        try (NativeBlock tight = new NativeBlock(2 * paddedRow); NativeBlock padded = new NativeBlock(2 * paddedRow)) {
            tight.fill(0x5A);
            padded.fill(0x5A);
            gl.call("glPixelStorei", GL_PACK_ALIGNMENT, 1);
            gl.call("glReadPixels", 0, 0, 3, 2, GL_RGB, GL_UNSIGNED_BYTE, tight.address());
            gl.call("glPixelStorei", GL_PACK_ALIGNMENT, 4);
            gl.call("glReadPixels", 0, 0, 3, 2, GL_RGB, GL_UNSIGNED_BYTE, padded.address());

            assertEquals(255, tight.byteAt(rowOfThreePixels));
            assertEquals(0x5A, tight.byteAt(2 * rowOfThreePixels));
            assertEquals(0x5A, padded.byteAt(rowOfThreePixels));
            assertEquals(255, padded.byteAt(paddedRow));
        }
    }

    @Test
    void errorsOfTheProviderAndOfTheLayerAreReadOnceAndInOrder() {
        gl.call("glEnable", 0x7FFF);
        assertEquals(GL_INVALID_ENUM, gl.call("glGetError"));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));

        try (NativeBlock names = new NativeBlock(Integer.BYTES)) {
            gl.call("glGenTextures", -1, names.address());
        }
        assertEquals(GL_INVALID_VALUE, gl.call("glGetError"));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aShaderThatDoesNotCompileKeepsItsStatusAndItsLog() {
        int shader = GlShaders.compile(gl, GL_FRAGMENT_SHADER, "#version 150\nvoid main() { this is not GLSL }\n");

        assertEquals(0, GlShaders.shaderParameter(gl, shader, GL_COMPILE_STATUS));
        assertTrue(GlShaders.shaderParameter(gl, shader, GL_INFO_LOG_LENGTH) > 1);
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aProgramThatDoesNotLinkKeepsItsStatusItsLogAndTheErrorOfItsUse() {
        int program = (int) gl.call("glCreateProgram");
        gl.call("glAttachShader", program, compiled(GL_VERTEX_SHADER, UNRESOLVED_VERTEX_SOURCE));
        gl.call("glAttachShader", program, compiled(GL_FRAGMENT_SHADER, FRAGMENT_SOURCE));
        gl.call("glLinkProgram", program);

        assertEquals(0, GlShaders.programParameter(gl, program, GL_LINK_STATUS));
        assertTrue(GlShaders.programParameter(gl, program, GL_INFO_LOG_LENGTH) > 1);
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
        gl.call("glUseProgram", program);
        assertEquals(GL_INVALID_OPERATION, gl.call("glGetError"));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void readAndDrawBindingsOfTheDefaultFramebufferAreSeparate() {
        int framebuffer = gl.generate("glGenFramebuffers");
        gl.call("glBindFramebuffer", GL_READ_FRAMEBUFFER, framebuffer);

        assertEquals(0, gl.integer(GL_FRAMEBUFFER_BINDING));
        assertEquals(framebuffer, gl.integer(GL_READ_FRAMEBUFFER_BINDING));
        gl.call("glBindFramebuffer", GL_READ_FRAMEBUFFER, 0);
        assertEquals(0, gl.integer(GL_READ_FRAMEBUFFER_BINDING));
        try (NativeBlock attached = new NativeBlock(Integer.BYTES)) {
            gl.call(
                    "glGetFramebufferAttachmentParameteriv",
                    GL_DRAW_FRAMEBUFFER, GL_BACK_LEFT, GL_ATTACHMENT_OBJECT_TYPE, attached.address());
            assertEquals(GL_FRAMEBUFFER_DEFAULT, attached.intAt(0));
        }
        gl.call("glDrawBuffer", GL_BACK);
        gl.call("glReadBuffer", GL_BACK);
        gl.clear(GlScene.GREEN);
        assertArrayEquals(GlScene.GREEN, gl.pixelAt(0, 0));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aSamplerKeepsItsParametersAndIsReportedByTheUnitItIsBoundTo() {
        int sampler = gl.generate("glGenSamplers");
        gl.call("glBindSampler", 0, sampler);
        gl.call("glSamplerParameteri", sampler, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        try (NativeBlock filter = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetSamplerParameteriv", sampler, GL_TEXTURE_MIN_FILTER, filter.address());
            assertEquals(GL_NEAREST, filter.intAt(0));
        }
        assertEquals(sampler, gl.integer(GL_SAMPLER_BINDING));
        assertEquals(1, gl.call("glIsSampler", sampler));
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aFenceIsSignaledOnceTheProviderFinishedTheCommandsBeforeIt() {
        gl.clear(GlScene.GREEN);
        long fence = gl.call("glFenceSync", GL_SYNC_GPU_COMMANDS_COMPLETE, 0);

        long outcome = gl.call("glClientWaitSync", fence, GL_SYNC_FLUSH_COMMANDS_BIT, ONE_SECOND_IN_NANOSECONDS);

        assertNotEquals(0, fence);
        assertTrue(
                outcome == GL_ALREADY_SIGNALED || outcome == GL_CONDITION_SATISFIED, "0x" + Long.toHexString(outcome));
        assertEquals(1, gl.call("glIsSync", fence));
        gl.call("glDeleteSync", fence);
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aProgramDrawsFromAVertexBufferIntoTheSceneAndAQueryCountsItsSamples() {
        gl.clear(GlScene.BLACK);
        int program = linkedProgram();
        int vertexArray = gl.generate("glGenVertexArrays");
        int buffer = gl.generate("glGenBuffers");
        int query = gl.generate("glGenQueries");
        gl.call("glBindVertexArray", vertexArray);
        gl.call("glBindBuffer", GL_ARRAY_BUFFER, buffer);
        try (NativeBlock coveringTriangle = NativeBlock.ofFloats(-1, -1, 3, -1, -1, 3)) {
            gl.call("glBufferData", GL_ARRAY_BUFFER, coveringTriangle.size(), coveringTriangle.address(), GL_STATIC_DRAW);
        }
        gl.call("glVertexAttribPointer", 0, 2, GL_FLOAT, 0, 0, 0);
        gl.call("glEnableVertexAttribArray", 0);
        gl.call("glUseProgram", program);
        try (NativeBlock tint = NativeBlock.ofText("tint")) {
            int location = (int) gl.call("glGetUniformLocation", program, tint.address());
            gl.call("glUniform4f", location, GlWords.of(0.0F), GlWords.of(1.0F), GlWords.of(0.0F), GlWords.of(1.0F));
        }

        gl.call("glBeginQuery", GL_SAMPLES_PASSED, query);
        gl.call("glDrawArrays", GL_TRIANGLES, 0, 3);
        gl.call("glEndQuery", GL_SAMPLES_PASSED);

        assertArrayEquals(GlScene.GREEN, gl.pixelAt(WIDTH / 2, HEIGHT / 2));
        try (NativeBlock samples = new NativeBlock(Integer.BYTES)) {
            gl.call("glGetQueryObjectuiv", query, GL_QUERY_RESULT, samples.address());
            assertEquals(WIDTH * HEIGHT, samples.intAt(0));
        }
        assertEquals(GL_NO_ERROR, gl.call("glGetError"));
    }

    @Test
    void aQueryThatSpansANativePassIsReportedAndNeverAnswered() {
        int query = gl.generate("glGenQueries");
        gl.call("glBeginQuery", GL_SAMPLES_PASSED, query);
        layer.exitCallback();
        layer.beforeNativePass();
        layer.enterCallback();
        gl.call("glEndQuery", GL_SAMPLES_PASSED);

        try (NativeBlock samples = new NativeBlock(Integer.BYTES)) {
            samples.setInt(0, UNANSWERED);
            gl.call("glGetQueryObjectuiv", query, GL_QUERY_RESULT, samples.address());
            assertEquals(UNANSWERED, samples.intAt(0));
        }
        List<String> reported = layer.reports().reportedOperations().stream()
                .map(ReportedOperation::toString)
                .collect(Collectors.toList());
        assertTrue(reported.stream().anyMatch(line -> line.contains("query")), reported.toString());
    }

    @Test
    void onlyExposedExtensionsAreListedAndEachOneIsReadable() {
        int count = gl.integer(GL_NUM_EXTENSIONS);

        assertEquals(session.exposedExtensions().size(), count);
        assertNotEquals(0, gl.call("glGetStringi", GL_EXTENSIONS, 0));
        assertEquals(0, gl.call("glGetStringi", GL_EXTENSIONS, count));
        assertEquals(GL_INVALID_VALUE, gl.call("glGetError"));
    }

    @Test
    void wrapperStateIsAppliedOnEntryAndAdoptedOrRestoredOnExit() {
        layer.exitCallback();
        layer.wrapperState().set(ProviderTestEnvironment.GL_BLEND, true);

        layer.enterCallback();
        assertEquals(1, gl.call("glIsEnabled", ProviderTestEnvironment.GL_BLEND));
        gl.call("glDisable", ProviderTestEnvironment.GL_BLEND);
        gl.call("glEnable", ProviderTestEnvironment.GL_DEPTH_TEST);
        layer.exitCallback();

        assertTrue(layer.wrapperState().isEnabled(ProviderTestEnvironment.GL_BLEND));
        assertTrue(layer.wrapperState().isEnabled(ProviderTestEnvironment.GL_DEPTH_TEST));
        layer.enterCallback();
        assertEquals(1, gl.call("glIsEnabled", ProviderTestEnvironment.GL_BLEND));
        assertEquals(1, gl.call("glIsEnabled", ProviderTestEnvironment.GL_DEPTH_TEST));
    }

    private int linkedProgram() {
        int vertex = compiled(GL_VERTEX_SHADER, VERTEX_SOURCE);
        int fragment = compiled(GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);
        int program = (int) gl.call("glCreateProgram");
        gl.call("glAttachShader", program, vertex);
        gl.call("glAttachShader", program, fragment);
        GlShaders.bindAttribute(gl, program, 0, "position");
        gl.call("glLinkProgram", program);
        assertEquals(1, GlShaders.programParameter(gl, program, GL_LINK_STATUS));
        return program;
    }

    private int compiled(int type, String source) {
        int shader = GlShaders.compile(gl, type, source);
        assertEquals(1, GlShaders.shaderParameter(gl, shader, GL_COMPILE_STATUS));
        return shader;
    }
}

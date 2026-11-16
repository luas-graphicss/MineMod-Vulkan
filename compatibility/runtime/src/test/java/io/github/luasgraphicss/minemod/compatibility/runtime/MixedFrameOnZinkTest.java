package io.github.luasgraphicss.minemod.compatibility.runtime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.compatibility.contracts.report.ReportedOperation;
import io.github.luasgraphicss.minemod.compatibility.layer.CompatibilityLayer;
import io.github.luasgraphicss.minemod.compatibility.provider.capability.ContextProfile;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlScene;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.GlShaders;
import io.github.luasgraphicss.minemod.compatibility.runtime.fixtures.ProviderTestEnvironment;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushReason;
import io.github.luasgraphicss.minemod.compatibility.state.submission.FlushRecord;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

@EnabledIfSystemProperty(named = ProviderTestEnvironment.SYSTEM_PROVIDER, matches = ".+")
class MixedFrameOnZinkTest {
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_STATIC_DRAW = 0x88E4;
    private static final int GL_FRAGMENT_SHADER = 0x8B30;
    private static final int GL_VERTEX_SHADER = 0x8B31;
    private static final int GL_LINK_STATUS = 0x8B82;
    private static final int GL_TRANSFORM_FEEDBACK_BUFFER = 0x8C8E;
    private static final int CAPTURE_BYTES = 256;
    private static final long FIRST_FRAME = 1;
    private static final String VERTEX_SOURCE =
            "#version 150\nin vec2 position;\nvoid main() { gl_Position = vec4(position, 0.0, 1.0); }\n";
    private static final String FRAGMENT_SOURCE = "#version 150\nout vec4 color;\nvoid main() { color = vec4(1.0); }\n";

    @TempDir
    Path providerDirectory;

    private CompatibilitySession session;
    private SessionContext context;
    private CompatibilityLayer layer;
    private GlScene gl;

    @BeforeEach
    void openAFrame() throws Exception {
        session = ProviderTestEnvironment.open(ProviderTestEnvironment.stage(providerDirectory), ContextProfile.CORE);
        context = session.createContext(ProviderTestEnvironment.standalonePlan());
        layer = context.layer();
        gl = new GlScene(layer);
        layer.beginFrame();
    }

    @AfterEach
    void closeTheSession() {
        context.close();
        session.close();
    }

    @Test
    void aCallbackBetweenNativePassesFlushesOnceForEachChangeOfOwner() {
        layer.wrapperState().set(ProviderTestEnvironment.GL_BLEND, true);
        layer.enterCallback();
        gl.clear(GlScene.MAGENTA);
        layer.exitCallback();
        layer.beforeNativePass();
        layer.enterCallback();
        int[] seenAfterTheNativePass = gl.pixelAt(0, 0);
        long blendAfterTheNativePass = gl.call("glIsEnabled", ProviderTestEnvironment.GL_BLEND);
        layer.exitCallback();
        layer.present();

        assertArrayEquals(GlScene.MAGENTA, seenAfterTheNativePass);
        assertEquals(1, blendAfterTheNativePass);
        assertEquals(
                Arrays.asList(
                        new FlushRecord(FIRST_FRAME, 0, FlushReason.CALLBACK_NEEDS_ATTACHMENTS),
                        new FlushRecord(FIRST_FRAME, 1, FlushReason.NATIVE_PASS_NEEDS_ATTACHMENTS),
                        new FlushRecord(FIRST_FRAME, 2, FlushReason.CALLBACK_NEEDS_ATTACHMENTS),
                        new FlushRecord(FIRST_FRAME, 3, FlushReason.SYNCHRONOUS_READ),
                        new FlushRecord(FIRST_FRAME, 4, FlushReason.PRESENTATION)),
                layer.flushes().recent());
    }

    @Test
    void aFrameWithoutProviderWorkFlushesNothing() {
        layer.enterCallback();
        gl.call("glViewport", 0, 0, ProviderTestEnvironment.SCENE_WIDTH, ProviderTestEnvironment.SCENE_HEIGHT);
        layer.exitCallback();
        layer.beforeNativePass();
        layer.present();

        assertEquals(0, layer.flushes().recordedCount());
    }

    @Test
    void transformFeedbackThatSpansANativePassIsReported() {
        layer.enterCallback();
        int program = programCapturingItsPosition();
        int buffer = gl.generate("glGenBuffers");
        gl.call("glBindBufferBase", GL_TRANSFORM_FEEDBACK_BUFFER, 0, buffer);
        gl.call("glBufferData", GL_TRANSFORM_FEEDBACK_BUFFER, CAPTURE_BYTES, 0, GL_STATIC_DRAW);
        gl.call("glUseProgram", program);

        gl.call("glBeginTransformFeedback", GL_TRIANGLES);
        layer.exitCallback();
        layer.beforeNativePass();
        layer.enterCallback();
        gl.call("glEndTransformFeedback");

        List<String> reported = layer.reports().reportedOperations().stream()
                .map(ReportedOperation::toString)
                .collect(Collectors.toList());
        assertTrue(reported.stream().anyMatch(line -> line.contains("transform feedback")), reported.toString());
    }

    private int programCapturingItsPosition() {
        int program = (int) gl.call("glCreateProgram");
        gl.call("glAttachShader", program, GlShaders.compile(gl, GL_VERTEX_SHADER, VERTEX_SOURCE));
        gl.call("glAttachShader", program, GlShaders.compile(gl, GL_FRAGMENT_SHADER, FRAGMENT_SOURCE));
        GlShaders.captureInterleaved(gl, program, "gl_Position");
        gl.call("glLinkProgram", program);
        assertEquals(1, GlShaders.programParameter(gl, program, GL_LINK_STATUS));
        return program;
    }
}

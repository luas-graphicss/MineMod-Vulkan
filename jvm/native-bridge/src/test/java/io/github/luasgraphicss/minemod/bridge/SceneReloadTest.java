package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.contract.abi.AbiException;
import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SceneReloadTest {
    private static final int EDGE = 64;
    private static final int PROGRAM = 7;

    @TempDir
    Path scratch;

    private NativeSceneHost host;
    private NativeSceneSession session;
    private ShaderCompilation compiled;

    @BeforeEach
    void openASessionAndCompileAProgram() {
        Assumptions.assumeTrue(LoadedBridge.BRIDGE.drawsScenes());
        try (NativeShaderService service = CoreShaderSamples.openService(scratch)) {
            compiled = service.compile(CoreShaderSamples.tinted("position_tex_color"));
        }
        try {
            host = LoadedBridge.BRIDGE.openSceneHost(false, Collections.<String>emptyList());
            session = host.openSession(SceneSessionSetup.offscreen(EDGE, EDGE));
        } catch (AbiException unavailable) {
            Assumptions.abort("no Vulkan device draws scenes here: " + unavailable.getMessage());
        }
    }

    @AfterEach
    void closeTheSession() {
        if (session != null) {
            session.close();
        }
        if (host != null) {
            host.close();
        }
    }

    @Test
    void publishesStagedProgramsAsANewGeneration() {
        long generation = session.diagnostics().metrics().resourceGeneration();
        SceneReload reload = session.reload();

        reload.begin();
        assertTrue(reload.stageProgram(PROGRAM, compiled.vertexWords(), compiled.fragmentWords()), reload.report());
        assertTrue(reload.commit(), reload.report());

        assertEquals(generation + 1, session.diagnostics().metrics().resourceGeneration());
        assertEquals("", reload.report());
    }

    @Test
    void keepsTheGenerationWhenAProgramIsRefused() {
        long generation = session.diagnostics().metrics().resourceGeneration();
        SceneReload reload = session.reload();
        int[] truncated = {CoreShaderSamples.SPIRV_MAGIC};

        reload.begin();
        assertFalse(reload.stageProgram(PROGRAM, truncated, compiled.fragmentWords()));
        assertTrue(reload.report().startsWith("program " + PROGRAM + ": "), reload.report());
        assertFalse(reload.commit());

        assertEquals(generation, session.diagnostics().metrics().resourceGeneration());
    }

    @Test
    void forgetsStagedProgramsWhenTheReloadIsAbandoned() {
        long generation = session.diagnostics().metrics().resourceGeneration();
        SceneReload reload = session.reload();

        reload.begin();
        assertTrue(reload.stageProgram(PROGRAM, compiled.vertexWords(), compiled.fragmentWords()), reload.report());
        reload.abort();

        assertFalse(reload.commit());
        assertEquals(generation, session.diagnostics().metrics().resourceGeneration());
    }

    @Test
    void describesTheSessionInAReport() {
        String report = session.diagnostics().report();

        assertTrue(report.contains("generation"), report);
    }

    @Test
    void refusesToReloadAClosedSession() {
        SceneReload reload = session.reload();
        session.close();

        assertThrows(IllegalStateException.class, reload::begin);
    }
}

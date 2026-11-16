package io.github.luasgraphicss.minemod.client.reload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.bridge.CoreShaderSource;
import io.github.luasgraphicss.minemod.bridge.ShaderCompilation;
import io.github.luasgraphicss.minemod.client.shaders.ShaderCompiler;
import io.github.luasgraphicss.minemod.codec.scene.BatchState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResourceReloadTest {
    private static final CoreShaderSource SOLID = shader("rendertype_solid");
    private static final CoreShaderSource TEXT = shader("rendertype_text");

    private final RecordingStaging staging = new RecordingStaging();
    private final ScriptedCompiler compiler = new ScriptedCompiler();
    private final ResourceReload reload = new ResourceReload(staging, compiler);

    @Test
    void publishesEveryReplacedShaderInOneGeneration() {
        ReloadOutcome outcome = reload.publish(reload.prepare(Arrays.asList(SOLID, TEXT)));

        assertEquals(ReloadOutcome.Status.PUBLISHED, outcome.status());
        assertEquals(2, outcome.replacedPrograms());
        assertEquals(Arrays.asList("begin", "stage 1", "stage 2", "commit"), staging.calls);
        assertEquals(1, reload.programs().programOf("rendertype_solid"));
        assertEquals(2, reload.programs().programOf("rendertype_text"));
    }

    @Test
    void compilesBeforeTheRendererIsTouched() {
        reload.prepare(Arrays.asList(SOLID, TEXT));

        assertEquals(Arrays.asList("rendertype_solid", "rendertype_text"), compiler.compiled);
        assertEquals(Collections.emptyList(), staging.calls);
    }

    @Test
    void keepsThePublishedProgramsWhenAShaderDoesNotCompile() {
        reload.publish(reload.prepare(Collections.singletonList(SOLID)));
        staging.calls.clear();
        compiler.broken.add("rendertype_text");

        ReloadOutcome outcome = reload.publish(reload.prepare(Arrays.asList(SOLID, TEXT)));

        assertEquals(ReloadOutcome.Status.REJECTED, outcome.status());
        assertEquals("rendertype_text: 0:3: syntax error", outcome.report());
        assertTrue(outcome.keptThePreviousGeneration());
        assertEquals(Collections.emptyList(), staging.calls);
        assertEquals(1, reload.programs().programOf("rendertype_solid"));
        assertEquals(BatchState.BUILT_IN_PROGRAM, reload.programs().programOf("rendertype_text"));
    }

    @Test
    void abandonsTheGenerationWhenTheRendererRefusesAProgram() {
        staging.refusedProgram = 2;

        ReloadOutcome outcome = reload.publish(reload.prepare(Arrays.asList(SOLID, TEXT)));

        assertEquals(ReloadOutcome.Status.REJECTED, outcome.status());
        assertEquals("rendertype_text: program 2: the vertex stage reads an unknown input", outcome.report());
        assertEquals(Arrays.asList("begin", "stage 1", "stage 2", "abort"), staging.calls);
        assertEquals(BatchState.BUILT_IN_PROGRAM, reload.programs().programOf("rendertype_solid"));
    }

    @Test
    void keepsThePublishedProgramsWhenTheRendererRefusesToPublish() {
        staging.refusesCommit = true;

        ReloadOutcome outcome = reload.publish(reload.prepare(Collections.singletonList(SOLID)));

        assertEquals(ReloadOutcome.Status.REJECTED, outcome.status());
        assertEquals(Arrays.asList("begin", "stage 1", "commit"), staging.calls);
        assertEquals(BatchState.BUILT_IN_PROGRAM, reload.programs().programOf("rendertype_solid"));
    }

    @Test
    void dropsAReloadThatANewerOneSuperseded() {
        PreparedReload stale = reload.prepare(Collections.singletonList(SOLID));
        PreparedReload latest = reload.prepare(Collections.singletonList(TEXT));

        assertEquals(ReloadOutcome.Status.SUPERSEDED, reload.publish(stale).status());
        assertEquals(Collections.emptyList(), staging.calls);
        assertEquals(ReloadOutcome.Status.NOT_RELOADED, reload.lastOutcome().status());

        assertEquals(ReloadOutcome.Status.PUBLISHED, reload.publish(latest).status());
        assertEquals(BatchState.BUILT_IN_PROGRAM, reload.programs().programOf("rendertype_solid"));
    }

    @Test
    void dropsAReloadPreparedForAnotherRenderer() {
        ResourceReload restarted = new ResourceReload(staging, compiler);

        PreparedReload prepared = reload.prepare(Collections.singletonList(SOLID));
        restarted.prepare(Collections.singletonList(SOLID));

        assertEquals(ReloadOutcome.Status.SUPERSEDED, restarted.publish(prepared).status());
        assertEquals(Collections.emptyList(), staging.calls);
    }

    @Test
    void returnsToTheBuiltInProgramsWhenNoPackReplacesShaders() {
        reload.publish(reload.prepare(Collections.singletonList(SOLID)));
        staging.calls.clear();

        ReloadOutcome outcome = reload.publish(reload.prepare(Collections.<CoreShaderSource>emptyList()));

        assertEquals(ReloadOutcome.Status.PUBLISHED, outcome.status());
        assertEquals(0, outcome.replacedPrograms());
        assertEquals(Arrays.asList("begin", "commit"), staging.calls);
        assertEquals(BatchState.BUILT_IN_PROGRAM, reload.programs().programOf("rendertype_solid"));
    }

    @Test
    void abandonsTheStagedProgramsWhenStagingFails() {
        staging.failsToStage = true;
        PreparedReload prepared = reload.prepare(Collections.singletonList(SOLID));

        assertThrows(IllegalStateException.class, () -> reload.publish(prepared));

        assertEquals(Arrays.asList("begin", "abort"), staging.calls);
        assertEquals(BatchState.BUILT_IN_PROGRAM, reload.programs().programOf("rendertype_solid"));
    }

    @Test
    void refusesMoreShadersThanAGenerationHolds() {
        List<CoreShaderSource> excessive = new ArrayList<>();
        for (int index = 0; index <= ResourceReload.MAX_PROGRAMS; index++) {
            excessive.add(shader("shader_" + index));
        }

        ReloadOutcome outcome = reload.publish(reload.prepare(excessive));

        assertEquals(ReloadOutcome.Status.REJECTED, outcome.status());
        assertEquals(Collections.emptyList(), compiler.compiled);
        assertEquals(Collections.emptyList(), staging.calls);
    }

    @Test
    void stopsTheCompilerWhenItCloses() {
        assertFalse(compiler.isClosed);

        reload.close();

        assertTrue(compiler.isClosed);
    }

    private static CoreShaderSource shader(String name) {
        String stage = "void main() {}\n";
        return new CoreShaderSource(name, stage, stage, Collections.<String, String>emptyMap());
    }

    private static final class RecordingStaging implements ProgramStaging {
        private final List<String> calls = new ArrayList<>();
        private int refusedProgram;
        private boolean refusesCommit;
        private boolean failsToStage;

        @Override
        public void begin() {
            calls.add("begin");
        }

        @Override
        public boolean stage(int program, int[] vertexWords, int[] fragmentWords) {
            if (failsToStage) {
                throw new IllegalStateException("the device was lost");
            }
            calls.add("stage " + program);
            return program != refusedProgram;
        }

        @Override
        public boolean commit() {
            calls.add("commit");
            return !refusesCommit;
        }

        @Override
        public void abort() {
            calls.add("abort");
        }

        @Override
        public String report() {
            return "program " + refusedProgram + ": the vertex stage reads an unknown input\n";
        }
    }

    private static final class ScriptedCompiler implements ShaderCompiler {
        private final Set<String> broken = new HashSet<>();
        private final List<String> compiled = new ArrayList<>();
        private boolean isClosed;

        @Override
        public ShaderCompilation compile(CoreShaderSource shader) {
            compiled.add(shader.name());
            return broken.contains(shader.name())
                    ? ShaderCompilation.failed("0:3: syntax error\n")
                    : ShaderCompilation.succeeded(new int[] {1}, new int[] {2}, "");
        }

        @Override
        public void close() {
            isClosed = true;
        }
    }
}

package io.github.luasgraphicss.minemod.client.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.bridge.SceneMetrics;
import io.github.luasgraphicss.minemod.client.reload.ReloadOutcome;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosticLinesTest {
    private static final SceneMetrics METRICS =
            new SceneMetrics(new long[] {600, 3_250_000, 9_800_000, 13_107_200, 3_145_728, 14, 2, 3});

    @Test
    void describesFrameTimesPeaksAndTheGeneration() {
        List<String> lines = DiagnosticLines.describe(METRICS, ReloadOutcome.published(2));

        assertEquals(
                Arrays.asList(
                        "MineMod Vulkan",
                        "frame 3.25 ms mean, 9.80 ms worst over 600",
                        "textures 12.5 MiB peak, staging 3.0 MiB peak",
                        "14 pipelines peak, 2 resources retiring, generation 3",
                        "shaders: 2 replaced by resource packs"),
                lines);
    }

    @Test
    void namesTheBuiltInProgramsWhenNoPackReplacesThem() {
        assertEquals("shaders: built-in programs", lastLineFor(ReloadOutcome.notReloaded()));
        assertEquals("shaders: built-in programs", lastLineFor(ReloadOutcome.published(0)));
    }

    @Test
    void namesTheFirstProblemOfARefusedReload() {
        ReloadOutcome refused = ReloadOutcome.rejected("rendertype_solid: 0:3: syntax error\nrendertype_text: missing");

        assertEquals(
                "shaders: reload refused, previous programs kept (rendertype_solid: 0:3: syntax error)",
                lastLineFor(refused));
    }

    private static String lastLineFor(ReloadOutcome reload) {
        List<String> lines = DiagnosticLines.describe(METRICS, reload);
        return lines.get(lines.size() - 1);
    }
}

package io.github.luasgraphicss.minemod.verification.journey.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.journey.evidence.RendererLines.Programs;
import io.github.luasgraphicss.minemod.verification.journey.fixtures.InspectionDraft;
import java.util.List;
import org.junit.jupiter.api.Test;

class RendererLinesTest {
    @Test
    void readsTheLinesTheRendererAddsToTheDebugScreen() {
        List<String> debugLines = InspectionDraft.named("world")
                .atGeneration(4)
                .retiring(2)
                .leavingOut(7)
                .withShaders(InspectionDraft.ONE_REPLACED)
                .build()
                .debugLines();

        RendererLines lines = RendererLines.parse(debugLines).orElseThrow();

        assertEquals(120, lines.framesMeasured());
        assertEquals(4, lines.generation());
        assertEquals(2, lines.retiringResources());
        assertEquals(7, lines.leftOutDraws());
        assertEquals(new RendererLines.Shaders(Programs.REPLACED, 1, ""), lines.shaders());
    }

    @Test
    void tellsBuiltInProgramsFromARefusedReload() {
        RendererLines builtIn = RendererLines.parse(
                        InspectionDraft.named("world").build().debugLines())
                .orElseThrow();
        RendererLines refused = RendererLines.parse(
                        InspectionDraft.named("world").refusing("syntax error").build().debugLines())
                .orElseThrow();

        assertEquals(Programs.BUILT_IN, builtIn.shaders().programs());
        assertEquals(Programs.REFUSED, refused.shaders().programs());
        assertEquals("position_tex_color.fsh:11: syntax error", refused.shaders().refusal());
    }

    @Test
    void theDebugScreenOfTheGameAloneHasNoRendererLines() {
        List<String> debugLines =
                InspectionDraft.named("world").withTheRendererOfTheGame().build().debugLines();

        assertTrue(RendererLines.parse(debugLines).isEmpty());
    }
}

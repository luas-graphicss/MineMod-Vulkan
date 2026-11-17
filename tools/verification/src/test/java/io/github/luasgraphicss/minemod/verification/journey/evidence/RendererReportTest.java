package io.github.luasgraphicss.minemod.verification.journey.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RendererReportTest {
    private static final String SECTION = """
            -- MineMod Vulkan --
            Details:
            \tRenderer: frames: 120
            \t\tframe time: mean 900 us, worst 2100 us
            \t\tpass write textures: 40 us, 2 draw(s)
            \t\tpass world: 410 us, 37 draw(s)
            \t\tpass interface: 95 us, 6 draw(s)
            \t\tresource buffer: peak 2048 byte(s) in 3 object(s)
            \t\tresource texture: peak 13107200 byte(s) in 14 object(s)
            \t\tqueue section uploads: peak 4 of 64""";

    @Test
    void readsTheFramesDrawsAndPeaksOfTheCrashReportSection() {
        RendererReport report = RendererReport.parse(SECTION).orElseThrow();

        assertEquals(120, report.frames());
        assertEquals(37, report.drawsIn(RendererReport.WORLD_PASS));
        assertEquals(6, report.drawsIn(RendererReport.INTERFACE_PASS));
        assertEquals(0, report.drawsIn("hand"));
        assertEquals(13107200, report.peakBytesOf(RendererReport.TEXTURES));
        assertEquals(new RendererReport.Resource(2048, 3), report.resources().get(RendererReport.BUFFERS));
        assertEquals(Optional.empty(), report.refusedReload());
    }

    @Test
    void keepsWhyAReloadWasRefused() {
        String refused = SECTION + "\n\t\treload refused:\n\t\tposition_tex_color.fsh:11: syntax error\n\t\tsecond line";

        RendererReport report = RendererReport.parse(refused).orElseThrow();

        assertEquals(Optional.of("position_tex_color.fsh:11: syntax error\nsecond line"), report.refusedReload());
        assertEquals(37, report.drawsIn(RendererReport.WORLD_PASS));
    }

    @Test
    void aSectionWithoutFramesIsNotAReport() {
        assertTrue(RendererReport.parse("-- MineMod Vulkan --\nDetails:\n\tRenderer: ").isEmpty());
    }
}

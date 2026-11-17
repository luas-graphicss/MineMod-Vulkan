package io.github.luasgraphicss.minemod.adapters.configuration.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class DiagnosticReportTest {
    @Test
    void printsTheSameSectionsOnEveryTarget() {
        DiagnosticReport report = DiagnosticReport.forTarget("1.12.2", "forge 14.23.5.2859")
                .section("settings", Arrays.asList("compatibility.mod-opengl=true", "shaders.pack="))
                .section("renderers", Collections.<String>emptyList())
                .section("recovery", Collections.singletonList("safe mode, the last 2 launch(es) ended before the first frame"))
                .build();

        assertEquals(
                Arrays.asList(
                        "MineMod Vulkan on Minecraft 1.12.2, forge 14.23.5.2859",
                        "settings: compatibility.mod-opengl=true; shaders.pack=",
                        "renderers: none",
                        "recovery: safe mode, the last 2 launch(es) ended before the first frame"),
                report.lines());
    }

    @Test
    void aNameFromAModCannotForgeALineOfTheReport() {
        DiagnosticReport report = DiagnosticReport.forTarget("1.21.1", "fabric\n0.16.5")
                .section("bridges", Collections.singletonList("chunk-meshes\nrenderers: none"))
                .build();

        assertEquals(
                Arrays.asList("MineMod Vulkan on Minecraft 1.21.1, fabric?0.16.5", "bridges: chunk-meshes?renderers: none"),
                report.lines());
    }
}

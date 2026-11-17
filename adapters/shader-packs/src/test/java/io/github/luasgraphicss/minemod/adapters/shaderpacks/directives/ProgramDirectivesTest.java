package io.github.luasgraphicss.minemod.adapters.shaderpacks.directives;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramSources;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.support.PackSketch;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ProgramDirectivesTest {
    @Test
    void readsTheDrawBuffersOfEitherDirective() {
        assertEquals(Arrays.asList(0, 2, 5, 7), fragment("/* DRAWBUFFERS:0257 */").drawBuffers());
        assertEquals(Arrays.asList(0, 1, 12), fragment("/* RENDERTARGETS: 0,1,12 */").drawBuffers());
        assertEquals(
                Arrays.asList(4, 11),
                fragment("/* DRAWBUFFERS:01 */", "/* RENDERTARGETS: 4, 11 */").drawBuffers());
    }

    @Test
    void aProgramWithoutADirectiveWritesTheFirstBuffer() {
        assertEquals(Collections.singletonList(0), fragment("void main() {}").drawBuffers());
    }

    @Test
    void collectsTheUniformsOfBothStagesWithTheirTypes() {
        ProgramDirectives directives = ProgramDirectives.of(new ProgramSources(
                "uniform mat4 gbufferModelView;",
                "uniform sampler2D colortex0, depthtex1;\nuniform highp float near;\nuniform vec3 samples[16];",
                0,
                false));

        assertEquals(
                Arrays.asList("gbufferModelView", "colortex0", "depthtex1", "near", "samples"),
                Arrays.asList(directives.uniformTypes().keySet().toArray()));
        assertEquals("sampler2D", directives.uniformTypes().get("depthtex1"));
        assertEquals("float", directives.uniformTypes().get("near"));
        assertEquals("vec3", directives.uniformTypes().get("samples"));
    }

    @Test
    void aUniformInsideACommentIsNotDeclared() {
        ProgramDirectives directives = fragment(
                "// uniform float retired;", "/* uniform float disabled;", "   uniform float alsoDisabled; */", "uniform float far;");

        assertEquals(Collections.singleton("far"), directives.uniformTypes().keySet());
    }

    @Test
    void readsTheSettingsAPackDeclaresInsideComments() {
        PackSettings settings = PackSettings.of(new PackSketch()
                .fragment(
                        "final",
                        "/*",
                        "const int colortex1Format = RGBA16F;",
                        "const int gaux1Format = R11F_G11F_B10F;",
                        "const bool colortex1Clear = false;",
                        "const int shadowMapResolution = 2048;",
                        "const float sunPathRotation = -40.0;",
                        "*/")
                .build());

        assertEquals("RGBA16F", settings.formatNames().get(1));
        assertEquals("R11F_G11F_B10F", settings.formatNames().get(4));
        assertFalse(settings.clears(1));
        assertTrue(settings.clears(0));
        assertEquals("2048", settings.shadowMapResolution());
    }

    private static ProgramDirectives fragment(String... lines) {
        return ProgramDirectives.of(new ProgramSources("", String.join("\n", lines), 0, false));
    }
}

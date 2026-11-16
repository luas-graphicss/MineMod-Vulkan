package io.github.luasgraphicss.minemod.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NativeShaderServiceTest {
    @TempDir
    Path scratch;

    @Test
    void compilesACoreShaderToSpirvForBothStages() {
        try (NativeShaderService service = CoreShaderSamples.openService(scratch)) {
            ShaderCompilation compilation = service.compile(CoreShaderSamples.tinted("position_tex_color"));

            assertTrue(compilation.succeeded(), compilation.log());
            assertEquals(CoreShaderSamples.SPIRV_MAGIC, compilation.vertexWords()[0]);
            assertEquals(CoreShaderSamples.SPIRV_MAGIC, compilation.fragmentWords()[0]);
        }
    }

    @Test
    void reportsTheProblemsOfAShaderThatDoesNotCompile() {
        try (NativeShaderService service = CoreShaderSamples.openService(scratch)) {
            ShaderCompilation compilation = service.compile(CoreShaderSamples.broken("position_tex_color"));

            assertFalse(compilation.succeeded());
            assertEquals(0, compilation.vertexWords().length);
            assertTrue(compilation.log().contains("undeclared"), compilation.log());
        }
    }

    @Test
    void reportsAnImportNoPackProvides() {
        CoreShaderSource importing = new CoreShaderSource(
                "position_tex_color",
                "#version 150\n#moj_import <absent.glsl>\n" + CoreShaderSamples.TINTED_VERTEX,
                CoreShaderSamples.TINTED_FRAGMENT,
                Collections.<String, String>emptyMap());

        try (NativeShaderService service = CoreShaderSamples.openService(scratch)) {
            ShaderCompilation compilation = service.compile(importing);

            assertFalse(compilation.succeeded());
            assertTrue(compilation.log().contains("absent.glsl"), compilation.log());
        }
    }

    @Test
    void refusesToCompileOnceClosed() {
        NativeShaderService service = CoreShaderSamples.openService(scratch);
        service.close();
        service.close();

        CoreShaderSource shader = CoreShaderSamples.tinted("position_tex_color");

        assertThrows(IllegalStateException.class, () -> service.compile(shader));
    }
}

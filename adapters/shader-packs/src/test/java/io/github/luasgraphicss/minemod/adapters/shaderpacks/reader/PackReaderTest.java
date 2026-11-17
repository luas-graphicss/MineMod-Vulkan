package io.github.luasgraphicss.minemod.adapters.shaderpacks.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramSources;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ShaderPack;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackReaderTest {
    @TempDir
    Path packDirectory;

    private Path shaders;

    @BeforeEach
    void createShadersDirectory() throws IOException {
        shaders = Files.createDirectories(packDirectory.resolve("shaders"));
    }

    @Test
    void readsTheProgramsWithTheirIncludesExpanded() throws IOException {
        write("lib/common.glsl", "uniform float frameTimeCounter;");
        write("local.glsl", "uniform sampler2D colortex0;");
        write("composite.fsh", "#include \"/lib/common.glsl\"", "#include \"local.glsl\"", "/* DRAWBUFFERS:0 */");
        write("composite.vsh", "void main() {}");
        write("composite_a.csh", "void main() {}");
        write("composite_b.csh", "void main() {}");
        write("gbuffers_terrain.gsh", "void main() {}");
        write("shaders.properties", "flip.composite.colortex0=false");
        write("block.properties", "block.1=stone");

        ShaderPack pack = PackReader.read("sample", shaders);

        ProgramSources composite = pack.programs().get("composite");
        assertEquals(Arrays.asList("composite", "gbuffers_terrain"), Arrays.asList(pack.programs().keySet().toArray()));
        assertTrue(composite.fragment().contains("uniform float frameTimeCounter;"));
        assertTrue(composite.fragment().contains("uniform sampler2D colortex0;"));
        assertFalse(composite.fragment().contains("#include"));
        assertEquals(2, composite.computeShaders());
        assertTrue(pack.programs().get("gbuffers_terrain").geometryShader());
        assertEquals(Collections.singletonMap("flip.composite.colortex0", "false"), pack.properties());
    }

    @Test
    void refusesAnIncludeThatLeavesThePack() throws IOException {
        Files.write(packDirectory.resolve("outside.glsl"), "uniform float far;".getBytes(StandardCharsets.UTF_8));
        write("final.fsh", "#include \"../outside.glsl\"");

        IOException refusal = assertThrows(IOException.class, () -> PackReader.read("sample", shaders));

        assertEquals("include \"../outside.glsl\" leaves the shader pack", refusal.getMessage());
    }

    @Test
    void refusesIncludesThatNeverEnd() throws IOException {
        write("loop.glsl", "#include \"loop.glsl\"");
        write("final.fsh", "#include \"loop.glsl\"");

        IOException refusal = assertThrows(IOException.class, () -> PackReader.read("sample", shaders));

        assertEquals("includes nest deeper than 10 at loop.glsl", refusal.getMessage());
    }

    @Test
    void refusesAnIncludeThatIsNotAFile() throws IOException {
        write("final.fsh", "#include \"missing.glsl\"");

        IOException refusal = assertThrows(IOException.class, () -> PackReader.read("sample", shaders));

        assertEquals("missing.glsl is not a file of the shader pack", refusal.getMessage());
    }

    private void write(String relativePath, String... lines) throws IOException {
        Path file = shaders.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }
}

package io.github.luasgraphicss.minemod.client.shaders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ShaderImportsTest {
    @Test
    void findsSharedAndLocalImportsInTheOrderTheyAppear() {
        String source = "#version 150\n"
                + "#moj_import <fog.glsl>\n"
                + "  #  moj_import \"tint.glsl\"  // local\n"
                + "#moj_import <fog.glsl>\n"
                + "void main() {}\n";

        assertEquals(Arrays.asList("<fog.glsl>", "\"tint.glsl\""), ShaderImports.keysIn(source));
    }

    @Test
    void ignoresLinesThatDoNotImport() {
        String source = "// #moj_import <fog.glsl>\n"
                + "#moj_imports <fog.glsl>\n"
                + "#moj_import fog.glsl\n"
                + "#moj_import <>\n"
                + "#moj_import <fog.glsl\n"
                + "#define moj_import <fog.glsl>\n";

        assertEquals(Collections.emptyList(), ShaderImports.keysIn(source));
    }

    @Test
    void findsAnImportOnALastLineWithoutANewline() {
        assertEquals(Collections.singletonList("<light.glsl>"), ShaderImports.keysIn("#moj_import <light.glsl>"));
    }

    @Test
    void readsSharedImportsFromTheIncludeDirectoryAndLocalOnesNextToTheShader() {
        assertEquals(Optional.of("shaders/include/fog.glsl"), ShaderImports.pathOf("<fog.glsl>"));
        assertEquals(Optional.of("shaders/core/parts/tint.glsl"), ShaderImports.pathOf("\"parts/tint.glsl\""));
    }

    @Test
    void refusesImportsThatLeaveTheShaderDirectories() {
        assertEquals(Optional.empty(), ShaderImports.pathOf("<../../pack.mcmeta>"));
        assertEquals(Optional.empty(), ShaderImports.pathOf("</etc/passwd>"));
        assertEquals(Optional.empty(), ShaderImports.pathOf("<other:fog.glsl>"));
        assertEquals(Optional.empty(), ShaderImports.pathOf("<Fog.glsl>"));
        assertEquals(Optional.empty(), ShaderImports.pathOf("<fog.glsl/>"));
        assertEquals(Optional.empty(), ShaderImports.pathOf("<>"));
    }
}

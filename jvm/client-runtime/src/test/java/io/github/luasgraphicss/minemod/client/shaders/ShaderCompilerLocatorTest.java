package io.github.luasgraphicss.minemod.client.shaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShaderCompilerLocatorTest {
    @TempDir
    Path directory;

    @Test
    void usesTheCompilerTheUserNamed() throws IOException {
        Path named = executable(directory.resolve("custom-glslang"));
        Path installed = executable(Files.createDirectory(directory.resolve("bin")).resolve("glslangValidator"));

        assertEquals(
                Optional.of(named),
                ShaderCompilerLocator.locate(named.toString(), installed.getParent().toString()));
    }

    @Test
    void findsTheCompilerOnTheSearchPath() throws IOException {
        Path empty = Files.createDirectory(directory.resolve("empty"));
        Path installed = executable(Files.createDirectory(directory.resolve("bin")).resolve("glslang"));

        String searchPath = empty + File.pathSeparator + File.pathSeparator + installed.getParent();

        assertEquals(Optional.of(installed), ShaderCompilerLocator.locate(null, searchPath));
    }

    @Test
    void findsNoCompilerWhenNoneIsInstalled() {
        assertEquals(Optional.empty(), ShaderCompilerLocator.locate(null, directory.toString()));
        assertEquals(Optional.empty(), ShaderCompilerLocator.locate("", null));
    }

    @Test
    void doesNotFallBackWhenTheNamedCompilerIsMissing() throws IOException {
        Path installed = executable(directory.resolve("glslangValidator"));

        assertEquals(
                Optional.empty(),
                ShaderCompilerLocator.locate(directory.resolve("absent").toString(), installed.getParent().toString()));
    }

    private static Path executable(Path file) throws IOException {
        Files.createFile(file);
        assertTrue(file.toFile().setExecutable(true));
        return file.toAbsolutePath();
    }
}

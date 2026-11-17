package io.github.luasgraphicss.minemod.verification.hooks.mappings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MojangNamesTest {
    private static final String MAPPINGS = """
            # generated
            com.mojang.blaze3d.platform.Window -> dez:
                long window -> g
                1:1:void <init>(com.mojang.blaze3d.platform.WindowEventHandler,java.lang.String) -> <init>
                82:90:void updateVsync(boolean) -> a
                95:96:long[] monitors(int[]) -> a
                100:101:void close() -> close
            com.mojang.blaze3d.platform.WindowEventHandler -> dfa:
            """;

    private MojangNames names;

    @BeforeEach
    void readMappings(@TempDir Path directory) throws IOException {
        names = MojangNames.read(Files.writeString(directory.resolve("client.txt"), MAPPINGS));
    }

    @Test
    void namesAnObfuscatedMethod() {
        assertEquals(
                Optional.of("com/mojang/blaze3d/platform/Window.updateVsync(Z)V"),
                names.namedMethodOf("dez", "a", "(Z)V"));
    }

    @Test
    void tellsOverloadsApartByDescriptor() {
        assertEquals(
                Optional.of("com/mojang/blaze3d/platform/Window.monitors([I)[J"),
                names.namedMethodOf("dez", "a", "([I)[J"));
    }

    @Test
    void namesTheTypesOfAConstructor() {
        assertEquals(
                Optional.of("com/mojang/blaze3d/platform/Window.<init>"
                        + "(Lcom/mojang/blaze3d/platform/WindowEventHandler;Ljava/lang/String;)V"),
                names.namedMethodOf("dez", "<init>", "(Ldfa;Ljava/lang/String;)V"));
    }

    @Test
    void leavesAnUnknownMethodUnnamed() {
        assertEquals(Optional.empty(), names.namedMethodOf("dez", "b", "()V"));
        assertEquals(Optional.empty(), names.namedMethodOf("zzz", "a", "(Z)V"));
    }
}

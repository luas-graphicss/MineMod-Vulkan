package io.github.luasgraphicss.minemod.verification.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CompareProviderOptionsTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @Test
    void rejectsAProviderWithoutItsJniLibrary() {
        int exitCode = run("compare", "--suite", "gl-differential", "--provider", "/usr/lib64");

        assertEquals(VerificationCli.EXIT_USAGE, exitCode);
        assertTrue(reported().startsWith("--provider and --natives name a provider together"), reported());
    }

    @Test
    void rejectsAJniLibraryWithoutItsProvider() {
        int exitCode = run("compare", "--suite", "gl-differential", "--natives", "libminemod_gl_provider_jni.so");

        assertEquals(VerificationCli.EXIT_USAGE, exitCode);
        assertTrue(reported().startsWith("--provider and --natives name a provider together"), reported());
    }

    @Test
    void failsWhenTheJniLibraryOfTheProviderIsNoFile() {
        int exitCode = run(
                "compare", "--suite", "gl-differential", "--provider", "no-provider", "--natives", "no-library.so");

        assertEquals(VerificationCli.EXIT_FAILURE, exitCode);
        assertTrue(reported().contains("the JNI library of the provider is not a file"), reported());
    }

    private int run(String... arguments) {
        return VerificationCli.run(
                arguments,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8));
    }

    private String reported() {
        return errors.toString(StandardCharsets.UTF_8);
    }
}

package io.github.luasgraphicss.minemod.verification.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VerificationCliTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @Test
    void foundationProfilePassesOnTheRepository() {
        int exitCode = run("check", "--profile", "foundation", "--root", System.getProperty("minemod.repositoryRoot"));

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).contains("check target-matrix: ok"), text(output));
    }

    @Test
    void reportsAnUnreadableRepositoryAsAFailure() {
        int exitCode = run("check", "--profile", "foundation", "--root", "/nonexistent-minemod-repository");

        assertEquals(VerificationCli.EXIT_FAILURE, exitCode);
        assertTrue(text(errors).startsWith("verification failed: "), text(errors));
    }

    @Test
    void rejectsAnUnknownProfile() {
        assertEquals(VerificationCli.EXIT_USAGE, run("check", "--profile", "imaginary"));
        assertTrue(text(errors).startsWith("unknown profile: imaginary"), text(errors));
    }

    @Test
    void rejectsAnUnknownCommand() {
        assertEquals(VerificationCli.EXIT_USAGE, run("render"));
    }

    @Test
    void rejectsAnOptionGivenTwice() {
        assertEquals(VerificationCli.EXIT_USAGE, run("check", "--profile", "foundation", "--profile", "foundation"));
    }

    @Test
    void rejectsAnOptionTheCommandDoesNotKnow() {
        assertEquals(VerificationCli.EXIT_USAGE, run("check", "--profile", "foundation", "--verbose", "yes"));
    }

    private int run(String... arguments) {
        return VerificationCli.run(
                arguments,
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(errors, true, StandardCharsets.UTF_8));
    }

    private static String text(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8);
    }
}

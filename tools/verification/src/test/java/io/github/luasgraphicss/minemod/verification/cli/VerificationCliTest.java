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
    void adaptersProfilePassesOnTheRepository() {
        int exitCode = run("check", "--profile", "adapters", "--root", System.getProperty("minemod.repositoryRoot"));

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).contains("check adapter-modules: ok"), text(output));
    }

    @Test
    void theBuildMatrixHasTheAdaptersOfEveryCombination() {
        int exitCode = run(
                "build-matrix",
                "--manifest",
                "manifests/minecraft-targets.json",
                "--root",
                System.getProperty("minemod.repositoryRoot"));

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).contains("build matrix: 73 of 73 combinations have their adapters"), text(output));
    }

    @Test
    void compatibilityProfilePassesOnTheRepository() {
        int exitCode = run("check", "--profile", "compatibility");

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).contains("profile compatibility: 3 passed, 0 failed"), text(output));
    }

    @Test
    void runsAScenarioByName() {
        int exitCode = run("scenario", "legacy-gl-contract");

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).endsWith("scenario legacy-gl-contract: passed" + System.lineSeparator()), text(output));
    }

    @Test
    void rejectsAnUnknownScenario() {
        assertEquals(VerificationCli.EXIT_USAGE, run("scenario", "imaginary"));
        assertTrue(text(errors).startsWith("unknown scenario: imaginary"), text(errors));
    }

    @Test
    void comparesTheDifferentialSuiteAndNamesItsProvider() {
        int exitCode = run("compare", "--suite", "gl-differential");

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).contains("against the simulated provider, 0 difference(s)"), text(output));
    }

    @Test
    void printsTheCapabilitiesOfOneContextAndWhatAFrameReports() {
        int exitCode = run("capabilities", "--context", "core");

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).contains("core context:"), text(output));
        assertTrue(!text(output).contains("compatibility context:"), text(output));
        assertTrue(text(output).contains("waiting for a bridge: query result across native draws"), text(output));
    }

    @Test
    void rejectsAnUnknownContext() {
        assertEquals(VerificationCli.EXIT_USAGE, run("capabilities", "--context", "imaginary"));
        assertTrue(text(errors).startsWith("unknown context: imaginary"), text(errors));
    }

    @Test
    void rejectsAnUnknownSuite() {
        assertEquals(VerificationCli.EXIT_USAGE, run("compare", "--suite", "imaginary"));
        assertTrue(text(errors).startsWith("unknown suite: imaginary"), text(errors));
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

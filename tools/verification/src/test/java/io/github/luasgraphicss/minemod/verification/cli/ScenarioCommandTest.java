package io.github.luasgraphicss.minemod.verification.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScenarioCommandTest {
    private static final String REPORT = "reports/legacy-gl-contract.txt";

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @TempDir
    Path repositoryRoot;

    @Test
    void writesWhatAPassingScenarioObservedWhereItIsTold() throws IOException {
        int exitCode = run("scenario", "legacy-gl-contract", "--report", REPORT, "--root", repositoryRoot.toString());

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        String report = Files.readString(repositoryRoot.resolve(REPORT));
        assertFalse(report.isBlank());
        assertTrue(report.endsWith("\n"), report);
        for (String observation : report.strip().split("\n")) {
            assertTrue(text(output).contains("  " + observation), observation);
        }
        assertTrue(text(output).contains("wrote " + repositoryRoot.resolve(REPORT)), text(output));
    }

    @Test
    void writesNoReportUnlessAsked() throws IOException {
        int exitCode = run("scenario", "legacy-gl-contract", "--root", repositoryRoot.toString());

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        try (var files = Files.list(repositoryRoot)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsACacheForAScenarioThatDownloadsNothing() {
        assertEquals(VerificationCli.EXIT_USAGE, run("scenario", "legacy-gl-contract", "--cache", "anywhere"));
        assertTrue(text(errors).startsWith("unknown option: --cache"), text(errors));
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

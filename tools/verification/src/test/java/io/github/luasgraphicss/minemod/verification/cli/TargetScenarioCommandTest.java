package io.github.luasgraphicss.minemod.verification.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.Scenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioCatalog;
import io.github.luasgraphicss.minemod.verification.scenario.TargetScenario;
import io.github.luasgraphicss.minemod.verification.scenario.TargetScenarioCatalog;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TargetScenarioCommandTest {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @TempDir
    Path emptyRepository;

    @Test
    void theScenariosOfTheVanillaGameAreRegisteredUnderNamesNoOtherScenarioHas() {
        List<String> targeted =
                TargetScenarioCatalog.all().stream().map(TargetScenario::name).toList();
        List<String> others = Stream.concat(
                        ScenarioCatalog.all().stream().map(Scenario::name),
                        RepositoryScenarioCatalog.all().stream().map(RepositoryScenario::name))
                .toList();

        assertEquals(List.of("vanilla-journey", "vanilla-without-gl", "resource-reload"), targeted);
        assertTrue(targeted.stream().noneMatch(others::contains), others.toString());
        assertTrue(TargetScenarioCatalog.named("resource-reload").isPresent());
        assertTrue(TargetScenarioCatalog.named("vanilla-matrix").isEmpty());
    }

    @Test
    void aScenarioOfTheGameNeedsItsTarget() {
        assertEquals(VerificationCli.EXIT_USAGE, run("scenario", "vanilla-journey"));
        assertTrue(text(errors).startsWith("missing required option: --target"), text(errors));
    }

    @Test
    void aTargetNamesALoaderAndARelease() {
        assertEquals(VerificationCli.EXIT_USAGE, run("scenario", "vanilla-without-gl", "--target", "../1.21.1"));
        assertTrue(
                text(errors).startsWith("a target names a loader and a release, as in fabric-1.21.1, found: ../1.21.1"),
                text(errors));
    }

    @Test
    void otherScenariosTakeNoTarget() {
        assertEquals(VerificationCli.EXIT_USAGE, run("scenario", "legacy-gl-contract", "--target", "fabric-1.21.1"));
        assertTrue(text(errors).startsWith("unknown option: --target"), text(errors));
    }

    @Test
    void aTargetOutsideTheManifestFailsTheScenario() {
        int exitCode = run(
                "scenario",
                "resource-reload",
                "--target",
                "fabric-9.9.9",
                "--root",
                System.getProperty("minemod.repositoryRoot"),
                "--cache",
                emptyRepository.toString());

        assertEquals(VerificationCli.EXIT_VIOLATIONS, exitCode, text(errors));
        assertTrue(text(output).contains("  - fabric-9.9.9 is not a target of the manifest"), text(output));
        assertTrue(text(output).contains("scenario resource-reload: 1 failure(s)"), text(output));
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

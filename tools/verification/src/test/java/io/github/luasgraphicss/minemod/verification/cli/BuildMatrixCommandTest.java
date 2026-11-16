package io.github.luasgraphicss.minemod.verification.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.matrix.plan.AdapterBuildPlanner;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildMatrixCommandTest {
    private static final String MANIFEST = "manifests/minecraft-targets.json";
    private static final String OLDEST_BUILD = "1.12.2 FORGE: minemod-vulkan-forge-1.12.2 (Java 8, FORGEGRADLE)";

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errors = new ByteArrayOutputStream();

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void copyTheCommittedManifest() throws IOException {
        Path copy = repositoryRoot.resolve(MANIFEST);
        Files.createDirectories(copy.getParent());
        Files.copy(Path.of(System.getProperty("minemod.repositoryRoot")).resolve(MANIFEST), copy);
    }

    @Test
    void reportsEveryCombinationWithoutItsAdapters() throws IOException {
        int exitCode = buildMatrix();

        assertEquals(VerificationCli.EXIT_VIOLATIONS, exitCode, text(output));
        assertTrue(
                text(output).contains(OLDEST_BUILD + " missing adapters/minecraft/mc-1.12.2, adapters/forge/mc-1.12.2"),
                text(output));
        assertTrue(
                text(output).contains("build matrix: 0 of " + plannedBuilds().size() + " combinations"),
                text(output));
    }

    @Test
    void passesWhenEveryCombinationHasItsAdapters() throws IOException {
        List<AdapterBuild> builds = plannedBuilds();
        for (AdapterBuild build : builds) {
            for (String module : build.modules()) {
                Path directory = Files.createDirectories(repositoryRoot.resolve(module));
                Files.writeString(directory.resolve("build.gradle.kts"), "");
            }
        }

        int exitCode = buildMatrix();

        assertEquals(VerificationCli.EXIT_OK, exitCode, text(output));
        assertTrue(text(output).contains(OLDEST_BUILD + " ready"), text(output));
        assertTrue(
                text(output).contains("build matrix: " + builds.size() + " of " + builds.size()),
                text(output));
    }

    @Test
    void plansNothingFromAManifestThatBreaksTheTargetPolicy() throws IOException {
        TargetManifestFile file = committedCopy();
        TargetManifest manifest = file.read();
        List<ReleaseTarget> withoutTheOldestRelease = manifest.releases().subList(1, manifest.releases().size());
        file.write(new TargetManifest(manifest.schemaVersion(), manifest.sources(), withoutTheOldestRelease));

        int exitCode = buildMatrix();

        assertEquals(VerificationCli.EXIT_VIOLATIONS, exitCode, text(output));
        assertTrue(text(output).contains("breaks the target policy; no build was planned"), text(output));
        assertFalse(text(output).contains("build matrix:"), text(output));
    }

    @Test
    void requiresTheManifestToPlanFrom() {
        assertEquals(VerificationCli.EXIT_USAGE, run("build-matrix", "--root", repositoryRoot.toString()));
        assertTrue(text(errors).startsWith("missing required option: --manifest"), text(errors));
    }

    private int buildMatrix() {
        return run("build-matrix", "--manifest", MANIFEST, "--root", repositoryRoot.toString());
    }

    private List<AdapterBuild> plannedBuilds() throws IOException {
        return AdapterBuildPlanner.plan(committedCopy().read());
    }

    private TargetManifestFile committedCopy() {
        return TargetManifestFile.at(repositoryRoot.resolve(MANIFEST));
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

package io.github.luasgraphicss.minemod.verification.targets.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget.JavaRuntime;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class TargetMatrixRulesTest {
    @Test
    void committedManifestSatisfiesThePolicy() throws IOException {
        assertEquals(List.of(), TargetMatrixRules.findViolations(committedManifest()));
    }

    @Test
    void committedManifestCoversThirtySevenReleases() throws IOException {
        assertEquals(37, committedManifest().releases().size());
    }

    @Test
    void rejectsAMissingRelease() throws IOException {
        TargetManifest manifest = committedManifest();
        List<ReleaseTarget> releases = new ArrayList<>(manifest.releases());
        releases.removeIf(release -> release.id().equals("1.13.1"));

        List<String> violations = TargetMatrixRules.findViolations(
                new TargetManifest(manifest.schemaVersion(), manifest.sources(), releases));

        assertTrue(violations.get(0).startsWith("releases are "), violations.toString());
    }

    @Test
    void rejectsARuntimeFromAnotherGeneration() throws IOException {
        TargetManifest manifest = changeRelease("1.17.1", release -> withRuntime(release, 17));

        assertEquals(
                List.of("release 1.17.1: runtime is Java 17, expected Java 16"),
                TargetMatrixRules.findViolations(manifest));
    }

    @Test
    void rejectsAnExistingLoaderOnAReleaseCoveredByTheClientBootstrap() throws IOException {
        LoaderTarget fabric = loaderOf("1.14", LoaderTarget.Kind.FABRIC);
        TargetManifest manifest = changeRelease("1.13", release -> {
            List<LoaderTarget> loaders = new ArrayList<>(release.loaders());
            loaders.add(fabric);
            return withLoaders(release, loaders);
        });

        List<String> violations = TargetMatrixRules.findViolations(manifest);

        assertTrue(
                violations.contains("release 1.13: client bootstrap must not be combined with an existing loader: "
                        + "[FABRIC, CLIENT_BOOTSTRAP]"),
                violations.toString());
    }

    @Test
    void rejectsAReleaseWithoutItsBaseLoader() throws IOException {
        TargetManifest manifest = changeRelease("1.21.1", release -> withLoaders(
                release,
                release.loaders().stream()
                        .filter(loader -> loader.loader() != LoaderTarget.Kind.FABRIC)
                        .toList()));

        assertEquals(
                List.of("release 1.21.1: base loader FABRIC is missing"), TargetMatrixRules.findViolations(manifest));
    }

    @Test
    void rejectsALoaderWithoutAnExactArtifact() throws IOException {
        TargetManifest manifest = changeRelease("1.12.2", release -> withLoaders(
                release,
                release.loaders().stream()
                        .map(loader -> new LoaderTarget(
                                loader.loader(),
                                "net.minecraftforge:forge",
                                loader.channel(),
                                loader.bootstrap(),
                                loader.mappings(),
                                loader.toolchain()))
                        .toList()));

        assertEquals(
                List.of("release 1.12.2: loader FORGE has no exact artifact coordinate: net.minecraftforge:forge"),
                TargetMatrixRules.findViolations(manifest));
    }

    private static TargetManifest committedManifest() throws IOException {
        return TargetManifestFile.inRepository(Path.of(System.getProperty("minemod.repositoryRoot"))).read();
    }

    private static LoaderTarget loaderOf(String releaseId, LoaderTarget.Kind kind) throws IOException {
        return committedManifest().releases().stream()
                .filter(release -> release.id().equals(releaseId))
                .flatMap(release -> release.loaders().stream())
                .filter(loader -> loader.loader() == kind)
                .findFirst()
                .orElseThrow();
    }

    private static TargetManifest changeRelease(String releaseId, UnaryOperator<ReleaseTarget> change)
            throws IOException {
        TargetManifest manifest = committedManifest();
        List<ReleaseTarget> releases = manifest.releases().stream()
                .map(release -> release.id().equals(releaseId) ? change.apply(release) : release)
                .toList();
        return new TargetManifest(manifest.schemaVersion(), manifest.sources(), releases);
    }

    private static ReleaseTarget withRuntime(ReleaseTarget release, int javaMajor) {
        return new ReleaseTarget(
                release.id(),
                release.family(),
                release.releaseTime(),
                release.metadata(),
                release.client(),
                release.clientMappings(),
                release.launch(),
                new JavaRuntime(release.runtime().component(), javaMajor),
                release.lwjgl(),
                release.loaders());
    }

    private static ReleaseTarget withLoaders(ReleaseTarget release, List<LoaderTarget> loaders) {
        return new ReleaseTarget(
                release.id(),
                release.family(),
                release.releaseTime(),
                release.metadata(),
                release.client(),
                release.clientMappings(),
                release.launch(),
                release.runtime(),
                release.lwjgl(),
                loaders);
    }
}

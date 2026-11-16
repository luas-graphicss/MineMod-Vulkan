package io.github.luasgraphicss.minemod.verification.matrix.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AdapterBuildPlannerTest {
    @Test
    void plansOneBuildForEveryPinnedLoaderCombination() throws IOException {
        TargetManifest manifest = committedManifest();
        long combinations = manifest.releases().stream()
                .mapToLong(release -> release.loaders().size())
                .sum();

        assertEquals(combinations, AdapterBuildPlanner.plan(manifest).size());
    }

    @Test
    void sharesTheGameAdapterBetweenTheLoadersOfARelease() throws IOException {
        List<AdapterBuild> builds = buildsOf("1.21.1");

        assertEquals(
                Set.of("adapters/minecraft/mc-1.21.1"),
                builds.stream().map(AdapterBuild::gameAdapter).collect(Collectors.toSet()));
        assertEquals(
                Set.of("adapters/forge/mc-1.21.1", "adapters/fabric/mc-1.21.1", "adapters/neoforge/mc-1.21.1"),
                builds.stream().map(AdapterBuild::loaderAdapter).collect(Collectors.toSet()));
    }

    @Test
    void namesTheArtifactAfterTheLoaderAndTheRelease() throws IOException {
        assertEquals(
                Set.of("minemod-vulkan-forge-1.17.1", "minemod-vulkan-fabric-1.17.1"),
                buildsOf("1.17.1").stream().map(AdapterBuild::artifactName).collect(Collectors.toSet()));
    }

    @Test
    void givesTheClientBootstrapItsOwnAdapterFamily() throws IOException {
        AdapterBuild build = buildsOf("1.13").get(0);

        assertEquals("adapters/bootstrap/mc-1.13", build.loaderAdapter());
        assertEquals("minemod-vulkan-bootstrap-1.13", build.artifactName());
    }

    @Test
    void buildsOnTheRuntimeOfTheRelease() throws IOException {
        assertEquals(8, buildsOf("1.12.2").get(0).javaRuntime());
        assertEquals(16, buildsOf("1.17.1").get(0).javaRuntime());
        assertEquals(21, buildsOf("1.21.1").get(0).javaRuntime());
    }

    private static List<AdapterBuild> buildsOf(String release) throws IOException {
        return AdapterBuildPlanner.plan(committedManifest()).stream()
                .filter(build -> build.release().equals(release))
                .toList();
    }

    private static TargetManifest committedManifest() throws IOException {
        return TargetManifestFile.inRepository(Path.of(System.getProperty("minemod.repositoryRoot")))
                .read();
    }
}

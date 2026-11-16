package io.github.luasgraphicss.minemod.verification.matrix.modules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdapterModulesTest {
    private static final String GAME_ADAPTER = "adapters/minecraft/mc-1.21.1";
    private static final String LOADER_ADAPTER = "adapters/fabric/mc-1.21.1";
    private static final AdapterBuild BUILD = new AdapterBuild(
            "1.21.1",
            LoaderTarget.Kind.FABRIC,
            "minemod-vulkan-fabric-1.21.1",
            21,
            LoaderTarget.Toolchain.FABRIC_LOOM,
            GAME_ADAPTER,
            LOADER_ADAPTER);

    @TempDir
    Path repositoryRoot;

    @Test
    void reportsBothAdaptersOfARepositoryWithoutThem() {
        assertEquals(List.of(GAME_ADAPTER, LOADER_ADAPTER), AdapterModules.findMissing(repositoryRoot, BUILD));
    }

    @Test
    void reportsADirectoryWithoutABuildFile() throws IOException {
        createModule(GAME_ADAPTER);
        Files.createDirectories(repositoryRoot.resolve(LOADER_ADAPTER));

        assertEquals(List.of(LOADER_ADAPTER), AdapterModules.findMissing(repositoryRoot, BUILD));
    }

    @Test
    void findsNothingMissingWhenBothAdaptersHaveABuildFile() throws IOException {
        createModule(GAME_ADAPTER);
        createModule(LOADER_ADAPTER);

        assertEquals(List.of(), AdapterModules.findMissing(repositoryRoot, BUILD));
    }

    private void createModule(String module) throws IOException {
        Path directory = Files.createDirectories(repositoryRoot.resolve(module));
        Files.createFile(directory.resolve("build.gradle.kts"));
    }
}

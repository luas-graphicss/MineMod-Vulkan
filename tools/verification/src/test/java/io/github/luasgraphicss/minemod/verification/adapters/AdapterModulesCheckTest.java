package io.github.luasgraphicss.minemod.verification.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.adapters.layout.ModuleFiles;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.refresh.AdapterModulesRefresher;
import io.github.luasgraphicss.minemod.verification.adapters.refresh.AdapterModulesRefresher.Written;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.fixtures.CommittedManifests;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AdapterModulesCheckTest {
    private static final String RELEASE_WITHOUT_A_LOADER = "1.13";
    private static final String GAME_ADAPTER = "adapters/minecraft/mc-1.13";
    private static final String LOADER_ADAPTER = "adapters/bootstrap/mc-1.13";

    private final AdapterModulesCheck check = new AdapterModulesCheck();

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void pinOneReleaseThatNeedsNoMappingFile() throws IOException {
        TargetManifest committed = CommittedManifests.targets();
        TargetManifest targets = new TargetManifest(
                committed.schemaVersion(),
                committed.sources(),
                committed.releases().stream()
                        .filter(release -> release.id().equals(RELEASE_WITHOUT_A_LOADER))
                        .toList());
        PresentationHooks hooks = new PresentationHooks(
                PresentationHooks.SCHEMA_VERSION,
                List.of(CommittedManifests.hooks().of(RELEASE_WITHOUT_A_LOADER).orElseThrow()));
        TargetManifestFile.inRepository(repositoryRoot).write(targets);
        PresentationHooksFile.inRepository(repositoryRoot).write(hooks);
    }

    @Test
    void theCommittedAdaptersCarryThePlansOfTheirReleases() throws IOException {
        Path committedRepository = Path.of(System.getProperty("minemod.repositoryRoot"));

        assertEquals(List.of(), check.findViolations(committedRepository));
    }

    @Test
    void reportsEveryAdapterOfARepositoryWithoutThem() throws IOException {
        assertEquals(
                List.of(
                        GAME_ADAPTER + " has no build file of a pinned-call adapter; rewrite it with \"adapters refresh\"",
                        LOADER_ADAPTER
                                + " has no build file of a pinned-call adapter; rewrite it with \"adapters refresh\""),
                check.findViolations(repositoryRoot));
    }

    @Test
    void acceptsTheAdaptersTheRefreshWrites() throws IOException, InterruptedException {
        Written written = refresh();

        assertEquals(new Written(1, 1), written);
        assertEquals(List.of(), check.findViolations(repositoryRoot));
    }

    @Test
    void reportsAPlanThatIsNoLongerThePinnedOne() throws IOException, InterruptedException {
        refresh();
        Path plan = ModuleFiles.planOf(repositoryRoot, LOADER_ADAPTER);
        Files.writeString(plan, Files.readString(plan).replace(".matches=1", ".matches=2"));

        List<String> violations = check.findViolations(repositoryRoot);

        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.getFirst().startsWith(LOADER_ADAPTER + " does not carry the plan"), violations.toString());
    }

    @Test
    void leavesAnAdapterWithABuildOfItsOwnAlone() throws IOException, InterruptedException {
        refresh();
        String ownBuild = "plugins {\n    id(\"fabric-loom\")\n}\n";
        Files.writeString(ModuleFiles.buildFileOf(repositoryRoot, LOADER_ADAPTER), ownBuild);
        Files.delete(ModuleFiles.planOf(repositoryRoot, LOADER_ADAPTER));

        assertEquals(List.of(), check.findViolations(repositoryRoot));
        assertEquals(new Written(1, 0), refresh());
        assertEquals(ownBuild, Files.readString(ModuleFiles.buildFileOf(repositoryRoot, LOADER_ADAPTER)));
    }

    private Written refresh() throws IOException, InterruptedException {
        try (NameDownloads names = new NameDownloads(repositoryRoot.resolve("cache"))) {
            return AdapterModulesRefresher.refresh(
                    repositoryRoot,
                    TargetManifestFile.inRepository(repositoryRoot).read(),
                    PresentationHooksFile.inRepository(repositoryRoot).read(),
                    new AdapterPlanResolver(names));
        }
    }
}

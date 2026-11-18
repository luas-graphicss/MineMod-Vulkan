package io.github.luasgraphicss.minemod.verification.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.refresh.AdapterModulesRefresher;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameDownloads;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile;
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

class FramePlansCheckTest {
    private static final String RELEASE_WITHOUT_A_MIXIN_LOADER = "1.13";
    private static final String GAME_ADAPTER = "adapters/minecraft/mc-1.13";
    private static final String LOADER_ADAPTER = "adapters/bootstrap/mc-1.13";
    private static final String NO_FRAME_PLAN = " has no frame plan; write it with \"frames refresh\"";
    private static final Path COMMITTED_REPOSITORY = Path.of(System.getProperty("minemod.repositoryRoot"));

    private final FramePlansCheck check = new FramePlansCheck();

    @TempDir
    Path repositoryRoot;

    @BeforeEach
    void pinOneReleaseThatRunsWithoutAMixinLoader() throws IOException, InterruptedException {
        TargetManifest committed = CommittedManifests.targets();
        TargetManifest targets = new TargetManifest(
                committed.schemaVersion(),
                committed.sources(),
                committed.releases().stream()
                        .filter(release -> release.id().equals(RELEASE_WITHOUT_A_MIXIN_LOADER))
                        .toList());
        PresentationHooks hooks = new PresentationHooks(
                PresentationHooks.SCHEMA_VERSION,
                List.of(CommittedManifests.hooks().of(RELEASE_WITHOUT_A_MIXIN_LOADER).orElseThrow()));
        TargetManifestFile.inRepository(repositoryRoot).write(targets);
        PresentationHooksFile.inRepository(repositoryRoot).write(hooks);
        try (NameDownloads names = new NameDownloads(repositoryRoot.resolve("cache"))) {
            AdapterModulesRefresher.refresh(repositoryRoot, targets, hooks, new AdapterPlanResolver(names));
        }
    }

    @Test
    void theCommittedAdaptersCarryTheFramePlansOfTheirReleases() throws IOException {
        assertEquals(List.of(), check.findViolations(COMMITTED_REPOSITORY));
    }

    @Test
    void reportsTheAdaptersThatCarryNoFramePlan() throws IOException {
        assertEquals(
                List.of(GAME_ADAPTER + NO_FRAME_PLAN, LOADER_ADAPTER + NO_FRAME_PLAN),
                check.findViolations(repositoryRoot));
    }

    @Test
    void acceptsTheFramePlansCommittedForTheRelease() throws IOException {
        copyCommittedPlanOf(GAME_ADAPTER);
        copyCommittedPlanOf(LOADER_ADAPTER);

        assertEquals(List.of(), check.findViolations(repositoryRoot));
    }

    @Test
    void reportsALoaderThatHandsACallToAnotherStandIn() throws IOException {
        copyCommittedPlanOf(GAME_ADAPTER);
        Path loaderPlan = copyCommittedPlanOf(LOADER_ADAPTER);
        Files.writeString(loaderPlan, Files.readString(loaderPlan).replaceFirst("calls/MatrixCalls", "calls/FogCalls"));

        List<String> violations = check.findViolations(repositoryRoot);

        assertEquals(2, violations.size(), violations.toString());
        assertTrue(violations.get(0).startsWith(LOADER_ADAPTER + " does not hand over the calls"), violations.toString());
        assertTrue(violations.get(1).endsWith("calls/FogCalls, which does not answer it"), violations.toString());
    }

    @Test
    void reportsAFramePlanPinnedOnAnotherClient() throws IOException {
        copyCommittedPlanOf(LOADER_ADAPTER);
        Path gamePlan = copyCommittedPlanOf(GAME_ADAPTER);
        Files.writeString(
                gamePlan,
                Files.readString(gamePlan).replaceFirst("client\\.sha1=[0-9a-f]{40}", "client.sha1=" + "0".repeat(40)));

        List<String> violations = check.findViolations(repositoryRoot);

        assertEquals(1, violations.size(), violations.toString());
        assertTrue(violations.getFirst().startsWith(GAME_ADAPTER + " pins its frame plan on client 0000"), violations.toString());
    }

    private Path copyCommittedPlanOf(String module) throws IOException {
        Path plan = FramePlanFile.of(repositoryRoot, module);
        Files.createDirectories(plan.getParent());
        return Files.copy(FramePlanFile.of(COMMITTED_REPOSITORY, module), plan);
    }
}

package io.github.luasgraphicss.minemod.verification.scenario.transformers;

import io.github.luasgraphicss.minemod.adapters.transformers.release.PinnedReleases;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.plan.ReleasePlans;
import io.github.luasgraphicss.minemod.verification.hooks.policy.PresentationHookRules;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class ProductionTransformersScenario implements RepositoryScenario {
    private static final int SHA1_PREFIX = 12;

    @Override
    public String name() {
        return "production-transformers";
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment) throws IOException, InterruptedException {
        ScenarioLog log = new ScenarioLog();
        TargetManifest targets = TargetManifestFile.inRepository(environment.repositoryRoot()).read();
        PresentationHooks hooks = PresentationHooksFile.inRepository(environment.repositoryRoot()).read();
        List<String> violations = PresentationHookRules.findViolations(hooks, targets);
        if (!violations.isEmpty()) {
            violations.forEach(log::fail);
            return log.report();
        }

        PinnedReleases pinned = ReleasePlans.pinnedBy(hooks);
        try (PinnedDownloads downloads = new PinnedDownloads(environment.cacheDirectory())) {
            for (ReleaseTarget release : targets.releases()) {
                verify(release, hooks.of(release.id()).orElseThrow(), pinned, downloads.clientOf(release), log);
            }
            refuseOtherClients(targets, hooks, pinned, downloads, log);
        }
        return log.report();
    }

    private static void verify(
            ReleaseTarget release, ReleaseHooks hooks, PinnedReleases pinned, Path clientJar, ScenarioLog log)
            throws IOException {
        String subject = "release " + release.id() + ": ";
        String clientSha1 = PinnedDownloads.sha1Of(clientJar);
        Optional<ReleasePlan> plan = pinned.planFor(clientSha1);
        if (plan.isEmpty() || !plan.get().release().equals(release.id())) {
            log.fail(subject + "client " + clientSha1 + " does not select the plan of its release");
            return;
        }
        try (ClientArchive client = ClientArchive.open(clientJar)) {
            List<String> failures = ClientVerification.findFailures(hooks, client);
            failures.forEach(failure -> log.fail(subject + failure));
            if (failures.isEmpty()) {
                log.observe(release.id() + ": " + hooks.points().size() + " points redirect " + hooks.callSites()
                        + " calls of client " + clientSha1.substring(0, SHA1_PREFIX));
            }
        }
    }

    private static void refuseOtherClients(
            TargetManifest targets, PresentationHooks hooks, PinnedReleases pinned, PinnedDownloads downloads, ScenarioLog log)
            throws IOException, InterruptedException {
        ReleaseTarget oldest = targets.releases().getFirst();
        ReleaseTarget newest = targets.releases().getLast();
        try (ClientArchive newestClient = ClientArchive.open(downloads.clientOf(newest))) {
            List<String> failures =
                    ClientVerification.findFailures(hooks.of(oldest.id()).orElseThrow(), newestClient);
            log.expect(
                    !failures.isEmpty(),
                    "the plan of " + oldest.id() + " to be refused on the client of " + newest.id());
            log.observe("the plan of " + oldest.id() + " is refused on the client of " + newest.id() + " with "
                    + failures.size() + " violation(s)");
        }

        Optional<Path> otherArtifact = downloads.clientMappingsOf(newest);
        if (otherArtifact.isPresent()) {
            String sha1 = PinnedDownloads.sha1Of(otherArtifact.get());
            log.expect(pinned.planFor(sha1).isEmpty(), "a file that is not a pinned client to select no plan");
            log.observe("a file with SHA-1 " + sha1.substring(0, SHA1_PREFIX) + " selects no plan");
        }
    }
}

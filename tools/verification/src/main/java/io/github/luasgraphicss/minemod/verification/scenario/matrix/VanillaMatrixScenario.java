package io.github.luasgraphicss.minemod.verification.scenario.matrix;

import io.github.luasgraphicss.minemod.verification.adapters.AdapterModulesCheck;
import io.github.luasgraphicss.minemod.verification.adapters.layout.ModuleFiles;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameDownloads;
import io.github.luasgraphicss.minemod.verification.adapters.storage.AdapterPlanFile;
import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.matrix.plan.AdapterBuildPlanner;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class VanillaMatrixScenario implements RepositoryScenario {
    @Override
    public String name() {
        return "vanilla-matrix";
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment) throws IOException, InterruptedException {
        ScenarioLog log = new ScenarioLog();
        Path root = environment.repositoryRoot();
        List<String> violations = new AdapterModulesCheck().findViolations(root);
        if (!violations.isEmpty()) {
            violations.forEach(log::fail);
            return log.report();
        }
        TargetManifest targets = TargetManifestFile.inRepository(root).read();
        PresentationHooks hooks = PresentationHooksFile.inRepository(root).read();
        Tally tally = new Tally();
        Iterator<AdapterBuild> builds = AdapterBuildPlanner.plan(targets).iterator();
        try (PinnedDownloads clients = new PinnedDownloads(environment.cacheDirectory());
                NameDownloads names = new NameDownloads(environment.cacheDirectory())) {
            AdapterPlanResolver resolver = new AdapterPlanResolver(names);
            for (ReleaseTarget release : targets.releases()) {
                ReleaseHooks releaseHooks = hooks.of(release.id()).orElseThrow();
                try (ClientArchive client = ClientArchive.open(clients.clientOf(release))) {
                    for (LoaderTarget loader : release.loaders()) {
                        verify(root, builds.next(), releaseHooks, loader, client, resolver, tally, log);
                    }
                }
            }
        }
        log.observe(tally.summary());
        log.observe("the clients of forge and neoforge are the vanilla client under the names those loaders run;"
                + " the binary patches of the loaders are not applied here");
        return log.report();
    }

    private static void verify(
            Path root,
            AdapterBuild build,
            ReleaseHooks hooks,
            LoaderTarget loader,
            ClientArchive client,
            AdapterPlanResolver resolver,
            Tally tally,
            ScenarioLog log)
            throws IOException, InterruptedException {
        String subject = build.release() + " " + loader.loader() + ": ";
        if (ModuleFiles.hasItsOwnBuild(root, build.loaderAdapter())) {
            tally.ownBuilds++;
            log.observe(subject + build.loaderAdapter() + " has a build of its own, outside the pinned calls");
            return;
        }
        AdapterPlan committed = AdapterPlanFile.read(ModuleFiles.planOf(root, build.loaderAdapter()));
        AdapterPlan expected = resolver.resolveAsPinned(hooks, loader, committed);
        if (!AdapterPlanFile.render(expected).equals(AdapterPlanFile.render(committed))) {
            log.fail(subject + "the committed plan is not what its pinned names give");
            return;
        }
        List<String> failures = RenamedClientVerification.findFailures(hooks, committed.plan(), client);
        failures.forEach(failure -> log.fail(subject + failure));
        if (failures.isEmpty()) {
            tally.verified.merge(committed.namespace(), 1, Integer::sum);
            log.observe(subject + hooks.points().size() + " points redirect " + hooks.callSites()
                    + " calls of the client in " + committed.namespace().id() + " names");
        }
    }

    private static final class Tally {
        private final Map<RuntimeNamespace, Integer> verified = new EnumMap<>(RuntimeNamespace.class);
        private int ownBuilds;

        private String summary() {
            String byNamespace = verified.entrySet().stream()
                    .map(entry -> entry.getValue() + " in " + entry.getKey().id() + " names")
                    .collect(Collectors.joining(", "));
            return "pinned-call adapters verified on the production client: " + byNamespace + "; " + ownBuilds
                    + " target(s) with a build of their own";
        }
    }
}

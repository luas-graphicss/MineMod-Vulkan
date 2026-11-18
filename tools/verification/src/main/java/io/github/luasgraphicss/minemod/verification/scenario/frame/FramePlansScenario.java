package io.github.luasgraphicss.minemod.verification.scenario.frame;

import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameDownloads;
import io.github.luasgraphicss.minemod.verification.frame.FramePlansCheck;
import io.github.luasgraphicss.minemod.verification.frame.FrameTargets;
import io.github.luasgraphicss.minemod.verification.frame.FrameTargets.FrameTarget;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.plan.FramePlans;
import io.github.luasgraphicss.minemod.verification.frame.plan.FramePoints;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile;
import io.github.luasgraphicss.minemod.verification.frame.verify.RenamedFrameVerification;
import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.plan.ReleasePlans;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import io.github.luasgraphicss.minemod.verification.scenario.RepositoryScenario;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioEnvironment;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioLog;
import io.github.luasgraphicss.minemod.verification.scenario.ScenarioReport;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class FramePlansScenario implements RepositoryScenario {
    private static final String REFRESH_HINT = "; rewrite them with \"frames refresh\"";

    @Override
    public String name() {
        return "frame-plans";
    }

    @Override
    public ScenarioReport run(ScenarioEnvironment environment) throws IOException, InterruptedException {
        ScenarioLog log = new ScenarioLog();
        Path root = environment.repositoryRoot();
        List<String> violations = new FramePlansCheck().findViolations(root);
        if (!violations.isEmpty()) {
            violations.forEach(log::fail);
            return log.report();
        }
        List<FrameTarget> targets = FrameTargets.of(TargetManifestFile.inRepository(root).read());
        int answeredCalls = 0;
        try (PinnedDownloads clients = new PinnedDownloads(environment.cacheDirectory());
                NameDownloads names = new NameDownloads(environment.cacheDirectory())) {
            AdapterPlanResolver resolver = new AdapterPlanResolver(names);
            for (FrameTarget target : targets) {
                try (ClientArchive client = ClientArchive.open(clients.clientOf(target.release()))) {
                    answeredCalls += verify(root, target, client, resolver, log);
                }
            }
        }
        log.observe("the stand-ins answer " + answeredCalls + " OpenGL calls of the " + targets.size()
                + " clients that run without a mixin loader");
        log.observe("the client of forge is the vanilla client under the names that loader runs; the binary patches"
                + " and the classes of the loader are not applied here");
        return log.report();
    }

    private static int verify(
            Path root, FrameTarget target, ClientArchive client, AdapterPlanResolver resolver, ScenarioLog log)
            throws IOException, InterruptedException {
        String subject = target.release().id() + " " + target.loader().loader() + ": ";
        StandIns standIns = StandIns.ofLwjglGeneration(target.release().lwjgl().generation());
        FramePlans.Pinned pinned = FramePlans.pin(root, target, client, standIns, resolver);
        Path gamePlan = FramePlanFile.of(root, target.build().gameAdapter());
        Path loaderPlan = FramePlanFile.of(root, target.build().loaderAdapter());
        if (!Files.readString(gamePlan).equals(pinned.gamePlan())
                || !Files.readString(loaderPlan).equals(pinned.loaderPlan())) {
            log.fail(subject + "the committed frame plans are not what the client and its pinned names give"
                    + REFRESH_HINT);
            return 0;
        }
        FramePlanFile.Committed committed = FramePlanFile.read(loaderPlan);
        List<String> failures = RenamedFrameVerification.findFailures(
                ReleasePlans.planOf(pinned.points()), pinned.runtime().plan(), committed.atLaunch(), client, standIns);
        failures.forEach(failure -> log.fail(subject + failure));
        if (!failures.isEmpty()) {
            return 0;
        }
        Map<String, Integer> left = FramePoints.leftWithTheLibrary(client, standIns, pinned.points().windowing());
        log.observe(subject + committed.atLaunch().plansByStandIn().size() + " stand-ins answer "
                + pinned.points().callSites() + " OpenGL calls of the client at "
                + pinned.points().points().size() + " points in " + committed.names().namespace().id()
                + " names; " + left.values().stream().mapToInt(Integer::intValue).sum()
                + " calls stay with the library in " + left.keySet());
        return pinned.points().callSites();
    }
}

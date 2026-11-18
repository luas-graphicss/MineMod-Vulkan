package io.github.luasgraphicss.minemod.verification.frame.policy;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.frame.FrameTargets.FrameTarget;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile.Committed;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FramePlanRules {
    private FramePlanRules() {}

    public static List<String> findViolations(
            FrameTarget target, Committed game, Committed loader, AdapterPlan presentation, StandIns standIns) {
        List<String> violations = new ArrayList<>();
        String gameModule = target.build().gameAdapter();
        String loaderModule = target.build().loaderAdapter();
        requirePinnedOn(target.release(), gameModule, game.names().plan(), violations);
        requirePinnedOn(target.release(), loaderModule, loader.names().plan(), violations);
        if (game.names().namespace() != RuntimeNamespace.OFFICIAL) {
            violations.add(gameModule + " writes its frame plan in " + game.names().namespace().id()
                    + " names, the adapter of the game keeps the names of the client");
        }
        if (!readsTheNamesOf(presentation, loader.names())) {
            violations.add(loaderModule + " does not read its frame plan from the names its release plan pins");
        }
        if (!shapeOf(game.atLaunch()).equals(shapeOf(loader.atLaunch()))) {
            violations.add(loaderModule + " does not hand over the calls that " + gameModule + " pins");
        }
        requireCatalogued(gameModule, game.atLaunch(), standIns, violations);
        requireCatalogued(loaderModule, loader.atLaunch(), standIns, violations);
        return violations;
    }

    private static void requirePinnedOn(ReleaseTarget release, String module, ReleasePlan plan, List<String> violations) {
        if (!plan.release().equals(release.id()) || !plan.isPinnedFor(release.client().sha1())) {
            violations.add(module + " pins its frame plan on client " + plan.clientSha1() + " of " + plan.release()
                    + ", the manifest pins client " + release.client().sha1() + " of " + release.id());
        }
    }

    private static boolean readsTheNamesOf(AdapterPlan presentation, AdapterPlan frame) {
        return frame.namespace() == presentation.namespace()
                && frame.namesUrl().equals(presentation.namesUrl())
                && frame.namesSha1().equals(presentation.namesSha1());
    }

    private static void requireCatalogued(String module, FramePlan plan, StandIns standIns, List<String> violations) {
        for (Map.Entry<String, ReleasePlan> answered : plan.plansByStandIn().entrySet()) {
            for (CallSiteRedirect redirect : answered.getValue().redirects()) {
                Optional<String> catalogued = standIns.answering(redirect.callee().toString());
                if (catalogued.isEmpty() || !catalogued.get().equals(answered.getKey())) {
                    violations.add(module + " hands " + redirect.callee() + " to " + answered.getKey()
                            + ", which does not answer it");
                }
            }
        }
    }

    private static List<String> shapeOf(FramePlan plan) {
        List<String> shape = new ArrayList<>();
        plan.plansByStandIn().forEach((standIn, answered) -> answered.redirects()
                .forEach(redirect -> shape.add(standIn + " " + redirect.point().id() + " " + redirect.callee() + " "
                        + redirect.point().expectedMatches())));
        return shape;
    }
}

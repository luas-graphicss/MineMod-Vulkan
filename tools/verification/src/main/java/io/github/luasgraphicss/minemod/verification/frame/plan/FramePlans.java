package io.github.luasgraphicss.minemod.verification.frame.plan;

import io.github.luasgraphicss.minemod.verification.adapters.layout.ModuleFiles;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.plan.RuntimePlans;
import io.github.luasgraphicss.minemod.verification.adapters.storage.AdapterPlanFile;
import io.github.luasgraphicss.minemod.verification.frame.FrameTargets.FrameTarget;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import java.io.IOException;
import java.nio.file.Path;

public final class FramePlans {
    public record Pinned(ReleaseHooks points, AdapterPlan runtime, String gamePlan, String loaderPlan) {}

    private FramePlans() {}

    public static Pinned pin(
            Path repositoryRoot, FrameTarget target, ClientArchive client, StandIns standIns, AdapterPlanResolver names)
            throws IOException, InterruptedException {
        ReleaseHooks points = FramePoints.pinnedOn(target.release(), client, standIns);
        AdapterPlan official =
                AdapterPlan.written(RuntimePlans.written(points, RuntimeNamespace.OFFICIAL), RuntimeNamespace.OFFICIAL);
        AdapterPlan presentation =
                AdapterPlanFile.read(ModuleFiles.planOf(repositoryRoot, target.build().loaderAdapter()));
        AdapterPlan runtime = names.resolveAsPinned(points, target.loader(), presentation);
        return new Pinned(
                points, runtime, FramePlanFile.render(official, standIns), FramePlanFile.render(runtime, standIns));
    }
}

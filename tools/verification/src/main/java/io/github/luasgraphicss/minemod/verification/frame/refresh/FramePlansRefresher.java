package io.github.luasgraphicss.minemod.verification.frame.refresh;

import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.frame.FrameTargets;
import io.github.luasgraphicss.minemod.verification.frame.FrameTargets.FrameTarget;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.frame.plan.FramePlans;
import io.github.luasgraphicss.minemod.verification.frame.storage.FramePlanFile;
import io.github.luasgraphicss.minemod.verification.hooks.cache.PinnedDownloads;
import io.github.luasgraphicss.minemod.verification.hooks.scan.ClientArchive;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FramePlansRefresher {
    public record Written(int plans, int pinnedCalls) {}

    private FramePlansRefresher() {}

    public static Written refresh(
            Path repositoryRoot, TargetManifest targets, PinnedDownloads clients, AdapterPlanResolver names)
            throws IOException, InterruptedException {
        int plans = 0;
        int pinnedCalls = 0;
        for (FrameTarget target : FrameTargets.of(targets)) {
            StandIns standIns = StandIns.ofLwjglGeneration(target.release().lwjgl().generation());
            try (ClientArchive client = ClientArchive.open(clients.clientOf(target.release()))) {
                FramePlans.Pinned pinned = FramePlans.pin(repositoryRoot, target, client, standIns, names);
                write(FramePlanFile.of(repositoryRoot, target.build().gameAdapter()), pinned.gamePlan());
                write(FramePlanFile.of(repositoryRoot, target.build().loaderAdapter()), pinned.loaderPlan());
                plans += 2;
                pinnedCalls += pinned.points().callSites();
            }
        }
        return new Written(plans, pinnedCalls);
    }

    private static void write(Path plan, String text) throws IOException {
        Files.createDirectories(plan.getParent());
        Files.writeString(plan, text);
    }
}

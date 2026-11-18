package io.github.luasgraphicss.minemod.verification.frame;

import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.matrix.plan.AdapterBuildPlanner;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FrameTargets {
    public record FrameTarget(ReleaseTarget release, LoaderTarget loader, AdapterBuild build) {}

    private FrameTargets() {}

    public static List<FrameTarget> of(TargetManifest targets) {
        Iterator<AdapterBuild> builds = AdapterBuildPlanner.plan(targets).iterator();
        List<FrameTarget> frames = new ArrayList<>();
        for (ReleaseTarget release : targets.releases()) {
            for (LoaderTarget loader : release.loaders()) {
                AdapterBuild build = builds.next();
                if (hasNoMixinLoader(release)) {
                    frames.add(new FrameTarget(release, loader, build));
                }
            }
        }
        return frames;
    }

    private static boolean hasNoMixinLoader(ReleaseTarget release) {
        return release.loaders().stream().noneMatch(loader -> loader.loader() == LoaderTarget.Kind.FABRIC);
    }
}

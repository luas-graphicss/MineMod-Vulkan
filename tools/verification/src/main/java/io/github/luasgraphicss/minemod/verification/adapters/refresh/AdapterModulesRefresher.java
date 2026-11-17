package io.github.luasgraphicss.minemod.verification.adapters.refresh;

import io.github.luasgraphicss.minemod.verification.adapters.layout.ModuleFiles;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlanResolver;
import io.github.luasgraphicss.minemod.verification.adapters.plan.RuntimePlans;
import io.github.luasgraphicss.minemod.verification.adapters.storage.AdapterPlanFile;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.matrix.plan.AdapterBuildPlanner;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class AdapterModulesRefresher {
    public record Written(int gameAdapters, int loaderAdapters) {}

    private AdapterModulesRefresher() {}

    public static Written refresh(
            Path repositoryRoot, TargetManifest targets, PresentationHooks hooks, AdapterPlanResolver resolver)
            throws IOException, InterruptedException {
        Iterator<AdapterBuild> builds = AdapterBuildPlanner.plan(targets).iterator();
        Set<String> gameAdapters = new HashSet<>();
        int loaderAdapters = 0;
        for (ReleaseTarget release : targets.releases()) {
            ReleaseHooks releaseHooks = hooks.of(release.id()).orElseThrow();
            for (LoaderTarget loader : release.loaders()) {
                AdapterBuild build = builds.next();
                if (!ModuleFiles.hasItsOwnBuild(repositoryRoot, build.gameAdapter())
                        && gameAdapters.add(build.gameAdapter())) {
                    AdapterPlan official = AdapterPlan.written(
                            RuntimePlans.written(releaseHooks, RuntimeNamespace.OFFICIAL), RuntimeNamespace.OFFICIAL);
                    write(repositoryRoot, build.gameAdapter(), ModuleFiles.gameBuildFile(), official);
                }
                if (!ModuleFiles.hasItsOwnBuild(repositoryRoot, build.loaderAdapter())) {
                    AdapterPlan runtime = resolver.resolve(releaseHooks, loader);
                    write(repositoryRoot, build.loaderAdapter(), ModuleFiles.loaderBuildFile(), runtime);
                    loaderAdapters++;
                }
            }
        }
        return new Written(gameAdapters.size(), loaderAdapters);
    }

    private static void write(Path repositoryRoot, String module, String buildFile, AdapterPlan plan)
            throws IOException {
        Path planFile = ModuleFiles.planOf(repositoryRoot, module);
        Files.createDirectories(planFile.getParent());
        Files.writeString(ModuleFiles.buildFileOf(repositoryRoot, module), buildFile);
        Files.writeString(planFile, AdapterPlanFile.render(plan));
    }
}

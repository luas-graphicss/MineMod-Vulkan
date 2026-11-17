package io.github.luasgraphicss.minemod.verification.adapters;

import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.verification.adapters.layout.ModuleFiles;
import io.github.luasgraphicss.minemod.verification.adapters.names.NamespacePolicy;
import io.github.luasgraphicss.minemod.verification.adapters.names.RuntimeNamespace;
import io.github.luasgraphicss.minemod.verification.adapters.plan.AdapterPlan;
import io.github.luasgraphicss.minemod.verification.adapters.plan.RuntimePlans;
import io.github.luasgraphicss.minemod.verification.adapters.source.NameSources;
import io.github.luasgraphicss.minemod.verification.adapters.storage.AdapterPlanFile;
import io.github.luasgraphicss.minemod.verification.check.Check;
import io.github.luasgraphicss.minemod.verification.hooks.model.PresentationHooks;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.policy.PresentationHookRules;
import io.github.luasgraphicss.minemod.verification.hooks.storage.PresentationHooksFile;
import io.github.luasgraphicss.minemod.verification.matrix.model.AdapterBuild;
import io.github.luasgraphicss.minemod.verification.matrix.plan.AdapterBuildPlanner;
import io.github.luasgraphicss.minemod.verification.targets.model.LoaderTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.ReleaseTarget;
import io.github.luasgraphicss.minemod.verification.targets.model.TargetManifest;
import io.github.luasgraphicss.minemod.verification.targets.storage.TargetManifestFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class AdapterModulesCheck implements Check {
    private static final Pattern SHA1 = Pattern.compile("[0-9a-f]{40}");
    private static final String REFRESH_HINT = "; rewrite it with \"adapters refresh\"";

    @Override
    public String name() {
        return "adapter-modules";
    }

    @Override
    public List<String> findViolations(Path repositoryRoot) throws IOException {
        TargetManifest targets = TargetManifestFile.inRepository(repositoryRoot).read();
        PresentationHooks hooks = PresentationHooksFile.inRepository(repositoryRoot).read();
        List<String> violations = new ArrayList<>(PresentationHookRules.findViolations(hooks, targets));
        if (!violations.isEmpty()) {
            return violations;
        }
        Iterator<AdapterBuild> builds = AdapterBuildPlanner.plan(targets).iterator();
        Set<String> checkedGameAdapters = new HashSet<>();
        for (ReleaseTarget release : targets.releases()) {
            ReleaseHooks releaseHooks = hooks.of(release.id()).orElseThrow();
            for (LoaderTarget loader : release.loaders()) {
                AdapterBuild build = builds.next();
                if (checkedGameAdapters.add(build.gameAdapter())) {
                    checkGameAdapter(repositoryRoot, build.gameAdapter(), releaseHooks, violations);
                }
                checkLoaderAdapter(repositoryRoot, build.loaderAdapter(), releaseHooks, loader, violations);
            }
        }
        return violations;
    }

    private static void checkGameAdapter(
            Path repositoryRoot, String module, ReleaseHooks hooks, List<String> violations) throws IOException {
        if (ModuleFiles.hasItsOwnBuild(repositoryRoot, module)) {
            return;
        }
        if (!hasBuildFile(repositoryRoot, module, ModuleFiles.gameBuildFile(), violations)) {
            return;
        }
        AdapterPlan official =
                AdapterPlan.written(RuntimePlans.written(hooks, RuntimeNamespace.OFFICIAL), RuntimeNamespace.OFFICIAL);
        checkWrittenPlan(repositoryRoot, module, official, violations);
    }

    private static void checkLoaderAdapter(
            Path repositoryRoot, String module, ReleaseHooks hooks, LoaderTarget loader, List<String> violations)
            throws IOException {
        if (ModuleFiles.hasItsOwnBuild(repositoryRoot, module)) {
            return;
        }
        if (!hasBuildFile(repositoryRoot, module, ModuleFiles.loaderBuildFile(), violations)) {
            return;
        }
        RuntimeNamespace namespace = NamespacePolicy.of(loader);
        if (!namespace.isReadFromMappings()) {
            checkWrittenPlan(
                    repositoryRoot,
                    module,
                    AdapterPlan.written(RuntimePlans.written(hooks, namespace), namespace),
                    violations);
            return;
        }
        Path planFile = ModuleFiles.planOf(repositoryRoot, module);
        if (!Files.isRegularFile(planFile)) {
            violations.add(module + " has no release plan" + REFRESH_HINT);
            return;
        }
        try {
            checkMappedPlan(module, AdapterPlanFile.read(planFile), hooks, namespace, violations);
        } catch (IOException malformed) {
            violations.add(module + ": " + malformed.getMessage());
        }
    }

    private static void checkMappedPlan(
            String module, AdapterPlan committed, ReleaseHooks hooks, RuntimeNamespace namespace, List<String> violations) {
        if (committed.namespace() != namespace) {
            violations.add(module + " is written in " + committed.namespace().id() + " names, its loader runs "
                    + namespace.id() + " names");
            return;
        }
        String source = NameSources.of(hooks.release(), namespace).url();
        if (!committed.namesUrl().equals(source)) {
            violations.add(module + " reads its names from " + committed.namesUrl() + ", expected " + source);
        }
        if (!SHA1.matcher(committed.namesSha1()).matches()) {
            violations.add(module + " does not pin the SHA-1 of the file its names were read from");
        }
        ReleasePlan official = RuntimePlans.written(hooks, RuntimeNamespace.OFFICIAL);
        if (!shapeOf(committed.plan()).equals(shapeOf(official))) {
            violations.add(module + " does not redirect the calls pinned in manifests/presentation-hooks.json"
                    + REFRESH_HINT);
        }
    }

    private static void checkWrittenPlan(Path repositoryRoot, String module, AdapterPlan expected, List<String> violations)
            throws IOException {
        Path planFile = ModuleFiles.planOf(repositoryRoot, module);
        if (!Files.isRegularFile(planFile)) {
            violations.add(module + " has no release plan" + REFRESH_HINT);
        } else if (!Files.readString(planFile).equals(AdapterPlanFile.render(expected))) {
            violations.add(module + " does not carry the plan pinned in manifests/presentation-hooks.json"
                    + REFRESH_HINT);
        }
    }

    private static boolean hasBuildFile(Path repositoryRoot, String module, String expected, List<String> violations)
            throws IOException {
        Path buildFile = ModuleFiles.buildFileOf(repositoryRoot, module);
        if (Files.isRegularFile(buildFile) && Files.readString(buildFile).equals(expected)) {
            return true;
        }
        violations.add(module + " has no build file of a pinned-call adapter" + REFRESH_HINT);
        return false;
    }

    private static List<String> shapeOf(ReleasePlan plan) {
        List<String> shape = new ArrayList<>();
        shape.add(plan.release());
        shape.add(plan.clientSha1());
        plan.redirects()
                .forEach(redirect -> shape.add(redirect.point().id() + " " + redirect.callee() + " "
                        + redirect.point().expectedMatches()));
        return shape;
    }
}

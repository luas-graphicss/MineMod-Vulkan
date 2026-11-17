package io.github.luasgraphicss.minemod.verification.scenario.matrix;

import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformGate;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.preflight.ClassSource;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.CallSiteRewriter;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.RewrittenClass;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.plan.ReleasePlans;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RenamedClientVerification {
    private RenamedClientVerification() {}

    public static List<String> findFailures(ReleaseHooks hooks, ReleasePlan runtimePlan, ClassSource client)
            throws IOException {
        List<String> failures = new ArrayList<>();
        Map<String, Renaming> renamings = renamingsOf(ReleasePlans.planOf(hooks), runtimePlan, failures);
        if (!failures.isEmpty()) {
            return failures;
        }
        CallSiteRewriter rewriter = new CallSiteRewriter(runtimePlan.redirects(), HookOwners.of(hooks.windowing()));
        CallSiteScanner library = new CallSiteScanner(PresentationCalls.of(hooks.windowing()));
        List<PointApplication> applications = new ArrayList<>();
        for (Map.Entry<String, Renaming> renaming : renamings.entrySet()) {
            Optional<byte[]> official = client.classFile(renaming.getKey());
            if (official.isEmpty()) {
                failures.add("the client has no class " + renaming.getKey());
                continue;
            }
            String runtimeClass = renaming.getValue().runtimeClass();
            RewrittenClass rewritten = rewriter.rewrite(
                    runtimeClass, DeclarationRenamer.rename(official.get(), runtimeClass, renaming.getValue().methods()));
            applications.addAll(rewritten.applications());
            if (!library.scan(rewritten.classFile()).isEmpty()) {
                failures.add(runtimeClass + " still calls the windowing library after the redirect");
            }
        }
        for (TransformViolation violation :
                TransformGate.findViolations(runtimePlan.transformPlan(), applications, List.of())) {
            failures.add(violation.description());
        }
        return failures;
    }

    private static Map<String, Renaming> renamingsOf(ReleasePlan official, ReleasePlan runtime, List<String> failures) {
        Map<String, MethodTarget> runtimeMethods = new HashMap<>();
        for (CallSiteRedirect redirect : runtime.redirects()) {
            runtimeMethods.put(redirect.point().id(), redirect.point().target());
        }
        Map<String, Renaming> renamings = new LinkedHashMap<>();
        for (CallSiteRedirect redirect : official.redirects()) {
            MethodTarget from = redirect.point().target();
            MethodTarget to = runtimeMethods.get(redirect.point().id());
            if (to == null) {
                failures.add("the plan of the loader has no point " + redirect.point().id());
                continue;
            }
            Renaming renaming = renamings.computeIfAbsent(from.owner(), owner -> new Renaming(to.owner(), new HashMap<>()));
            if (!renaming.runtimeClass().equals(to.owner())) {
                failures.add(from.owner() + " is named both " + renaming.runtimeClass() + " and " + to.owner());
            }
            renaming.methods().put(from.name() + from.descriptor(), to);
        }
        return renamings;
    }

    private record Renaming(String runtimeClass, Map<String, MethodTarget> methods) {}
}

package io.github.luasgraphicss.minemod.verification.scenario.matrix;

import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformGate;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.preflight.ClassSource;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.CallSiteRewriter;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.RewrittenClass;
import io.github.luasgraphicss.minemod.verification.hooks.catalog.PresentationCalls;
import io.github.luasgraphicss.minemod.verification.hooks.model.ReleaseHooks;
import io.github.luasgraphicss.minemod.verification.hooks.plan.ReleasePlans;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import io.github.luasgraphicss.minemod.verification.scenario.matrix.ClassRenamings.Renaming;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RenamedClientVerification {
    private RenamedClientVerification() {}

    public static List<String> findFailures(ReleaseHooks hooks, ReleasePlan runtimePlan, ClassSource client)
            throws IOException {
        List<String> failures = new ArrayList<>();
        Map<String, Renaming> renamings = ClassRenamings.between(ReleasePlans.planOf(hooks), runtimePlan, failures);
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
}

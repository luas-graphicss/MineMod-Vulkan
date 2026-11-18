package io.github.luasgraphicss.minemod.verification.frame.verify;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.FramePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformGate;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.preflight.ClassSource;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.CallSiteRewriter;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.RewrittenClass;
import io.github.luasgraphicss.minemod.verification.frame.catalog.StandIns;
import io.github.luasgraphicss.minemod.verification.hooks.scan.CallSiteScanner;
import io.github.luasgraphicss.minemod.verification.scenario.matrix.ClassRenamings;
import io.github.luasgraphicss.minemod.verification.scenario.matrix.ClassRenamings.Renaming;
import io.github.luasgraphicss.minemod.verification.scenario.matrix.DeclarationRenamer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RenamedFrameVerification {
    private RenamedFrameVerification() {}

    public static List<String> findFailures(
            ReleasePlan official, ReleasePlan runtime, FramePlan atLaunch, ClassSource client, StandIns standIns)
            throws IOException {
        List<String> failures = new ArrayList<>();
        Map<String, Renaming> renamings = ClassRenamings.between(official, runtime, failures);
        if (!failures.isEmpty()) {
            return failures;
        }
        Map<String, CallSiteRewriter> rewriters = new LinkedHashMap<>();
        atLaunch.plansByStandIn()
                .forEach((standIn, plan) -> rewriters.put(standIn, new CallSiteRewriter(plan.redirects(), standIn)));
        CallSiteScanner library = new CallSiteScanner(standIns.calls());
        Map<String, List<PointApplication>> applications = new HashMap<>();
        for (Map.Entry<String, Renaming> renaming : renamings.entrySet()) {
            Optional<byte[]> officialClass = client.classFile(renaming.getKey());
            if (officialClass.isEmpty()) {
                failures.add("the client has no class " + renaming.getKey());
                continue;
            }
            String runtimeClass = renaming.getValue().runtimeClass();
            byte[] answered =
                    DeclarationRenamer.rename(officialClass.get(), runtimeClass, renaming.getValue().methods());
            for (Map.Entry<String, CallSiteRewriter> rewriter : rewriters.entrySet()) {
                if (rewriter.getValue().rewrites(runtimeClass)) {
                    RewrittenClass rewritten = rewriter.getValue().rewrite(runtimeClass, answered);
                    applications
                            .computeIfAbsent(rewriter.getKey(), standIn -> new ArrayList<>())
                            .addAll(rewritten.applications());
                    answered = rewritten.classFile();
                }
            }
            if (!library.scan(answered).isEmpty()) {
                failures.add(runtimeClass + " still calls OpenGL where a stand-in answers");
            }
        }
        atLaunch.plansByStandIn().forEach((standIn, plan) -> {
            List<PointApplication> applied = applications.getOrDefault(standIn, List.of());
            for (TransformViolation violation : TransformGate.findViolations(plan.transformPlan(), applied, List.of())) {
                failures.add(violation.description());
            }
        });
        return failures;
    }
}

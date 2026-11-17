package io.github.luasgraphicss.minemod.adapters.loaders.launch.apply;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.transformers.application.ForeignTransform;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.preflight.ClassSource;
import io.github.luasgraphicss.minemod.adapters.transformers.preflight.PlanPreflight;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.CallSiteRewriter;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.RewrittenClass;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public final class ClassFileApplication {
    private final ReleasePlan plan;
    private final CallSiteRewriter rewriter;

    public ClassFileApplication(ReleasePlan plan, String hookOwner) {
        this.plan = plan;
        this.rewriter = new CallSiteRewriter(plan.redirects(), hookOwner);
    }

    public boolean rewrites(String className) {
        return rewriter.rewrites(className);
    }

    public byte[] rewrite(String className, byte[] classFile) {
        RewrittenClass rewritten = rewriter.rewrite(className, classFile);
        ClassGate.requireExactMatches(plan, className, rewritten.applications());
        return rewritten.classFile();
    }

    public void requireApplicableTo(ClassSource classes) throws IOException {
        List<TransformViolation> violations = PlanPreflight.findViolations(
                plan.transformPlan(), rewriter, classes, Collections.<ForeignTransform>emptyList());
        if (!violations.isEmpty()) {
            throw new PlanRefusedException(plan.release(), violations);
        }
    }
}

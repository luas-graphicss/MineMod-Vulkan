package io.github.luasgraphicss.minemod.adapters.transformers.preflight;

import io.github.luasgraphicss.minemod.adapters.transformers.application.ForeignTransform;
import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformGate;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPlan;
import io.github.luasgraphicss.minemod.adapters.transformers.rewrite.CallSiteRewriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlanPreflight {
    private PlanPreflight() {}

    public static List<TransformViolation> findViolations(
            TransformPlan plan, CallSiteRewriter rewriter, ClassSource classes, List<ForeignTransform> foreignTransforms)
            throws IOException {
        List<PointApplication> applications = new ArrayList<>();
        for (String className : rewriter.rewrittenClasses()) {
            Optional<byte[]> classFile = classes.classFile(className);
            if (classFile.isPresent()) {
                applications.addAll(rewriter.rewrite(className, classFile.get()).applications());
            }
        }
        return TransformGate.findViolations(plan, applications, foreignTransforms);
    }
}

package io.github.luasgraphicss.minemod.adapters.loaders.launch.apply;

import io.github.luasgraphicss.minemod.adapters.loaders.launch.plan.PlanRefusedException;
import io.github.luasgraphicss.minemod.adapters.transformers.application.ForeignTransform;
import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.callsite.CallSiteRedirect;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformGate;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPlan;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import io.github.luasgraphicss.minemod.adapters.transformers.release.ReleasePlan;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ClassGate {
    private ClassGate() {}

    static void requireExactMatches(ReleasePlan plan, String className, List<PointApplication> applications) {
        List<TransformPoint> pointsOfClass = new ArrayList<>();
        for (CallSiteRedirect redirect : plan.redirects()) {
            if (redirect.enclosingClass().equals(className)) {
                pointsOfClass.add(redirect.point());
            }
        }
        List<TransformViolation> violations = TransformGate.findViolations(
                new TransformPlan(pointsOfClass), applications, Collections.<ForeignTransform>emptyList());
        if (!violations.isEmpty()) {
            throw new PlanRefusedException(plan.release(), violations);
        }
    }
}

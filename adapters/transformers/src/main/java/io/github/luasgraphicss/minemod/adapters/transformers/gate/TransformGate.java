package io.github.luasgraphicss.minemod.adapters.transformers.gate;

import io.github.luasgraphicss.minemod.adapters.transformers.application.ForeignTransform;
import io.github.luasgraphicss.minemod.adapters.transformers.application.PointApplication;
import io.github.luasgraphicss.minemod.adapters.transformers.gate.TransformViolation.Kind;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformKind;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPlan;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TransformGate {
    private TransformGate() {}

    public static List<TransformViolation> findViolations(
            TransformPlan plan, List<PointApplication> applications, List<ForeignTransform> foreignTransforms) {
        List<TransformViolation> violations = new ArrayList<>();
        Map<String, Integer> matchesByPoint = collectMatches(plan, applications, violations);
        for (TransformPoint point : plan.points()) {
            checkMatches(point, matchesByPoint, violations);
        }
        for (ForeignTransform foreign : foreignTransforms) {
            checkOwnership(plan, foreign, violations);
        }
        return Collections.unmodifiableList(violations);
    }

    private static Map<String, Integer> collectMatches(
            TransformPlan plan, List<PointApplication> applications, List<TransformViolation> violations) {
        Map<String, Integer> matchesByPoint = new HashMap<>();
        for (PointApplication application : applications) {
            String id = application.pointId();
            if (!plan.find(id).isPresent()) {
                violations.add(new TransformViolation(
                        Kind.UNDECLARED_POINT, "point " + id + " was applied without being declared"));
            } else if (matchesByPoint.put(id, application.matches()) != null) {
                violations.add(new TransformViolation(
                        Kind.DUPLICATE_APPLICATION, "point " + id + " was applied more than once"));
            }
        }
        return matchesByPoint;
    }

    private static void checkMatches(
            TransformPoint point, Map<String, Integer> matchesByPoint, List<TransformViolation> violations) {
        Integer matches = matchesByPoint.get(point.id());
        if (matches == null) {
            violations.add(new TransformViolation(
                    Kind.NOT_APPLIED, "point " + point.id() + " was never applied to " + point.target()));
            return;
        }
        if (matches == 0) {
            violations.add(new TransformViolation(
                    Kind.NO_MATCH,
                    "point " + point.id() + " matched nothing in " + point.target()
                            + ", expected " + point.expectedMatches()));
            return;
        }
        if (matches != point.expectedMatches()) {
            violations.add(new TransformViolation(
                    Kind.UNEXPECTED_MATCH_COUNT,
                    "point " + point.id() + " matched " + matches + " times in " + point.target()
                            + ", expected " + point.expectedMatches()));
        }
    }

    private static void checkOwnership(
            TransformPlan plan, ForeignTransform foreign, List<TransformViolation> violations) {
        for (TransformPoint point : plan.pointsAt(foreign.target())) {
            if (point.kind() == TransformKind.REPLACEMENT) {
                violations.add(new TransformViolation(
                        Kind.REPLACEMENT_OVER_FOREIGN_TRANSFORM,
                        "point " + point.id() + " replaces " + point.target()
                                + ", which " + foreign.origin() + " also transforms"));
            } else if (foreign.kind() == TransformKind.REPLACEMENT) {
                violations.add(new TransformViolation(
                        Kind.CONCURRENT_REPLACEMENT,
                        foreign.origin() + " replaces " + point.target()
                                + ", which point " + point.id() + " hooks"));
            }
        }
    }
}

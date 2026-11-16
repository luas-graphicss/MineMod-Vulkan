package io.github.luasgraphicss.minemod.adapters.transformers.point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TransformPlan {
    private final Map<String, TransformPoint> pointsById;

    public TransformPlan(List<TransformPoint> points) {
        Map<String, TransformPoint> byId = new LinkedHashMap<>();
        Map<MethodTarget, TransformPoint> firstByTarget = new HashMap<>();
        for (TransformPoint point : points) {
            if (byId.put(point.id(), point) != null) {
                throw new IllegalArgumentException("point " + point.id() + " is declared twice");
            }
            TransformPoint earlier = firstByTarget.putIfAbsent(point.target(), point);
            if (earlier != null && eitherReplaces(earlier, point)) {
                throw new IllegalArgumentException("points " + earlier.id() + " and " + point.id()
                        + " share the replaced method " + point.target());
            }
        }
        this.pointsById = Collections.unmodifiableMap(byId);
    }

    public Collection<TransformPoint> points() {
        return pointsById.values();
    }

    public Optional<TransformPoint> find(String id) {
        return Optional.ofNullable(pointsById.get(id));
    }

    public List<TransformPoint> pointsAt(MethodTarget target) {
        List<TransformPoint> matching = new ArrayList<>();
        for (TransformPoint point : pointsById.values()) {
            if (point.target().equals(target)) {
                matching.add(point);
            }
        }
        return matching;
    }

    private static boolean eitherReplaces(TransformPoint first, TransformPoint second) {
        return first.kind() == TransformKind.REPLACEMENT || second.kind() == TransformKind.REPLACEMENT;
    }
}

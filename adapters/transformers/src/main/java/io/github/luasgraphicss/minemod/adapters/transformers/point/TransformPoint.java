package io.github.luasgraphicss.minemod.adapters.transformers.point;

import java.util.Objects;

public final class TransformPoint {
    private static final int REPLACED_BODIES = 1;

    private final String id;
    private final MethodTarget target;
    private final TransformKind kind;
    private final int expectedMatches;

    private TransformPoint(String id, MethodTarget target, TransformKind kind, int expectedMatches) {
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("a transform point needs an id");
        }
        this.id = id;
        this.target = Objects.requireNonNull(target, "target");
        this.kind = kind;
        this.expectedMatches = expectedMatches;
    }

    public static TransformPoint hook(String id, MethodTarget target, int expectedMatches) {
        if (expectedMatches < 1) {
            throw new IllegalArgumentException(
                    "point " + id + " must expect at least one match, not " + expectedMatches);
        }
        return new TransformPoint(id, target, TransformKind.HOOK, expectedMatches);
    }

    public static TransformPoint replacement(String id, MethodTarget target) {
        return new TransformPoint(id, target, TransformKind.REPLACEMENT, REPLACED_BODIES);
    }

    public String id() {
        return id;
    }

    public MethodTarget target() {
        return target;
    }

    public TransformKind kind() {
        return kind;
    }

    public int expectedMatches() {
        return expectedMatches;
    }
}

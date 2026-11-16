package io.github.luasgraphicss.minemod.adapters.transformers.application;

import io.github.luasgraphicss.minemod.adapters.transformers.point.MethodTarget;
import io.github.luasgraphicss.minemod.adapters.transformers.point.TransformKind;
import java.util.Objects;

public final class ForeignTransform {
    private final String origin;
    private final MethodTarget target;
    private final TransformKind kind;

    public ForeignTransform(String origin, MethodTarget target, TransformKind kind) {
        if (origin.trim().isEmpty()) {
            throw new IllegalArgumentException("a foreign transform needs the origin that applies it");
        }
        this.origin = origin;
        this.target = Objects.requireNonNull(target, "target");
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public String origin() {
        return origin;
    }

    public MethodTarget target() {
        return target;
    }

    public TransformKind kind() {
        return kind;
    }
}

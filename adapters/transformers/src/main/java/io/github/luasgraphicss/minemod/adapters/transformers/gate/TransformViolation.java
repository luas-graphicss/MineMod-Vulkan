package io.github.luasgraphicss.minemod.adapters.transformers.gate;

import java.util.Objects;

public final class TransformViolation {
    public enum Kind {
        UNDECLARED_POINT,
        DUPLICATE_APPLICATION,
        NOT_APPLIED,
        NO_MATCH,
        UNEXPECTED_MATCH_COUNT,
        REPLACEMENT_OVER_FOREIGN_TRANSFORM,
        CONCURRENT_REPLACEMENT
    }

    private final Kind kind;
    private final String description;

    TransformViolation(Kind kind, String description) {
        this.kind = kind;
        this.description = description;
    }

    public Kind kind() {
        return kind;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TransformViolation)) {
            return false;
        }
        TransformViolation violation = (TransformViolation) other;
        return kind == violation.kind && description.equals(violation.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, description);
    }

    @Override
    public String toString() {
        return kind + ": " + description;
    }
}

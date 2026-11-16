package io.github.luasgraphicss.minemod.compatibility.contracts.report;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.Objects;

public final class ResourceContext {
    private final ObjectKind kind;
    private final int name;

    public ResourceContext(ObjectKind kind, int name) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.name = name;
    }

    public ObjectKind kind() {
        return kind;
    }

    public int name() {
        return name;
    }

    @Override
    public String toString() {
        return kind + " " + name;
    }
}

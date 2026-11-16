package io.github.luasgraphicss.minemod.compatibility.dispatch.catalog;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.Objects;

public final class NameArgument {
    private final int index;
    private final ObjectKind kind;

    public NameArgument(int index, ObjectKind kind) {
        if (index < 0) {
            throw new IllegalArgumentException("argument index out of range: " + index);
        }
        this.index = index;
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public int index() {
        return index;
    }

    public ObjectKind kind() {
        return kind;
    }
}

package io.github.luasgraphicss.minemod.compatibility.resources.names;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectScope;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class NameSpace {
    private final ObjectScope scope;
    private final Map<ObjectKind, NameTable> tables = new EnumMap<>(ObjectKind.class);

    public NameSpace(ObjectScope scope, int capacityPerKind) {
        this.scope = Objects.requireNonNull(scope, "scope");
        for (ObjectKind kind : ObjectKind.values()) {
            if (kind.scope() == scope) {
                tables.put(kind, new NameTable(kind, capacityPerKind));
            }
        }
    }

    public ObjectScope scope() {
        return scope;
    }

    public NameTable table(ObjectKind kind) {
        NameTable table = tables.get(kind);
        if (table == null) {
            throw new IllegalArgumentException(kind + " names are not kept per " + scope);
        }
        return table;
    }

    public int liveCount() {
        int count = 0;
        for (NameTable table : tables.values()) {
            count += table.liveCount();
        }
        return count;
    }

    public int retiringCount() {
        int count = 0;
        for (NameTable table : tables.values()) {
            count += table.retiringCount();
        }
        return count;
    }

    public int collect(long completedFrame, ObjectDisposer disposer) {
        int disposed = 0;
        for (NameTable table : tables.values()) {
            disposed += table.collect(completedFrame, disposer);
        }
        return disposed;
    }

    public int discardAll() {
        int discarded = 0;
        for (NameTable table : tables.values()) {
            discarded += table.discardAll();
        }
        return discarded;
    }
}

package io.github.luasgraphicss.minemod.compatibility.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class BridgeDescriptor {
    private final String id;
    private final int spiVersion;
    private final Set<String> operations;

    public BridgeDescriptor(String id, int spiVersion, Collection<String> operations) {
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("a bridge needs an id");
        }
        if (operations.isEmpty()) {
            throw new IllegalArgumentException("bridge " + id + " serves no operation");
        }
        this.id = id;
        this.spiVersion = spiVersion;
        this.operations = Collections.unmodifiableSet(new TreeSet<>(operations));
    }

    public String id() {
        return id;
    }

    public int spiVersion() {
        return spiVersion;
    }

    public Set<String> operations() {
        return operations;
    }
}

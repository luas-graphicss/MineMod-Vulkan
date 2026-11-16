package io.github.luasgraphicss.minemod.contract.capability;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum Capability {
    COMMAND_STREAM(0),
    BUFFER_RESOURCES(1);

    private final long mask;

    Capability(int bit) {
        this.mask = 1L << bit;
    }

    public long mask() {
        return mask;
    }

    public static long maskOf(Set<Capability> capabilities) {
        long mask = 0;
        for (Capability capability : capabilities) {
            mask |= capability.mask;
        }
        return mask;
    }

    public static Set<Capability> knownIn(long mask) {
        Set<Capability> known = EnumSet.noneOf(Capability.class);
        for (Capability capability : values()) {
            if ((mask & capability.mask) != 0) {
                known.add(capability);
            }
        }
        return Collections.unmodifiableSet(known);
    }
}

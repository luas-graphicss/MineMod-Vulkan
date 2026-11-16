package io.github.luasgraphicss.minemod.compatibility.layer.state;

import io.github.luasgraphicss.minemod.compatibility.state.reconcile.LeftoverPolicy;
import java.util.Objects;

public final class TrackedCapability {
    private final int capability;
    private final LeftoverPolicy policy;

    public TrackedCapability(int capability, LeftoverPolicy policy) {
        this.capability = capability;
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public int capability() {
        return capability;
    }

    public LeftoverPolicy policy() {
        return policy;
    }
}

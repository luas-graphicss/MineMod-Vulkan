package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.state.reconcile.LeftoverPolicy;
import java.util.Arrays;
import java.util.Optional;

public enum TrackedState {
    BLEND(0x0BE2, LeftoverPolicy.RESTORE),
    DEPTH_TEST(0x0B71, LeftoverPolicy.ADOPT),
    CULL_FACE(0x0B44, LeftoverPolicy.RESTORE),
    SCISSOR_TEST(0x0C11, LeftoverPolicy.ADOPT);

    public static final long DISABLED = 0;
    public static final long ENABLED = 1;

    private final int capability;
    private final LeftoverPolicy policy;

    TrackedState(int capability, LeftoverPolicy policy) {
        this.capability = capability;
        this.policy = policy;
    }

    public static Optional<TrackedState> ofCapability(long capability) {
        return Arrays.stream(values()).filter(state -> state.capability == capability).findFirst();
    }

    public static LeftoverPolicy[] policies() {
        return Arrays.stream(values()).map(TrackedState::policy).toArray(LeftoverPolicy[]::new);
    }

    public int capability() {
        return capability;
    }

    public LeftoverPolicy policy() {
        return policy;
    }
}

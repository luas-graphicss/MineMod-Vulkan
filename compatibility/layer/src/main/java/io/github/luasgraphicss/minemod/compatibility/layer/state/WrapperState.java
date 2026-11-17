package io.github.luasgraphicss.minemod.compatibility.layer.state;

import io.github.luasgraphicss.minemod.compatibility.contracts.support.OperationSupport;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.LeftoverPolicy;
import io.github.luasgraphicss.minemod.compatibility.state.reconcile.StateReconciler;
import io.github.luasgraphicss.minemod.compatibility.translation.call.CallObserver;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.report.OperationReporter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class WrapperState implements CallObserver {
    private static final String ENABLE = "glEnable";
    private static final String DISABLE = "glDisable";
    private static final long DISABLED = 0;
    private static final long ENABLED = 1;
    private final Map<Integer, Integer> slotsByCapability = new HashMap<>();
    private final int[] capabilities;
    private final long[] wrapperValues;
    private final StateReconciler reconciler;
    private final ProviderCalls provider;
    private final OperationReporter reporter;

    public WrapperState(List<TrackedCapability> tracked, ProviderCalls provider, OperationReporter reporter) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.reporter = Objects.requireNonNull(reporter, "reporter");
        this.capabilities = new int[tracked.size()];
        this.wrapperValues = new long[tracked.size()];
        LeftoverPolicy[] policies = new LeftoverPolicy[tracked.size()];
        for (int slot = 0; slot < capabilities.length; slot++) {
            capabilities[slot] = tracked.get(slot).capability();
            policies[slot] = tracked.get(slot).policy();
            if (slotsByCapability.put(capabilities[slot], slot) != null) {
                throw new IllegalArgumentException(
                        "capability 0x" + Integer.toHexString(capabilities[slot]) + " is tracked twice");
            }
        }
        this.reconciler = new StateReconciler(
                new long[capabilities.length], policies, this::applyToProvider, this::adoptIntoWrapper);
    }

    public StateReconciler reconciler() {
        return reconciler;
    }

    public void set(int capability, boolean enabled) {
        int slot = slotOf(capability);
        long value = enabled ? ENABLED : DISABLED;
        wrapperValues[slot] = value;
        reconciler.setSemantic(slot, value);
    }

    public boolean isEnabled(int capability) {
        return wrapperValues[slotOf(capability)] == ENABLED;
    }

    @Override
    public void observe(String function, long[] arguments) {
        boolean enables = function.equals(ENABLE);
        if (!enables && !function.equals(DISABLE)) {
            return;
        }
        Integer slot = slotsByCapability.get((int) arguments[0]);
        if (slot == null) {
            return;
        }
        if (reconciler.inSection()) {
            reconciler.observeDirect(slot, enables ? ENABLED : DISABLED);
        } else {
            reporter.report(function + " of a wrapper-tracked capability outside a callback", OperationSupport.OUT_OF_CONTRACT);
        }
    }

    private int slotOf(int capability) {
        Integer slot = slotsByCapability.get(capability);
        if (slot == null) {
            throw new IllegalArgumentException(
                    "capability 0x" + Integer.toHexString(capability) + " is not tracked by the wrappers");
        }
        return slot;
    }

    private void applyToProvider(int slot, long value) {
        provider.invoke(value == ENABLED ? ENABLE : DISABLE, new long[] {capabilities[slot]});
    }

    private void adoptIntoWrapper(int slot, long value) {
        wrapperValues[slot] = value;
    }
}

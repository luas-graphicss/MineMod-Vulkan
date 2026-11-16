package io.github.luasgraphicss.minemod.compatibility.state.reconcile;

import java.util.Objects;

public final class StateReconciler {
    private final long[] semantic;
    private final long[] context;
    private final LeftoverPolicy[] policies;
    private final StateApplier applier;
    private final AdoptedStateSink adopted;
    private boolean inSection;

    public StateReconciler(
            long[] initialValues, LeftoverPolicy[] policies, StateApplier applier, AdoptedStateSink adopted) {
        if (policies.length != initialValues.length) {
            throw new IllegalArgumentException(
                    initialValues.length + " state slot(s) cannot follow " + policies.length + " leftover policies");
        }
        this.semantic = initialValues.clone();
        this.context = initialValues.clone();
        this.policies = policies.clone();
        this.applier = Objects.requireNonNull(applier, "applier");
        this.adopted = Objects.requireNonNull(adopted, "adopted");
    }

    public void setSemantic(int slot, long value) {
        semantic[slot] = value;
        if (inSection && context[slot] != value) {
            applier.apply(slot, value);
            context[slot] = value;
        }
    }

    public void observeDirect(int slot, long value) {
        if (!inSection) {
            throw new IllegalStateException("direct state changes only happen inside an OpenGL section");
        }
        context[slot] = value;
    }

    public int enterSection() {
        if (inSection) {
            throw new IllegalStateException("an OpenGL section is already open");
        }
        int applied = 0;
        for (int slot = 0; slot < semantic.length; slot++) {
            if (context[slot] != semantic[slot]) {
                applier.apply(slot, semantic[slot]);
                context[slot] = semantic[slot];
                applied++;
            }
        }
        inSection = true;
        return applied;
    }

    public int exitSection() {
        if (!inSection) {
            throw new IllegalStateException("no OpenGL section is open");
        }
        int adoptedCount = 0;
        for (int slot = 0; slot < semantic.length; slot++) {
            if (context[slot] != semantic[slot] && policies[slot] == LeftoverPolicy.ADOPT) {
                adopted.adopt(slot, context[slot]);
                semantic[slot] = context[slot];
                adoptedCount++;
            }
        }
        inSection = false;
        return adoptedCount;
    }

    public boolean inSection() {
        return inSection;
    }

    public long semanticValue(int slot) {
        return semantic[slot];
    }

    public long contextValue(int slot) {
        return context[slot];
    }
}

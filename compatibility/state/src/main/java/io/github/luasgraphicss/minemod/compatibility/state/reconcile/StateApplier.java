package io.github.luasgraphicss.minemod.compatibility.state.reconcile;

public interface StateApplier {
    void apply(int slot, long value);
}

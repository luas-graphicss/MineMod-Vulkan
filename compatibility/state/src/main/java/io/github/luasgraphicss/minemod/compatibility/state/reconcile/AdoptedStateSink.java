package io.github.luasgraphicss.minemod.compatibility.state.reconcile;

public interface AdoptedStateSink {
    void adopt(int slot, long value);
}

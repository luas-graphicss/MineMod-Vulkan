package io.github.luasgraphicss.minemod.compatibility.translation.call;

public interface CallObserver {
    void observe(String function, long[] arguments);
}

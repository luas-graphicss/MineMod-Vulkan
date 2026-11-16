package io.github.luasgraphicss.minemod.compatibility.dispatch.install;

public interface FunctionLookup {
    long UNAVAILABLE = 0;

    long addressOf(String function);
}

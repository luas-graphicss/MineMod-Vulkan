package io.github.luasgraphicss.minemod.compatibility.natives.gate;

public interface CallInterceptor {
    long call(String function, long[] words);
}

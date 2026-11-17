package io.github.luasgraphicss.minemod.verification.differential.provider;

@FunctionalInterface
interface GlCalls {
    long call(String function, long... words);
}

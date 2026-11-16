package io.github.luasgraphicss.minemod.compatibility.translation.legacy;

public interface ProviderLists {
    int NO_LISTS = 0;

    int generate(int range);

    void delete(int firstBacking, int range);
}

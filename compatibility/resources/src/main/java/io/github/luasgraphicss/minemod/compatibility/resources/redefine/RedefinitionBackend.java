package io.github.luasgraphicss.minemod.compatibility.resources.redefine;

public interface RedefinitionBackend {
    int allocatePrivateStorage();

    void redirectReferences(int fromBacking, int toBacking);

    void discard(int backing);
}

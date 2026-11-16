package io.github.luasgraphicss.minemod.verification.scenario.simulation;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.List;

public interface GlDriver {
    long call(String function, long... arguments);

    int[] generate(ObjectKind kind, int count);

    void delete(ObjectKind kind, int... names);

    boolean isObject(ObjectKind kind, int name);

    int generateLists(int range);

    void deleteLists(int first, int range);

    void setListBase(int base);

    void callLists(int... offsets);

    int takeError();

    List<String> providerCalls();
}

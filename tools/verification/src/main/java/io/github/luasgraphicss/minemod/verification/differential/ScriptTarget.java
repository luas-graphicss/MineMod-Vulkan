package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.Optional;

public interface ScriptTarget {
    int generate(ObjectKind kind);

    void delete(ObjectKind kind, int name);

    boolean isObject(ObjectKind kind, int name);

    void call(String function, long... arguments);

    Optional<String> readPixel();

    int takeError();

    String observedName(int slot, int name);

    int unusedName(int index);
}

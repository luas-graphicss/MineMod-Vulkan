package io.github.luasgraphicss.minemod.compatibility.translation.names;

import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.CallDescription;
import io.github.luasgraphicss.minemod.compatibility.dispatch.catalog.NameArgument;
import java.util.Objects;

public final class NameArguments {
    private final ObjectNames names;

    public NameArguments(ObjectNames names) {
        this.names = Objects.requireNonNull(names, "names");
    }

    public boolean translate(CallDescription description, long[] arguments) {
        for (NameArgument argument : description.nameArguments()) {
            int backing = names.backingForUse(argument.kind(), (int) arguments[argument.index()]);
            if (backing == ObjectNames.REJECTED) {
                return false;
            }
            arguments[argument.index()] = backing;
        }
        return true;
    }
}

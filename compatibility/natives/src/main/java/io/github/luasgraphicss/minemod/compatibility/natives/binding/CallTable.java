package io.github.luasgraphicss.minemod.compatibility.natives.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class CallTable {
    private final String[] names;
    private final Map<String, Integer> indices = new HashMap<>();

    private CallTable(String[] names) {
        this.names = names;
        for (int index = 0; index < names.length; index++) {
            indices.put(names[index], index);
        }
    }

    public static CallTable loaded() {
        return Loaded.TABLE;
    }

    public OptionalInt indexOf(String function) {
        Integer index = indices.get(function);
        return index == null ? OptionalInt.empty() : OptionalInt.of(index);
    }

    public String nameOf(int index) {
        return names[index];
    }

    public int size() {
        return names.length;
    }

    private static final class Loaded {
        private static final CallTable TABLE = new CallTable(NativeBinding.callNames());
    }
}

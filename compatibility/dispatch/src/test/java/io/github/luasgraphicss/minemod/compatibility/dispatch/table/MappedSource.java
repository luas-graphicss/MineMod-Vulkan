package io.github.luasgraphicss.minemod.compatibility.dispatch.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MappedSource implements FunctionSource {
    private final Map<String, Long> addresses = new HashMap<>();
    private final List<String> asked = new ArrayList<>();

    MappedSource with(String function, long address) {
        addresses.put(function, address);
        return this;
    }

    @Override
    public long addressOf(String function) {
        asked.add(function);
        Long address = addresses.get(function);
        return address == null ? 0 : address;
    }

    List<String> asked() {
        return asked;
    }
}

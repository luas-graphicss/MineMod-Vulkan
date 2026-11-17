package io.github.luasgraphicss.minemod.verification.ecosystem.session;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class ResourceLedger {
    private final Map<String, Integer> live = new TreeMap<>();

    public void create(String resource) {
        live.merge(resource, 1, Integer::sum);
    }

    public void destroy(String resource) {
        if (live.merge(resource, -1, Integer::sum) == 0) {
            live.remove(resource);
        }
    }

    public Map<String, Integer> live() {
        return Collections.unmodifiableMap(new TreeMap<>(live));
    }
}

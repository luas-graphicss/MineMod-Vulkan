package io.github.luasgraphicss.minemod.client.shaders;

import io.github.luasgraphicss.minemod.codec.scene.BatchState;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ProgramCatalog {
    private final Map<String, Integer> ids = new HashMap<>();
    private Set<String> published = Collections.emptySet();

    public synchronized int idOf(String shader) {
        Integer known = ids.get(shader);
        if (known != null) {
            return known;
        }
        int assigned = ids.size() + 1;
        ids.put(shader, assigned);
        return assigned;
    }

    public synchronized void publish(Set<String> shaders) {
        published = new HashSet<>(shaders);
    }

    public synchronized int programOf(String shader) {
        return published.contains(shader) ? ids.get(shader) : BatchState.BUILT_IN_PROGRAM;
    }

    public synchronized int publishedCount() {
        return published.size();
    }
}

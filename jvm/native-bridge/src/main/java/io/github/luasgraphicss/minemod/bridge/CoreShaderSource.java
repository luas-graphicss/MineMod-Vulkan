package io.github.luasgraphicss.minemod.bridge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CoreShaderSource {
    private final String name;
    private final String vertex;
    private final String fragment;
    private final Map<String, String> includes;

    public CoreShaderSource(String name, String vertex, String fragment, Map<String, String> includes) {
        this.name = Objects.requireNonNull(name, "name");
        this.vertex = Objects.requireNonNull(vertex, "vertex");
        this.fragment = Objects.requireNonNull(fragment, "fragment");
        this.includes = Collections.unmodifiableMap(new LinkedHashMap<>(includes));
    }

    public String name() {
        return name;
    }

    public String vertex() {
        return vertex;
    }

    public String fragment() {
        return fragment;
    }

    public Map<String, String> includes() {
        return includes;
    }
}

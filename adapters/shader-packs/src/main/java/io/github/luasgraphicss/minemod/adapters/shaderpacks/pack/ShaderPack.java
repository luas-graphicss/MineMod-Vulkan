package io.github.luasgraphicss.minemod.adapters.shaderpacks.pack;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class ShaderPack {
    private final String name;
    private final Map<String, ProgramSources> programs;
    private final Map<String, String> properties;

    public ShaderPack(String name, Map<String, ProgramSources> programs, Map<String, String> properties) {
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("a shader pack has a name");
        }
        this.name = name;
        this.programs = Collections.unmodifiableMap(new TreeMap<>(programs));
        this.properties = Collections.unmodifiableMap(new TreeMap<>(properties));
    }

    public String name() {
        return name;
    }

    public Map<String, ProgramSources> programs() {
        return programs;
    }

    public Map<String, String> properties() {
        return properties;
    }
}

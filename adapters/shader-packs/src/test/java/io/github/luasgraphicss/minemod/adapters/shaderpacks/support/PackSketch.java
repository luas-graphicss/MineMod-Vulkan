package io.github.luasgraphicss.minemod.adapters.shaderpacks.support;

import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ProgramSources;
import io.github.luasgraphicss.minemod.adapters.shaderpacks.pack.ShaderPack;
import java.util.Map;
import java.util.TreeMap;

public final class PackSketch {
    private final Map<String, ProgramSources> programs = new TreeMap<>();
    private final Map<String, String> properties = new TreeMap<>();

    public PackSketch fragment(String program, String... lines) {
        programs.put(program, new ProgramSources("", String.join("\n", lines), 0, false));
        return this;
    }

    public PackSketch computing(String program, int computeShaders) {
        programs.put(program, new ProgramSources("", "", computeShaders, false));
        return this;
    }

    public PackSketch withGeometryShader(String program) {
        programs.put(program, new ProgramSources("", "", 0, true));
        return this;
    }

    public PackSketch property(String key, String value) {
        properties.put(key, value);
        return this;
    }

    public ShaderPack build() {
        return new ShaderPack("sketch", programs, properties);
    }
}

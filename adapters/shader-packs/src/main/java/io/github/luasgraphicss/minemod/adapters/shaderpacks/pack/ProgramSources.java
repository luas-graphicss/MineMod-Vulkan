package io.github.luasgraphicss.minemod.adapters.shaderpacks.pack;

public final class ProgramSources {
    private final String vertex;
    private final String fragment;
    private final int computeShaders;
    private final boolean geometryShader;

    public ProgramSources(String vertex, String fragment, int computeShaders, boolean geometryShader) {
        if (computeShaders < 0) {
            throw new IllegalArgumentException("a program has no negative count of compute shaders");
        }
        this.vertex = vertex;
        this.fragment = fragment;
        this.computeShaders = computeShaders;
        this.geometryShader = geometryShader;
    }

    public String vertex() {
        return vertex;
    }

    public String fragment() {
        return fragment;
    }

    public int computeShaders() {
        return computeShaders;
    }

    public boolean geometryShader() {
        return geometryShader;
    }
}

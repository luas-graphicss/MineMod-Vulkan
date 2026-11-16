package io.github.luasgraphicss.minemod.client.shaders;

import java.util.Objects;

public final class ProgramStages {
    private final String name;
    private final String vertexStage;
    private final String fragmentStage;
    private final boolean isRedefined;

    public ProgramStages(String name, String vertexStage, String fragmentStage, boolean isRedefined) {
        this.name = Objects.requireNonNull(name, "name");
        this.vertexStage = Objects.requireNonNull(vertexStage, "vertexStage");
        this.fragmentStage = Objects.requireNonNull(fragmentStage, "fragmentStage");
        this.isRedefined = isRedefined;
    }

    public String name() {
        return name;
    }

    public String vertexStage() {
        return vertexStage;
    }

    public String fragmentStage() {
        return fragmentStage;
    }

    public boolean isRedefined() {
        return isRedefined;
    }
}

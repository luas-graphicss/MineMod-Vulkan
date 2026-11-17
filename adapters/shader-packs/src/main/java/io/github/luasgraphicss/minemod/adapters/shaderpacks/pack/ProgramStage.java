package io.github.luasgraphicss.minemod.adapters.shaderpacks.pack;

public enum ProgramStage {
    SHADOW,
    SHADOW_COMPOSITE,
    PREPARE,
    OPAQUE_GEOMETRY,
    DEFERRED,
    TRANSLUCENT_GEOMETRY,
    COMPOSITE,
    FINAL;

    public boolean drawsFullscreen() {
        return this != SHADOW && this != OPAQUE_GEOMETRY && this != TRANSLUCENT_GEOMETRY;
    }
}

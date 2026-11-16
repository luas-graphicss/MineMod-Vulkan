package io.github.luasgraphicss.minemod.adapters.ownership.stage;

public enum PipelineStage {
    PRESENTATION,
    FRAME_SUBMISSION,
    TERRAIN_GEOMETRY,
    TERRAIN_DRAW,
    ENTITY_DRAW,
    SHADER_PIPELINE;

    public boolean acceptsBridge() {
        return this != PRESENTATION && this != FRAME_SUBMISSION;
    }
}

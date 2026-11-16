package io.github.luasgraphicss.minemod.adapters.ownership.claim;

import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.Objects;

public final class StageClaim {
    private final RendererIdentity renderer;
    private final PipelineStage stage;

    public StageClaim(RendererIdentity renderer, PipelineStage stage) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.stage = Objects.requireNonNull(stage, "stage");
    }

    public RendererIdentity renderer() {
        return renderer;
    }

    public PipelineStage stage() {
        return stage;
    }
}

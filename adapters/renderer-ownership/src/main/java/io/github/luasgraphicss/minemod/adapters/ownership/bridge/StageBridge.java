package io.github.luasgraphicss.minemod.adapters.ownership.bridge;

import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.Objects;

public final class StageBridge {
    private final RendererIdentity renderer;
    private final PipelineStage stage;

    public StageBridge(RendererIdentity renderer, PipelineStage stage) {
        if (!stage.acceptsBridge()) {
            throw new IllegalArgumentException(stage + " belongs to the native renderer and cannot be bridged");
        }
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.stage = stage;
    }

    public RendererIdentity renderer() {
        return renderer;
    }

    public PipelineStage stage() {
        return stage;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StageBridge)) {
            return false;
        }
        StageBridge bridge = (StageBridge) other;
        return renderer.equals(bridge.renderer) && stage == bridge.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(renderer, stage);
    }
}

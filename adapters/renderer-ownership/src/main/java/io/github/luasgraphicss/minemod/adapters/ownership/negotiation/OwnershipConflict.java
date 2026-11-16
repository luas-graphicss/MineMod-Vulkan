package io.github.luasgraphicss.minemod.adapters.ownership.negotiation;

import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OwnershipConflict {
    public enum Reason {
        STAGE_NOT_SHAREABLE("belongs to the native renderer alone"),
        CONTESTED_BY_REPLACEMENTS("is replaced by more than one renderer"),
        NO_BRIDGE_FOR_VERSION("has no bridge for this exact renderer version");

        private final String description;

        Reason(String description) {
            this.description = description;
        }
    }

    private final PipelineStage stage;
    private final List<RendererIdentity> renderers;
    private final Reason reason;

    OwnershipConflict(PipelineStage stage, List<RendererIdentity> renderers, Reason reason) {
        this.stage = stage;
        this.renderers = Collections.unmodifiableList(new ArrayList<>(renderers));
        this.reason = reason;
    }

    public PipelineStage stage() {
        return stage;
    }

    public List<RendererIdentity> renderers() {
        return renderers;
    }

    public Reason reason() {
        return reason;
    }

    @Override
    public String toString() {
        return stage + " " + reason.description + ", claimed by " + renderers;
    }
}

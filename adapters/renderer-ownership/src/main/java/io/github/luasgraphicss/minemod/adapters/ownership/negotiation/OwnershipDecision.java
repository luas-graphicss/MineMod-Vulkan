package io.github.luasgraphicss.minemod.adapters.ownership.negotiation;

import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class OwnershipDecision {
    private final Map<PipelineStage, RendererIdentity> bridgedStages;
    private final List<OwnershipConflict> conflicts;

    OwnershipDecision(Map<PipelineStage, RendererIdentity> bridgedStages, List<OwnershipConflict> conflicts) {
        this.bridgedStages = Collections.unmodifiableMap(bridgedStages);
        this.conflicts = Collections.unmodifiableList(conflicts);
    }

    public Map<PipelineStage, RendererIdentity> bridgedStages() {
        return bridgedStages;
    }

    public List<OwnershipConflict> conflicts() {
        return conflicts;
    }

    public boolean allowsWorldLoad() {
        return conflicts.isEmpty();
    }
}

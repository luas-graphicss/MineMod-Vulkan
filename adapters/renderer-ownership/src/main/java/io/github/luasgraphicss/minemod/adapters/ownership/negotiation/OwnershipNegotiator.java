package io.github.luasgraphicss.minemod.adapters.ownership.negotiation;

import io.github.luasgraphicss.minemod.adapters.ownership.bridge.StageBridge;
import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.claim.StageClaim;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipConflict.Reason;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OwnershipNegotiator {
    private final Set<StageBridge> bridges;

    public OwnershipNegotiator(Collection<StageBridge> bridges) {
        this.bridges = new HashSet<>(bridges);
    }

    public OwnershipDecision negotiate(List<StageClaim> claims) {
        Map<PipelineStage, Set<RendererIdentity>> claimantsByStage = new EnumMap<>(PipelineStage.class);
        for (StageClaim claim : claims) {
            claimantsByStage
                    .computeIfAbsent(claim.stage(), stage -> new LinkedHashSet<>())
                    .add(claim.renderer());
        }

        Map<PipelineStage, RendererIdentity> bridgedStages = new EnumMap<>(PipelineStage.class);
        List<OwnershipConflict> conflicts = new ArrayList<>();
        for (Map.Entry<PipelineStage, Set<RendererIdentity>> claimed : claimantsByStage.entrySet()) {
            PipelineStage stage = claimed.getKey();
            List<RendererIdentity> claimants = new ArrayList<>(claimed.getValue());
            Optional<Reason> refusal = refusalOf(stage, claimants);
            if (refusal.isPresent()) {
                conflicts.add(new OwnershipConflict(stage, claimants, refusal.get()));
            } else {
                bridgedStages.put(stage, claimants.get(0));
            }
        }
        return new OwnershipDecision(bridgedStages, conflicts);
    }

    private Optional<Reason> refusalOf(PipelineStage stage, List<RendererIdentity> claimants) {
        if (!stage.acceptsBridge()) {
            return Optional.of(Reason.STAGE_NOT_SHAREABLE);
        }
        if (claimants.size() > 1) {
            return Optional.of(Reason.CONTESTED_BY_REPLACEMENTS);
        }
        if (!bridges.contains(new StageBridge(claimants.get(0), stage))) {
            return Optional.of(Reason.NO_BRIDGE_FOR_VERSION);
        }
        return Optional.empty();
    }
}

package io.github.luasgraphicss.minemod.adapters.ownership.negotiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.ownership.bridge.StageBridge;
import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.claim.StageClaim;
import io.github.luasgraphicss.minemod.adapters.ownership.negotiation.OwnershipConflict.Reason;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OwnershipNegotiatorTest {
    private static final RendererIdentity CHUNK_RENDERER = new RendererIdentity("chunk-renderer", "2.1.0");
    private static final RendererIdentity NEWER_CHUNK_RENDERER = new RendererIdentity("chunk-renderer", "2.2.0");
    private static final RendererIdentity SHADER_LOADER = new RendererIdentity("shader-loader", "1.7.3");

    private final OwnershipNegotiator negotiator = new OwnershipNegotiator(Arrays.asList(
            new StageBridge(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY),
            new StageBridge(SHADER_LOADER, PipelineStage.TERRAIN_GEOMETRY)));

    @Test
    void allowsTheWorldWhenNoRendererClaimsAStage() {
        OwnershipDecision decision = negotiator.negotiate(Collections.<StageClaim>emptyList());

        assertTrue(decision.allowsWorldLoad());
        assertTrue(decision.bridgedStages().isEmpty());
    }

    @Test
    void bridgesAStageForTheExactRendererVersion() {
        OwnershipDecision decision = negotiator.negotiate(
                Collections.singletonList(new StageClaim(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY)));

        assertTrue(decision.allowsWorldLoad());
        assertEquals(
                Collections.singletonMap(PipelineStage.TERRAIN_GEOMETRY, CHUNK_RENDERER), decision.bridgedStages());
    }

    @Test
    void refusesARendererVersionNoBridgeNames() {
        OwnershipDecision decision = negotiator.negotiate(
                Collections.singletonList(new StageClaim(NEWER_CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY)));

        assertFalse(decision.allowsWorldLoad());
        assertEquals(Reason.NO_BRIDGE_FOR_VERSION, decision.conflicts().get(0).reason());
        assertEquals(
                "TERRAIN_GEOMETRY has no bridge for this exact renderer version, claimed by [chunk-renderer 2.2.0]",
                decision.conflicts().get(0).toString());
    }

    @Test
    void refusesAClaimOverPresentation() {
        OwnershipDecision decision = negotiator.negotiate(
                Collections.singletonList(new StageClaim(CHUNK_RENDERER, PipelineStage.PRESENTATION)));

        assertFalse(decision.allowsWorldLoad());
        assertEquals(Reason.STAGE_NOT_SHAREABLE, decision.conflicts().get(0).reason());
    }

    @Test
    void refusesTwoRenderersReplacingTheSameStageEvenWhenBothHaveBridges() {
        OwnershipDecision decision = negotiator.negotiate(Arrays.asList(
                new StageClaim(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY),
                new StageClaim(SHADER_LOADER, PipelineStage.TERRAIN_GEOMETRY)));

        assertFalse(decision.allowsWorldLoad());
        assertTrue(decision.bridgedStages().isEmpty());
        assertEquals(Reason.CONTESTED_BY_REPLACEMENTS, decision.conflicts().get(0).reason());
        assertEquals(Arrays.asList(CHUNK_RENDERER, SHADER_LOADER), decision.conflicts().get(0).renderers());
    }

    @Test
    void countsARepeatedClaimOnce() {
        StageClaim claim = new StageClaim(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY);

        assertTrue(negotiator.negotiate(Arrays.asList(claim, claim)).allowsWorldLoad());
    }

    @Test
    void settlesEveryClaimedStageAsBridgedOrConflicting() {
        OwnershipDecision decision = negotiator.negotiate(Arrays.asList(
                new StageClaim(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY),
                new StageClaim(CHUNK_RENDERER, PipelineStage.TERRAIN_DRAW),
                new StageClaim(SHADER_LOADER, PipelineStage.SHADER_PIPELINE)));

        Set<PipelineStage> settled = EnumSet.copyOf(decision.bridgedStages().keySet());
        for (OwnershipConflict conflict : decision.conflicts()) {
            settled.add(conflict.stage());
        }

        assertEquals(
                EnumSet.of(PipelineStage.TERRAIN_GEOMETRY, PipelineStage.TERRAIN_DRAW, PipelineStage.SHADER_PIPELINE),
                settled);
        assertEquals(2, decision.conflicts().size());
    }
}

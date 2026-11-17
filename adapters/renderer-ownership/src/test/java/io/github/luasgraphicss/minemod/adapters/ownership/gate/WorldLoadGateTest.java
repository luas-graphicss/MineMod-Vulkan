package io.github.luasgraphicss.minemod.adapters.ownership.gate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.ownership.bridge.StageBridge;
import io.github.luasgraphicss.minemod.adapters.ownership.catalog.RendererCatalog;
import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorldLoadGateTest {
    private static final RendererIdentity CHUNK_RENDERER = new RendererIdentity("chunk-renderer", "2.1.0");
    private static final List<StageBridge> NO_BRIDGES = Collections.<StageBridge>emptyList();

    private final RendererCatalog catalog = new RendererCatalog(families());

    @Test
    void opensWhenNoInstalledModReplacesAStage() {
        WorldLoadGate gate =
                WorldLoadGate.forInstalledMods(catalog, NO_BRIDGES, Collections.singletonMap("recipe-viewer", "17.0.0"));

        assertTrue(gate.diagnosis().isEmpty());
        assertDoesNotThrow(gate::requireOpen);
    }

    @Test
    void opensWhenEveryStageOfTheExactVersionIsBridged() {
        WorldLoadGate gate = WorldLoadGate.forInstalledMods(
                catalog,
                Arrays.asList(
                        new StageBridge(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY),
                        new StageBridge(CHUNK_RENDERER, PipelineStage.TERRAIN_DRAW)),
                Collections.singletonMap("chunk-renderer", "2.1.0"));

        assertDoesNotThrow(gate::requireOpen);
        assertEquals(
                EnumSet.of(PipelineStage.TERRAIN_GEOMETRY, PipelineStage.TERRAIN_DRAW),
                gate.decision().bridgedStages().keySet());
    }

    @Test
    void staysClosedForARendererVersionWithoutABridge() {
        WorldLoadGate gate = WorldLoadGate.forInstalledMods(
                catalog,
                Collections.singletonList(new StageBridge(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY)),
                Collections.singletonMap("chunk-renderer", "2.2.0"));

        RendererConflictException refusal = assertThrows(RendererConflictException.class, gate::requireOpen);

        assertEquals(
                Arrays.asList(
                        "TERRAIN_GEOMETRY has no bridge for this exact renderer version, claimed by [chunk-renderer 2.2.0]",
                        "TERRAIN_DRAW has no bridge for this exact renderer version, claimed by [chunk-renderer 2.2.0]"),
                refusal.diagnosis());
        assertTrue(refusal.getMessage().contains("neither is switched off for you"));
    }

    @Test
    void staysClosedWhenTwoRenderersReplaceTheSameStage() {
        Map<String, String> installed = new HashMap<>();
        installed.put("chunk-renderer", "2.1.0");
        installed.put("shader-loader", "1.7.3");

        WorldLoadGate gate = WorldLoadGate.forInstalledMods(catalog, NO_BRIDGES, installed);

        assertTrue(gate.diagnosis()
                .contains("TERRAIN_DRAW is replaced by more than one renderer, "
                        + "claimed by [chunk-renderer 2.1.0, shader-loader 1.7.3]"));
    }

    @Test
    void staysClosedForARendererThatPresentsTheFrame() {
        WorldLoadGate gate = WorldLoadGate.forInstalledMods(
                catalog, NO_BRIDGES, Collections.singletonMap("own-swapchain", "0.5.3"));

        assertEquals(
                Collections.singletonList(
                        "PRESENTATION belongs to the native renderer alone, claimed by [own-swapchain 0.5.3]"),
                gate.diagnosis());
    }

    private static Map<String, Set<PipelineStage>> families() {
        Map<String, Set<PipelineStage>> families = new HashMap<>();
        families.put("chunk-renderer", EnumSet.of(PipelineStage.TERRAIN_GEOMETRY, PipelineStage.TERRAIN_DRAW));
        families.put("shader-loader", EnumSet.of(PipelineStage.TERRAIN_DRAW, PipelineStage.SHADER_PIPELINE));
        families.put("own-swapchain", EnumSet.of(PipelineStage.PRESENTATION));
        return families;
    }
}

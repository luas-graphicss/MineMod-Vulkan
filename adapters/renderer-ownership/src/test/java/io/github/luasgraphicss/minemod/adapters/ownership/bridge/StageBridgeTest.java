package io.github.luasgraphicss.minemod.adapters.ownership.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.luasgraphicss.minemod.adapters.ownership.claim.RendererIdentity;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import org.junit.jupiter.api.Test;

class StageBridgeTest {
    private static final RendererIdentity CHUNK_RENDERER = new RendererIdentity("chunk-renderer", "2.1.0");

    @Test
    void rejectsABridgeOverTheStagesOfTheNativeRenderer() {
        assertThrows(
                IllegalArgumentException.class, () -> new StageBridge(CHUNK_RENDERER, PipelineStage.PRESENTATION));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StageBridge(CHUNK_RENDERER, PipelineStage.FRAME_SUBMISSION));
    }

    @Test
    void coversOneExactRendererVersion() {
        StageBridge bridge = new StageBridge(CHUNK_RENDERER, PipelineStage.TERRAIN_GEOMETRY);

        assertEquals(bridge, new StageBridge(new RendererIdentity("chunk-renderer", "2.1.0"), bridge.stage()));
        assertNotEquals(bridge, new StageBridge(new RendererIdentity("chunk-renderer", "2.1.1"), bridge.stage()));
        assertNotEquals(bridge, new StageBridge(CHUNK_RENDERER, PipelineStage.TERRAIN_DRAW));
    }

    @Test
    void rejectsARendererWithoutAnExactVersion() {
        assertThrows(IllegalArgumentException.class, () -> new RendererIdentity("chunk-renderer", " "));
    }
}

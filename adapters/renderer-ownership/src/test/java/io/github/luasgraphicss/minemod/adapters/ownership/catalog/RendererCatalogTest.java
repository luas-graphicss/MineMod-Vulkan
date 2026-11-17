package io.github.luasgraphicss.minemod.adapters.ownership.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.luasgraphicss.minemod.adapters.ownership.claim.StageClaim;
import io.github.luasgraphicss.minemod.adapters.ownership.stage.PipelineStage;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RendererCatalogTest {
    @Test
    void theBundledFamiliesNameTheStagesEachRendererReplaces() {
        RendererCatalog catalog = RendererCatalog.bundled();

        assertTrue(catalog.rendererIds().contains("sodium"));
        assertEquals(
                EnumSet.of(PipelineStage.TERRAIN_GEOMETRY, PipelineStage.TERRAIN_DRAW),
                stagesClaimedBy(catalog, "sodium", "0.6.0"));
        assertEquals(EnumSet.of(PipelineStage.SHADER_PIPELINE), stagesClaimedBy(catalog, "iris", "1.8.0"));
        assertTrue(stagesClaimedBy(catalog, "vulkanmod", "0.5.3").contains(PipelineStage.PRESENTATION));
    }

    @Test
    void aModOutsideTheCatalogClaimsNothing() {
        RendererCatalog catalog = RendererCatalog.bundled();

        assertTrue(catalog.claimsOf(Collections.singletonMap("recipe-viewer", "17.0.0")).isEmpty());
    }

    @Test
    void aClaimCarriesTheExactInstalledVersion() {
        RendererCatalog catalog = new RendererCatalog(
                Collections.singletonMap("chunk-renderer", EnumSet.of(PipelineStage.TERRAIN_DRAW)));
        Map<String, String> installed = new HashMap<>();
        installed.put("chunk-renderer", "2.1.0+build.4");
        installed.put("recipe-viewer", "17.0.0");

        List<StageClaim> claims = catalog.claimsOf(installed);

        assertEquals(1, claims.size());
        assertEquals("chunk-renderer 2.1.0+build.4", claims.get(0).renderer().toString());
        assertEquals(PipelineStage.TERRAIN_DRAW, claims.get(0).stage());
    }

    @Test
    void aCatalogGrowsWithoutChangingTheOneItCameFrom() {
        RendererCatalog bundled = RendererCatalog.bundled();

        RendererCatalog extended = bundled.with("voxel-renderer", EnumSet.of(PipelineStage.TERRAIN_DRAW));

        assertTrue(extended.rendererIds().contains("voxel-renderer"));
        assertTrue(extended.rendererIds().contains("sodium"));
        assertTrue(bundled.claimsOf(Collections.singletonMap("voxel-renderer", "3.0.0")).isEmpty());
    }

    @Test
    void refusesARendererThatReplacesNoStage() {
        Map<String, Set<PipelineStage>> families =
                Collections.<String, Set<PipelineStage>>singletonMap("idle", EnumSet.noneOf(PipelineStage.class));

        assertThrows(IllegalArgumentException.class, () -> new RendererCatalog(families));
    }

    private static Set<PipelineStage> stagesClaimedBy(RendererCatalog catalog, String modId, String version) {
        Set<PipelineStage> stages = EnumSet.noneOf(PipelineStage.class);
        for (StageClaim claim : catalog.claimsOf(Collections.singletonMap(modId, version))) {
            stages.add(claim.stage());
        }
        return stages;
    }
}

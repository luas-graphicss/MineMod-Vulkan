package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.bootstrap.ClientRendering;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.DiffuseLights;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FogSettings;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.TextureUnits;
import io.github.luasgraphicss.minemod.client.draw.frame.ClipSpace;
import io.github.luasgraphicss.minemod.client.draw.frame.GameView;
import io.github.luasgraphicss.minemod.client.lightmap.LightmapPublisher;
import io.github.luasgraphicss.minemod.client.overlay.OverlayPublisher;

final class SceneViews {
    private SceneViews() {}

    static GameView seenThrough(FixedPipeline pipeline, VertexFormat format, int mode, float[] modelView) {
        GameView.Builder view =
                GameView.builder(ClipSpace.fromOpenGl(pipeline.matrices().projection().current()));
        FogSettings fog = pipeline.fog();
        if (fog.isEnabled()) {
            view.fog(fog.color(), fog.start(), fog.end(), FogShapes.current());
        }
        if (PrimitiveModes.drawsLines(mode)) {
            return view.build();
        }
        if (FixedShading.lights(pipeline, format)) {
            DiffuseLights lights = pipeline.lights();
            view.lights(lights.directionSeenFrom(modelView, 0), lights.directionSeenFrom(modelView, 1));
        }
        LightmapPublisher lightmap = ClientRendering.renderer().lightmap();
        if (FixedShading.looksUp(pipeline, format, TextureUnits.LIGHTMAP) && lightmap.isDefined()) {
            view.lightmap(lightmap.texture());
        }
        OverlayPublisher overlay = ClientRendering.renderer().overlay();
        if (FixedShading.looksUp(pipeline, format, TextureUnits.OVERLAY) && overlay.isDefined()) {
            view.overlay(overlay.texture());
        }
        return view.build();
    }
}

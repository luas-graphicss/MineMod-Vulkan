package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.FixedPipeline;
import io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction.TextureUnits;
import io.github.luasgraphicss.minemod.codec.scene.Shading;
import java.util.EnumSet;
import java.util.Set;

final class FixedShading {
    private FixedShading() {}

    static Set<Shading> of(FixedPipeline pipeline, VertexFormat format, int mode) {
        Set<Shading> steps = EnumSet.noneOf(Shading.class);
        if (pipeline.fog().isEnabled()) {
            steps.add(Shading.FOG);
        }
        if (PrimitiveModes.drawsLines(mode)) {
            steps.add(Shading.WIDE_LINES);
            return steps;
        }
        if (samples(pipeline, format)) {
            steps.add(Shading.TEXTURE);
        }
        if (looksUpTheLightmap(pipeline, format)) {
            steps.add(Shading.LIGHTMAP);
        }
        if (lights(pipeline, format)) {
            steps.add(Shading.NORMAL);
        }
        return steps;
    }

    static boolean samples(FixedPipeline pipeline, VertexFormat format) {
        TextureUnits units = pipeline.units();
        return units.isEnabled(TextureUnits.SAMPLED)
                && !units.generatesCoordinates(TextureUnits.SAMPLED)
                && VertexLayouts.stores(format, DefaultVertexFormat.ELEMENT_UV0);
    }

    static boolean looksUpTheLightmap(FixedPipeline pipeline, VertexFormat format) {
        return pipeline.units().isEnabled(TextureUnits.LIGHTMAP)
                && VertexLayouts.stores(format, DefaultVertexFormat.ELEMENT_UV1);
    }

    static boolean isLitEvenly(FixedPipeline pipeline, VertexFormat format) {
        return pipeline.units().isEnabled(TextureUnits.LIGHTMAP)
                && !VertexLayouts.stores(format, DefaultVertexFormat.ELEMENT_UV1);
    }

    static boolean lights(FixedPipeline pipeline, VertexFormat format) {
        return pipeline.lights().areShining() && VertexLayouts.stores(format, DefaultVertexFormat.ELEMENT_NORMAL);
    }
}

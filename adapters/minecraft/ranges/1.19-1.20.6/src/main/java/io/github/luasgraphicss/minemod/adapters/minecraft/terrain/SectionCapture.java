package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.client.terrain.SectionGeometry;
import io.github.luasgraphicss.minemod.client.terrain.SectionLayer;
import io.github.luasgraphicss.minemod.client.terrain.SectionMeshAssembler;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;

public final class SectionCapture {
    private SectionCapture() {}

    public static SectionGeometry geometryOf(Map<RenderType, BufferBuilder.RenderedBuffer> compiledLayers) {
        SectionMeshAssembler assembler = new SectionMeshAssembler();
        add(assembler, SectionLayer.SOLID, compiledLayers.get(RenderType.solid()));
        add(assembler, SectionLayer.CUTOUT_MIPPED, compiledLayers.get(RenderType.cutoutMipped()));
        add(assembler, SectionLayer.CUTOUT, compiledLayers.get(RenderType.cutout()));
        add(assembler, SectionLayer.TRANSLUCENT, compiledLayers.get(RenderType.translucent()));
        add(assembler, SectionLayer.TRANSLUCENT, compiledLayers.get(RenderType.tripwire()));
        return assembler.assemble();
    }

    private static void add(
            SectionMeshAssembler assembler, SectionLayer layer, BufferBuilder.RenderedBuffer compiled) {
        if (compiled == null) {
            return;
        }
        BufferBuilder.DrawState state = compiled.drawState();
        if (state.mode() != VertexFormat.Mode.QUADS || !state.format().equals(DefaultVertexFormat.BLOCK)) {
            throw new IllegalStateException(
                    "section layers are quads in the block vertex format, found " + state.mode() + " in "
                            + state.format());
        }
        assembler.add(layer, CompiledLayer.quadsOf(compiled));
    }
}

package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.luasgraphicss.minemod.client.terrain.SectionGeometry;
import io.github.luasgraphicss.minemod.client.terrain.SectionLayer;
import io.github.luasgraphicss.minemod.client.terrain.SectionMeshAssembler;
import java.util.Set;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;

public final class SectionCapture {
    private static final int QUADS = 7;

    private final Set<RenderType> compiledLayers;
    private final ChunkBufferBuilderPack buffers;
    private final SectionMeshAssembler assembler = new SectionMeshAssembler();

    private SectionCapture(Set<RenderType> compiledLayers, ChunkBufferBuilderPack buffers) {
        this.compiledLayers = compiledLayers;
        this.buffers = buffers;
    }

    public static SectionGeometry geometryOf(Set<RenderType> compiledLayers, ChunkBufferBuilderPack buffers) {
        SectionCapture capture = new SectionCapture(compiledLayers, buffers);
        for (RenderType compiledAs : RenderType.chunkBufferLayers()) {
            capture.add(layerOf(compiledAs), compiledAs);
        }
        return capture.assembler.assemble();
    }

    private static SectionLayer layerOf(RenderType compiledAs) {
        if (compiledAs == RenderType.solid()) {
            return SectionLayer.SOLID;
        }
        if (compiledAs == RenderType.cutoutMipped()) {
            return SectionLayer.CUTOUT_MIPPED;
        }
        return compiledAs == RenderType.cutout() ? SectionLayer.CUTOUT : SectionLayer.TRANSLUCENT;
    }

    private void add(SectionLayer layer, RenderType compiledAs) {
        if (!compiledLayers.contains(compiledAs)) {
            return;
        }
        FinishedLayer finished = FinishedLayer.lastOf(buffers.builder(compiledAs));
        BufferBuilder.DrawState state = finished.state();
        if (state.mode() != QUADS || !state.format().equals(DefaultVertexFormat.BLOCK)) {
            throw new IllegalStateException(
                    "section layers are quads in the block vertex format, found mode " + state.mode() + " in "
                            + state.format());
        }
        assembler.add(layer, finished.quads());
    }
}

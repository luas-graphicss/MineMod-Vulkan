package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.luasgraphicss.minemod.client.terrain.SectionGeometry;
import io.github.luasgraphicss.minemod.client.terrain.SectionLayer;
import io.github.luasgraphicss.minemod.client.terrain.SectionMeshAssembler;
import java.util.Set;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;

public final class SectionCapture {
    private final Set<RenderType> compiledLayers;
    private final ChunkBufferBuilderPack buffers;
    private final SectionMeshAssembler assembler = new SectionMeshAssembler();

    private SectionCapture(Set<RenderType> compiledLayers, ChunkBufferBuilderPack buffers) {
        this.compiledLayers = compiledLayers;
        this.buffers = buffers;
    }

    public static SectionGeometry geometryOf(Set<RenderType> compiledLayers, ChunkBufferBuilderPack buffers) {
        SectionCapture capture = new SectionCapture(compiledLayers, buffers);
        capture.add(SectionLayer.SOLID, RenderType.solid());
        capture.add(SectionLayer.CUTOUT_MIPPED, RenderType.cutoutMipped());
        capture.add(SectionLayer.CUTOUT, RenderType.cutout());
        capture.add(SectionLayer.TRANSLUCENT, RenderType.translucent());
        capture.add(SectionLayer.TRANSLUCENT, RenderType.tripwire());
        return capture.assembler.assemble();
    }

    private void add(SectionLayer layer, RenderType compiledAs) {
        if (!compiledLayers.contains(compiledAs)) {
            return;
        }
        FinishedLayer finished = FinishedLayer.lastOf(buffers.builder(compiledAs));
        BufferBuilder.DrawState state = finished.state();
        if (state.mode() != VertexFormat.Mode.QUADS || !state.format().equals(DefaultVertexFormat.BLOCK)) {
            throw new IllegalStateException(
                    "section layers are quads in the block vertex format, found " + state.mode() + " in "
                            + state.format());
        }
        assembler.add(layer, finished.quads());
    }
}

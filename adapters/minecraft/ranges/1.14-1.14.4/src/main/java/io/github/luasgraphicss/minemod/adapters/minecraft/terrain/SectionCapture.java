package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.luasgraphicss.minemod.client.terrain.LayerQuads;
import io.github.luasgraphicss.minemod.client.terrain.SectionGeometry;
import io.github.luasgraphicss.minemod.client.terrain.SectionLayer;
import io.github.luasgraphicss.minemod.client.terrain.SectionMeshAssembler;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.world.level.BlockLayer;

public final class SectionCapture {
    private static final int QUADS = 7;

    private final CompiledChunk compiled;
    private final ChunkBufferBuilderPack buffers;
    private final SectionMeshAssembler assembler = new SectionMeshAssembler();

    private SectionCapture(CompiledChunk compiled, ChunkBufferBuilderPack buffers) {
        this.compiled = compiled;
        this.buffers = buffers;
    }

    public static SectionGeometry geometryOf(CompiledChunk compiled, ChunkBufferBuilderPack buffers) {
        SectionCapture capture = new SectionCapture(compiled, buffers);
        for (BlockLayer compiledAs : BlockLayer.values()) {
            capture.add(layerOf(compiledAs), compiledAs);
        }
        return capture.assembler.assemble();
    }

    private static SectionLayer layerOf(BlockLayer compiledAs) {
        switch (compiledAs) {
            case SOLID:
                return SectionLayer.SOLID;
            case CUTOUT_MIPPED:
                return SectionLayer.CUTOUT_MIPPED;
            case CUTOUT:
                return SectionLayer.CUTOUT;
            default:
                return SectionLayer.TRANSLUCENT;
        }
    }

    private void add(SectionLayer layer, BlockLayer compiledAs) {
        if (!compiled.hasLayer(compiledAs)) {
            return;
        }
        BufferBuilder finished = buffers.builder(compiledAs);
        if (finished.getDrawMode() != QUADS || !finished.getVertexFormat().equals(DefaultVertexFormat.BLOCK)) {
            throw new IllegalStateException("section layers are quads in the block vertex format, found mode "
                    + finished.getDrawMode() + " in " + finished.getVertexFormat());
        }
        int vertexBytes = finished.getVertexFormat().getVertexSize();
        ByteBuffer vertices = finished.getBuffer().duplicate();
        vertices.clear();
        vertices.limit(finished.getVertexCount() * vertexBytes);
        assembler.add(layer, LayerQuads.inBuildOrder(vertices, vertexBytes));
    }
}

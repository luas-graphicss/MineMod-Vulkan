package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.luasgraphicss.minemod.client.terrain.LayerQuads;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class CompiledLayer {
    private static final int UNSIGNED_BYTE_MASK = 0xFF;

    private CompiledLayer() {}

    static LayerQuads quadsOf(BufferBuilder.RenderedBuffer compiled) {
        BufferBuilder.DrawState state = compiled.drawState();
        int vertexStride = state.format().getVertexSize();
        ByteBuffer vertices = compiled.vertexBuffer();
        if (state.sequentialIndex()) {
            return LayerQuads.inBuildOrder(vertices, vertexStride);
        }
        ByteBuffer drawOrder = compiled.indexBuffer();
        int indexBytes = state.indexType().bytes;
        return indexBytes == Byte.BYTES
                ? LayerQuads.inDrawOrder(vertices, vertexStride, widened(drawOrder), Short.BYTES)
                : LayerQuads.inDrawOrder(vertices, vertexStride, drawOrder, indexBytes);
    }

    private static ByteBuffer widened(ByteBuffer narrow) {
        ByteBuffer wide = ByteBuffer.allocate(narrow.remaining() * Short.BYTES).order(ByteOrder.nativeOrder());
        while (narrow.hasRemaining()) {
            wide.putShort((short) (narrow.get() & UNSIGNED_BYTE_MASK));
        }
        wide.flip();
        return wide;
    }
}

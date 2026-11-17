package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain.BufferBuilderAccessor;
import io.github.luasgraphicss.minemod.client.terrain.LayerQuads;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class FinishedLayer {
    private static final int UNSIGNED_BYTE_MASK = 0xFF;

    private final BufferBuilder.DrawState state;
    private final ByteBuffer contents;

    private FinishedLayer(BufferBuilder.DrawState state, ByteBuffer contents) {
        this.state = state;
        this.contents = contents;
    }

    static FinishedLayer lastOf(BufferBuilder builder) {
        BufferBuilderAccessor finished = (BufferBuilderAccessor) builder;
        BufferBuilder.DrawState state = finished.minemod$drawStates().get(finished.minemod$lastPoppedStateIndex());
        int firstByte = finished.minemod$totalUploadedBytes();
        ByteBuffer contents = finished.minemod$buffer().duplicate();
        contents.limit(firstByte + state.bufferSize());
        contents.position(firstByte);
        return new FinishedLayer(state, contents.slice());
    }

    BufferBuilder.DrawState state() {
        return state;
    }

    LayerQuads quads() {
        int vertexStride = state.format().getVertexSize();
        ByteBuffer vertices = part(0, state.vertexBufferSize());
        if (state.sequentialIndex()) {
            return LayerQuads.inBuildOrder(vertices, vertexStride);
        }
        ByteBuffer drawOrder = part(state.vertexBufferSize(), state.bufferSize());
        int indexBytes = state.indexType().bytes;
        return indexBytes == Byte.BYTES
                ? LayerQuads.inDrawOrder(vertices, vertexStride, widened(drawOrder), Short.BYTES)
                : LayerQuads.inDrawOrder(vertices, vertexStride, drawOrder, indexBytes);
    }

    private ByteBuffer part(int firstByte, int endByte) {
        ByteBuffer part = contents.duplicate().order(ByteOrder.nativeOrder());
        part.limit(endByte);
        part.position(firstByte);
        return part;
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

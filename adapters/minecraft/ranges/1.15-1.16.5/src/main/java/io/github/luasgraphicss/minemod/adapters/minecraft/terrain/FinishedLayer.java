package io.github.luasgraphicss.minemod.adapters.minecraft.terrain;

import com.mojang.blaze3d.vertex.BufferBuilder;
import io.github.luasgraphicss.minemod.adapters.minecraft.mixin.terrain.BufferBuilderAccessor;
import io.github.luasgraphicss.minemod.client.terrain.LayerQuads;
import java.nio.ByteBuffer;

final class FinishedLayer {
    private final BufferBuilder.DrawState state;
    private final ByteBuffer vertices;

    private FinishedLayer(BufferBuilder.DrawState state, ByteBuffer vertices) {
        this.state = state;
        this.vertices = vertices;
    }

    static FinishedLayer lastOf(BufferBuilder builder) {
        BufferBuilderAccessor finished = (BufferBuilderAccessor) builder;
        BufferBuilder.DrawState state = finished.minemod$drawStates().get(finished.minemod$lastPoppedStateIndex());
        int firstByte = finished.minemod$totalUploadedBytes();
        ByteBuffer contents = finished.minemod$buffer().duplicate();
        contents.limit(firstByte + state.vertexCount() * state.format().getVertexSize());
        contents.position(firstByte);
        return new FinishedLayer(state, contents.slice());
    }

    BufferBuilder.DrawState state() {
        return state;
    }

    LayerQuads quads() {
        return LayerQuads.inBuildOrder(vertices.duplicate(), state.format().getVertexSize());
    }
}

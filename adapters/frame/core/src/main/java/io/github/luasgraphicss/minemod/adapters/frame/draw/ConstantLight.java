package io.github.luasgraphicss.minemod.adapters.frame.draw;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;

final class ConstantLight {
    private ConstantLight() {}

    static Geometry lit(Geometry geometry, float[] coordinates) {
        int stride = geometry.stride();
        int litStride = stride + VertexElement.LIGHT.bytes();
        ByteBuffer source = geometry.vertices();
        ByteBuffer lit = ByteBuffer.allocateDirect(geometry.vertexCount() * litStride).order(ByteOrder.nativeOrder());
        for (int vertex = 0; vertex < geometry.vertexCount(); vertex++) {
            ByteBuffer one = source.duplicate();
            one.position(source.position() + vertex * stride);
            one.limit(one.position() + stride);
            lit.put(one).putShort((short) coordinates[0]).putShort((short) coordinates[1]);
        }
        lit.flip();
        Map<VertexElement, Integer> offsets = new EnumMap<>(geometry.offsets());
        offsets.put(VertexElement.LIGHT, stride);
        return new Geometry(geometry.mode(), lit, geometry.vertexCount(), litStride, offsets);
    }
}

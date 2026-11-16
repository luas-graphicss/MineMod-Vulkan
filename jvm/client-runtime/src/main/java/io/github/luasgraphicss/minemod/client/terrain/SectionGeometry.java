package io.github.luasgraphicss.minemod.client.terrain;

import io.github.luasgraphicss.minemod.codec.scene.SectionMesh;
import java.nio.ByteBuffer;

public final class SectionGeometry {
    private static final int RANGE_VALUES = SectionMesh.LAYER_COUNT * 2;

    private final ByteBuffer vertices;
    private final ByteBuffer indices;
    private final int[] layerRanges;

    SectionGeometry(ByteBuffer vertices, ByteBuffer indices, int[] layerRanges) {
        this.vertices = vertices;
        this.indices = indices;
        this.layerRanges = layerRanges;
    }

    public static SectionGeometry empty() {
        return new SectionGeometry(ByteBuffer.allocate(0), ByteBuffer.allocate(0), new int[RANGE_VALUES]);
    }

    public boolean isEmpty() {
        return !vertices.hasRemaining();
    }

    public int bytes() {
        return vertices.remaining() + indices.remaining();
    }

    SectionMesh mesh() {
        return new SectionMesh(vertices, indices, layerRanges);
    }
}

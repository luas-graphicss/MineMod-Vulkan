package io.github.luasgraphicss.minemod.codec.scene;

import java.nio.ByteBuffer;

public final class SectionMesh {
    public static final int VERTEX_BYTES = 28;
    public static final int LAYER_COUNT = 4;

    private static final int INDEX_BYTES = 4;
    private static final int RANGE_VALUES = 2;

    private final ByteBuffer vertices;
    private final ByteBuffer indices;
    private final int[] layerRanges;

    public SectionMesh(ByteBuffer vertices, ByteBuffer indices, int[] layerRanges) {
        if (vertices.remaining() == 0 || vertices.remaining() % VERTEX_BYTES != 0) {
            throw new IllegalArgumentException(
                    "section vertices take " + VERTEX_BYTES + " bytes each, found " + vertices.remaining());
        }
        if (indices.remaining() == 0 || indices.remaining() % INDEX_BYTES != 0) {
            throw new IllegalArgumentException(
                    "section indices take " + INDEX_BYTES + " bytes each, found " + indices.remaining());
        }
        if (layerRanges.length != LAYER_COUNT * RANGE_VALUES) {
            throw new IllegalArgumentException(
                    "a section has a first index and an index count for each of its " + LAYER_COUNT + " layers");
        }
        this.vertices = vertices.duplicate();
        this.indices = indices.duplicate();
        this.layerRanges = layerRanges.clone();
    }

    int blobBytes() {
        return vertices.remaining() + indices.remaining();
    }

    void putCounts(ByteBuffer payload) {
        payload.putInt(vertices.remaining() / VERTEX_BYTES).putInt(indices.remaining() / INDEX_BYTES);
        for (int value : layerRanges) {
            payload.putInt(value);
        }
    }

    void putBlob(ByteBuffer payload) {
        payload.put(vertices.duplicate()).put(indices.duplicate());
    }
}

package io.github.luasgraphicss.minemod.client.terrain;

import io.github.luasgraphicss.minemod.codec.scene.SectionMesh;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class LayerQuads {
    private static final int QUAD_VERTICES = 4;
    private static final int[] QUAD_CORNERS = {0, 1, 2, 2, 3, 0};
    private static final int UNSIGNED_SHORT_MASK = 0xFFFF;
    private static final ByteBuffer BUILD_ORDER = ByteBuffer.allocate(0);

    private final ByteBuffer vertices;
    private final int vertexStride;
    private final ByteBuffer drawOrder;
    private final int indexBytes;

    private LayerQuads(ByteBuffer vertices, int vertexStride, ByteBuffer drawOrder, int indexBytes) {
        if (vertexStride < SectionMesh.VERTEX_BYTES) {
            throw new IllegalArgumentException("a terrain vertex starts with " + SectionMesh.VERTEX_BYTES
                    + " bytes of position, color, texture and light, found a stride of " + vertexStride);
        }
        if (vertices.remaining() % (vertexStride * QUAD_VERTICES) != 0) {
            throw new IllegalArgumentException("terrain quads take " + QUAD_VERTICES + " vertices of " + vertexStride
                    + " bytes each, found " + vertices.remaining() + " bytes");
        }
        this.vertices = vertices.duplicate();
        this.vertexStride = vertexStride;
        this.drawOrder = drawOrder.duplicate().order(ByteOrder.nativeOrder());
        this.indexBytes = indexBytes;
    }

    public static LayerQuads inBuildOrder(ByteBuffer vertices, int vertexStride) {
        return new LayerQuads(vertices, vertexStride, BUILD_ORDER, Integer.BYTES);
    }

    public static LayerQuads inDrawOrder(ByteBuffer vertices, int vertexStride, ByteBuffer indices, int indexBytes) {
        if (indexBytes != Short.BYTES && indexBytes != Integer.BYTES) {
            throw new IllegalArgumentException("terrain indices take 2 or 4 bytes each, found " + indexBytes);
        }
        LayerQuads quads = new LayerQuads(vertices, vertexStride, indices, indexBytes);
        if (indices.remaining() != quads.indexCount() * indexBytes) {
            throw new IllegalArgumentException("the draw order of " + quads.vertexCount() + " vertices takes "
                    + quads.indexCount() + " indices of " + indexBytes + " bytes, found " + indices.remaining()
                    + " bytes");
        }
        return quads;
    }

    int vertexCount() {
        return vertices.remaining() / vertexStride;
    }

    int indexCount() {
        return vertexCount() / QUAD_VERTICES * QUAD_CORNERS.length;
    }

    void copyVertices(ByteBuffer target) {
        ByteBuffer source = vertices.duplicate();
        int firstByte = source.position();
        for (int vertex = 0; vertex < vertexCount(); vertex++) {
            int vertexStart = firstByte + vertex * vertexStride;
            source.limit(vertexStart + SectionMesh.VERTEX_BYTES).position(vertexStart);
            target.put(source);
        }
    }

    void appendIndices(ByteBuffer target, int baseVertex) {
        if (!drawOrder.hasRemaining()) {
            appendBuildOrder(target, baseVertex);
            return;
        }
        ByteBuffer order = drawOrder.duplicate().order(ByteOrder.nativeOrder());
        while (order.hasRemaining()) {
            int index = indexBytes == Short.BYTES ? order.getShort() & UNSIGNED_SHORT_MASK : order.getInt();
            if (index < 0 || index >= vertexCount()) {
                throw new IllegalArgumentException(
                        "the draw order names vertex " + index + " of " + vertexCount() + " vertices");
            }
            target.putInt(baseVertex + index);
        }
    }

    private void appendBuildOrder(ByteBuffer target, int baseVertex) {
        for (int quad = 0; quad < vertexCount() / QUAD_VERTICES; quad++) {
            for (int corner : QUAD_CORNERS) {
                target.putInt(baseVertex + quad * QUAD_VERTICES + corner);
            }
        }
    }
}

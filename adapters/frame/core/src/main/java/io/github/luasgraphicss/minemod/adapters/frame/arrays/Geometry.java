package io.github.luasgraphicss.minemod.adapters.frame.arrays;

import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class Geometry {
    private static final int ABSENT = -1;

    private final int mode;
    private final ByteBuffer vertices;
    private final int vertexCount;
    private final int stride;
    private final Map<VertexElement, Integer> offsets;

    public Geometry(int mode, ByteBuffer vertices, int vertexCount, int stride, Map<VertexElement, Integer> offsets) {
        if (!offsets.containsKey(VertexElement.POSITION)) {
            throw new IllegalArgumentException("a vertex needs a position: " + offsets.keySet());
        }
        if (vertexCount < 0 || (long) vertexCount * stride > vertices.remaining()) {
            throw new IllegalArgumentException(vertexCount + " vertices of " + stride + " bytes do not fit in "
                    + vertices.remaining() + " bytes");
        }
        this.mode = mode;
        this.vertices = vertices.duplicate();
        this.vertexCount = vertexCount;
        this.stride = stride;
        this.offsets = Collections.unmodifiableMap(new EnumMap<>(offsets));
    }

    public int mode() {
        return mode;
    }

    public ByteBuffer vertices() {
        return vertices.duplicate().order(ByteOrder.nativeOrder());
    }

    public int vertexCount() {
        return vertexCount;
    }

    public int stride() {
        return stride;
    }

    public Map<VertexElement, Integer> offsets() {
        return offsets;
    }

    public boolean has(VertexElement element) {
        return offsets.containsKey(element);
    }

    public int offsetOf(VertexElement element) {
        Integer offset = offsets.get(element);
        return offset == null ? ABSENT : offset;
    }

    public Geometry copy() {
        ByteBuffer source = vertices();
        source.limit(source.position() + vertexCount * stride);
        ByteBuffer kept = ByteBuffer.allocateDirect(source.remaining()).order(ByteOrder.nativeOrder());
        kept.put(source);
        kept.flip();
        return new Geometry(mode, kept, vertexCount, stride, offsets);
    }

    public GameVertexLayout layout() {
        GameVertexLayout.Builder layout = GameVertexLayout.builder(stride);
        for (Map.Entry<VertexElement, Integer> element : offsets.entrySet()) {
            layout.element(element.getKey(), element.getValue());
        }
        return layout.build();
    }
}

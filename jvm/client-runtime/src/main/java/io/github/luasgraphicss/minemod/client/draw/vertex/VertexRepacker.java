package io.github.luasgraphicss.minemod.client.draw.vertex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class VertexRepacker {
    private static final int WORD_BYTES = 4;

    private VertexRepacker() {}

    public static void pack(GameVertexLayout layout, ByteBuffer vertices, int vertexCount, ByteBuffer packed) {
        long sourceBytes = (long) vertexCount * layout.stride();
        if (vertexCount < 0 || sourceBytes > vertices.remaining()) {
            throw new IllegalArgumentException(vertexCount + " vertices of " + layout.stride()
                    + " bytes do not fit in " + vertices.remaining() + " bytes");
        }
        ByteBuffer source = vertices.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        if (layout.isPacked()) {
            source.limit(source.position() + (int) sourceBytes);
            packed.put(source);
            return;
        }
        ByteBuffer target = packed.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int vertex = source.position();
        for (int index = 0; index < vertexCount; index++) {
            for (int element = 0; element < layout.elementCount(); element++) {
                int from = vertex + layout.sourceOffset(element);
                for (int word = 0; word < layout.bytesOf(element); word += WORD_BYTES) {
                    target.putInt(source.getInt(from + word));
                }
            }
            vertex += layout.stride();
        }
        packed.position(target.position());
    }
}

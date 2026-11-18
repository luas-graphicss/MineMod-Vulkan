package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import io.github.luasgraphicss.minemod.client.draw.vertex.GameVertexLayout;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

final class LineGeometry {
    private static final int VERTEX_BYTES = 20;
    private static final int COLOR_OFFSET = 12;
    private static final int NORMAL_OFFSET = 16;
    private static final int COLOR_BYTES = 4;
    private static final int AXES = 3;
    private static final int COPIES_OF_AN_END = 2;
    private static final int VERTICES_OF_A_LINE = 4;
    private static final int NO_ELEMENT = -1;
    private static final byte OPAQUE_WHITE = (byte) 0xFF;
    private static final float LONGEST_NORMAL_BYTE = 127.0f;
    private static final GameVertexLayout LAYOUT = GameVertexLayout.builder(VERTEX_BYTES)
            .element(VertexElement.POSITION, 0)
            .element(VertexElement.COLOR, COLOR_OFFSET)
            .element(VertexElement.NORMAL, NORMAL_OFFSET)
            .build();

    private LineGeometry() {}

    static GameVertexLayout layout() {
        return LAYOUT;
    }

    static int vertexCountOf(ByteBuffer widened) {
        return widened.remaining() / VERTEX_BYTES;
    }

    static Optional<ByteBuffer> widened(VertexFormat format, int mode, ByteBuffer vertices, int vertexCount) {
        int position = offsetOf(format, DefaultVertexFormat.ELEMENT_POSITION);
        if (position == NO_ELEMENT) {
            return Optional.empty();
        }
        Source source = new Source(
                vertices.duplicate().order(ByteOrder.nativeOrder()),
                format.getVertexSize(),
                position,
                offsetOf(format, DefaultVertexFormat.ELEMENT_COLOR));
        int lines = lineCountOf(mode, vertexCount);
        ByteBuffer widened = ByteBuffer.allocate(lines * VERTICES_OF_A_LINE * VERTEX_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        for (int line = 0; line < lines; line++) {
            int start = mode == PrimitiveModes.LINES ? line * 2 : line;
            int end = (start + 1) % vertexCount;
            byte[] direction = source.directionBetween(start, end);
            source.copy(start, direction, widened);
            source.copy(end, direction, widened);
        }
        widened.flip();
        return Optional.of(widened);
    }

    private static int lineCountOf(int mode, int vertexCount) {
        if (vertexCount < 2) {
            return 0;
        }
        switch (mode) {
            case PrimitiveModes.LINES:
                return vertexCount / 2;
            case PrimitiveModes.LINE_LOOP:
                return vertexCount;
            default:
                return vertexCount - 1;
        }
    }

    private static int offsetOf(VertexFormat format, VertexFormatElement stored) {
        int offset = 0;
        for (VertexFormatElement element : format.getElements()) {
            if (element.equals(stored)) {
                return offset;
            }
            offset += element.getByteSize();
        }
        return NO_ELEMENT;
    }

    private static final class Source {
        private final ByteBuffer vertices;
        private final int stride;
        private final int position;
        private final int color;

        Source(ByteBuffer vertices, int stride, int position, int color) {
            this.vertices = vertices;
            this.stride = stride;
            this.position = position;
            this.color = color;
        }

        byte[] directionBetween(int start, int end) {
            float[] direction = new float[AXES];
            float squaredLength = 0.0f;
            for (int axis = 0; axis < AXES; axis++) {
                direction[axis] = coordinate(end, axis) - coordinate(start, axis);
                squaredLength += direction[axis] * direction[axis];
            }
            float length = (float) Math.sqrt(squaredLength);
            byte[] packed = new byte[AXES];
            for (int axis = 0; axis < AXES && length > 0.0f; axis++) {
                packed[axis] = (byte) Math.round(direction[axis] / length * LONGEST_NORMAL_BYTE);
            }
            return packed;
        }

        void copy(int vertex, byte[] direction, ByteBuffer widened) {
            for (int copy = 0; copy < COPIES_OF_AN_END; copy++) {
                for (int axis = 0; axis < AXES; axis++) {
                    widened.putFloat(coordinate(vertex, axis));
                }
                for (int channel = 0; channel < COLOR_BYTES; channel++) {
                    widened.put(color == NO_ELEMENT ? OPAQUE_WHITE : vertices.get(first(vertex) + color + channel));
                }
                widened.put(direction).put((byte) 0);
            }
        }

        private float coordinate(int vertex, int axis) {
            return vertices.getFloat(first(vertex) + position + axis * Float.BYTES);
        }

        private int first(int vertex) {
            return vertices.position() + vertex * stride;
        }
    }
}

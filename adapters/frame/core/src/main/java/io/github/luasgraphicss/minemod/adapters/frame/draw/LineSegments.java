package io.github.luasgraphicss.minemod.adapters.frame.draw;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;

final class LineSegments {
    private static final int VERTEX_BYTES = 20;
    private static final int COLOR_OFFSET = 12;
    private static final int NORMAL_OFFSET = 16;
    private static final int COLOR_BYTES = 4;
    private static final int AXES = 3;
    private static final int FLOAT_BYTES = 4;
    private static final int COPIES_OF_AN_END = 2;
    private static final int VERTICES_OF_A_LINE = 4;
    private static final byte OPAQUE_WHITE = (byte) 0xFF;
    private static final byte NO_PADDING = 0;
    private static final float LONGEST_NORMAL_BYTE = 127.0f;

    private LineSegments() {}

    static Geometry widened(Geometry lines) {
        int lineCount = lineCountOf(lines.mode(), lines.vertexCount());
        ByteBuffer widened = ByteBuffer.allocateDirect(lineCount * VERTICES_OF_A_LINE * VERTEX_BYTES)
                .order(ByteOrder.nativeOrder());
        ByteBuffer source = lines.vertices();
        for (int line = 0; line < lineCount; line++) {
            int start = lines.mode() == PrimitiveModes.LINES ? line * 2 : line;
            int end = (start + 1) % lines.vertexCount();
            byte[] direction = directionBetween(lines, source, start, end);
            copy(lines, source, start, direction, widened);
            copy(lines, source, end, direction, widened);
        }
        widened.flip();
        Map<VertexElement, Integer> offsets = new EnumMap<>(VertexElement.class);
        offsets.put(VertexElement.POSITION, 0);
        offsets.put(VertexElement.COLOR, COLOR_OFFSET);
        offsets.put(VertexElement.NORMAL, NORMAL_OFFSET);
        return new Geometry(PrimitiveModes.LINES, widened, lineCount * VERTICES_OF_A_LINE, VERTEX_BYTES, offsets);
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

    private static byte[] directionBetween(Geometry lines, ByteBuffer source, int start, int end) {
        float[] direction = new float[AXES];
        float squaredLength = 0.0f;
        for (int axis = 0; axis < AXES; axis++) {
            direction[axis] = coordinate(lines, source, end, axis) - coordinate(lines, source, start, axis);
            squaredLength += direction[axis] * direction[axis];
        }
        byte[] packed = new byte[AXES];
        if (squaredLength > 0.0f) {
            float length = (float) Math.sqrt(squaredLength);
            for (int axis = 0; axis < AXES; axis++) {
                packed[axis] = (byte) Math.round(direction[axis] / length * LONGEST_NORMAL_BYTE);
            }
        }
        return packed;
    }

    private static float coordinate(Geometry lines, ByteBuffer source, int vertex, int axis) {
        int position = lines.offsetOf(VertexElement.POSITION);
        return source.getFloat(source.position() + vertex * lines.stride() + position + axis * FLOAT_BYTES);
    }

    private static void copy(Geometry lines, ByteBuffer source, int vertex, byte[] direction, ByteBuffer widened) {
        int from = source.position() + vertex * lines.stride();
        int color = lines.offsetOf(VertexElement.COLOR);
        for (int copy = 0; copy < COPIES_OF_AN_END; copy++) {
            for (int axis = 0; axis < AXES; axis++) {
                widened.putFloat(coordinate(lines, source, vertex, axis));
            }
            for (int channel = 0; channel < COLOR_BYTES; channel++) {
                widened.put(color < 0 ? OPAQUE_WHITE : source.get(from + color + channel));
            }
            widened.put(direction).put(NO_PADDING);
        }
    }
}

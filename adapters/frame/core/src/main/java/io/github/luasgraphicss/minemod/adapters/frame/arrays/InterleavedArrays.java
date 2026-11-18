package io.github.luasgraphicss.minemod.adapters.frame.arrays;

import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

final class InterleavedArrays {
    private static final int BYTE = 0x1400;
    private static final int UNSIGNED_BYTE = 0x1401;
    private static final int SHORT = 0x1402;
    private static final int FLOAT = 0x1406;
    private static final int POSITION_VALUES = 3;
    private static final int COLOR_VALUES = 4;
    private static final int COORDINATE_VALUES = 2;

    private InterleavedArrays() {}

    static Optional<Geometry> read(
            ArrayPointer positions,
            Map<VertexElement, ArrayPointer> others,
            int mode,
            int first,
            int count,
            ArrayBuffers buffers) {
        if (!holds(positions, VertexElement.POSITION) || first < 0 || count < 0) {
            return Optional.empty();
        }
        int stride = positions.stride() > 0 ? positions.stride() : VertexElement.POSITION.bytes();
        Map<VertexElement, Integer> offsets = new EnumMap<>(VertexElement.class);
        offsets.put(VertexElement.POSITION, 0);
        for (Map.Entry<VertexElement, ArrayPointer> other : others.entrySet()) {
            VertexElement element = other.getKey();
            ArrayPointer pointer = other.getValue();
            long offset = pointer.offset() - positions.offset();
            boolean isInterleaved = pointer.sharesSourceWith(positions)
                    && pointer.stride() == positions.stride()
                    && offset >= 0
                    && offset + element.bytes() <= stride;
            if (!holds(pointer, element) || !isInterleaved) {
                return Optional.empty();
            }
            offsets.put(element, (int) offset);
        }
        Optional<ByteBuffer> source = sourceOf(positions, buffers);
        long start = positions.offset() + (long) first * stride;
        long end = start + (long) count * stride;
        if (!source.isPresent() || start < 0 || end > source.get().limit()) {
            return Optional.empty();
        }
        ByteBuffer vertices = source.get();
        vertices.limit((int) end);
        vertices.position((int) start);
        return Optional.of(new Geometry(mode, vertices, count, stride, offsets));
    }

    private static Optional<ByteBuffer> sourceOf(ArrayPointer positions, ArrayBuffers buffers) {
        if (positions.memory() == null) {
            return buffers.contentsOf(positions.bufferObject());
        }
        ByteBuffer memory = positions.memory().duplicate();
        memory.clear();
        return Optional.of(memory);
    }

    private static boolean holds(ArrayPointer pointer, VertexElement element) {
        switch (element) {
            case POSITION:
                return pointer.size() == POSITION_VALUES && pointer.type() == FLOAT;
            case COLOR:
                return pointer.size() == COLOR_VALUES && pointer.type() == UNSIGNED_BYTE;
            case TEXTURE:
                return pointer.size() == COORDINATE_VALUES && pointer.type() == FLOAT;
            case LIGHT:
                return pointer.size() == COORDINATE_VALUES && pointer.type() == SHORT;
            case NORMAL:
                return pointer.type() == BYTE;
            default:
                return false;
        }
    }
}

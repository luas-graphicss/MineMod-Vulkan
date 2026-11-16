package io.github.luasgraphicss.minemod.client.draw.vertex;

import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class GameVertexLayout {
    private final int stride;
    private final Set<VertexElement> elements;
    private final int[] sourceOffsets;
    private final int[] elementBytes;
    private final int packedStride;
    private final boolean isPacked;

    private GameVertexLayout(int stride, Map<VertexElement, Integer> offsets) {
        this.stride = stride;
        this.elements = Collections.unmodifiableSet(offsets.keySet());
        this.sourceOffsets = new int[offsets.size()];
        this.elementBytes = new int[offsets.size()];
        int packedOffset = 0;
        int index = 0;
        boolean isInPackedOrder = true;
        for (Map.Entry<VertexElement, Integer> element : offsets.entrySet()) {
            sourceOffsets[index] = element.getValue();
            elementBytes[index] = element.getKey().bytes();
            isInPackedOrder &= sourceOffsets[index] == packedOffset;
            packedOffset += elementBytes[index];
            index++;
        }
        this.packedStride = packedOffset;
        this.isPacked = isInPackedOrder && stride == packedOffset;
    }

    public static Builder builder(int stride) {
        if (stride < 1) {
            throw new IllegalArgumentException("a vertex takes at least one byte, found " + stride);
        }
        return new Builder(stride);
    }

    public int stride() {
        return stride;
    }

    public Set<VertexElement> elements() {
        return elements;
    }

    public int packedStride() {
        return packedStride;
    }

    public boolean isPacked() {
        return isPacked;
    }

    int elementCount() {
        return sourceOffsets.length;
    }

    int sourceOffset(int element) {
        return sourceOffsets[element];
    }

    int bytesOf(int element) {
        return elementBytes[element];
    }

    public static final class Builder {
        private final int stride;
        private final Map<VertexElement, Integer> offsets = new EnumMap<>(VertexElement.class);

        private Builder(int stride) {
            this.stride = stride;
        }

        public Builder element(VertexElement element, int offset) {
            if (offset < 0 || offset + element.bytes() > stride) {
                throw new IllegalArgumentException(
                        element + " at byte " + offset + " does not fit a vertex of " + stride + " bytes");
            }
            if (offsets.put(element, offset) != null) {
                throw new IllegalArgumentException("a vertex holds " + element + " once");
            }
            return this;
        }

        public GameVertexLayout build() {
            if (!offsets.containsKey(VertexElement.POSITION)) {
                throw new IllegalArgumentException("a vertex format needs a position: " + offsets.keySet());
            }
            return new GameVertexLayout(stride, new EnumMap<>(offsets));
        }
    }
}

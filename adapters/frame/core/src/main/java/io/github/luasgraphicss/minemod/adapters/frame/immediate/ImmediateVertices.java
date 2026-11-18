package io.github.luasgraphicss.minemod.adapters.frame.immediate;

import io.github.luasgraphicss.minemod.adapters.frame.arrays.Geometry;
import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class ImmediateVertices {
    private static final int VERTEX_BYTES = 20;
    private static final int COORDINATE_OFFSET = 12;
    private static final int FIRST_VERTICES = 256;

    private ByteBuffer vertices = ByteBuffer.allocateDirect(FIRST_VERTICES * VERTEX_BYTES).order(ByteOrder.nativeOrder());
    private boolean isOpen;
    private boolean hasCoordinates;
    private int mode;
    private float horizontal;
    private float vertical;

    public void begin(int primitiveMode) {
        vertices.clear();
        mode = primitiveMode;
        hasCoordinates = false;
        isOpen = true;
    }

    public void coordinates(float newHorizontal, float newVertical) {
        horizontal = newHorizontal;
        vertical = newVertical;
        hasCoordinates |= isOpen;
    }

    public void vertex(float x, float y, float z) {
        if (!isOpen) {
            return;
        }
        if (vertices.remaining() < VERTEX_BYTES) {
            grow();
        }
        vertices.putFloat(x).putFloat(y).putFloat(z).putFloat(horizontal).putFloat(vertical);
    }

    public Optional<Geometry> end() {
        if (!isOpen || vertices.position() == 0) {
            isOpen = false;
            return Optional.empty();
        }
        isOpen = false;
        ByteBuffer drawn = vertices.duplicate();
        drawn.flip();
        Map<VertexElement, Integer> offsets = new EnumMap<>(VertexElement.class);
        offsets.put(VertexElement.POSITION, 0);
        if (hasCoordinates) {
            offsets.put(VertexElement.TEXTURE, COORDINATE_OFFSET);
        }
        return Optional.of(new Geometry(mode, drawn, drawn.remaining() / VERTEX_BYTES, VERTEX_BYTES, offsets));
    }

    private void grow() {
        ByteBuffer larger = ByteBuffer.allocateDirect(2 * vertices.capacity()).order(ByteOrder.nativeOrder());
        vertices.flip();
        larger.put(vertices);
        vertices = larger;
    }
}

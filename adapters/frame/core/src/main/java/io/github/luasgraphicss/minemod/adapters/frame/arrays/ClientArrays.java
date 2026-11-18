package io.github.luasgraphicss.minemod.adapters.frame.arrays;

import io.github.luasgraphicss.minemod.codec.scene.VertexElement;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class ClientArrays {
    private static final int VERTEX_ARRAY = 0x8074;
    private static final int NORMAL_ARRAY = 0x8075;
    private static final int COLOR_ARRAY = 0x8076;
    private static final int COORDINATE_ARRAY = 0x8078;
    private static final int FIRST_UNIT_NAME = 0x84C0;
    private static final VertexElement[] ELEMENT_OF_UNIT = {VertexElement.TEXTURE, VertexElement.LIGHT};

    private final Map<VertexElement, ArrayPointer> pointers = new EnumMap<>(VertexElement.class);
    private final Map<VertexElement, Boolean> isEnabled = new EnumMap<>(VertexElement.class);
    private int unit;

    public void activateUnit(int unitName) {
        unit = unitName - FIRST_UNIT_NAME;
    }

    public void enable(int array, boolean isRead) {
        elementOf(array).ifPresent(element -> isEnabled.put(element, isRead));
    }

    public void vertices(ArrayPointer pointer) {
        pointers.put(VertexElement.POSITION, pointer);
    }

    public void colors(ArrayPointer pointer) {
        pointers.put(VertexElement.COLOR, pointer);
    }

    public void normals(ArrayPointer pointer) {
        pointers.put(VertexElement.NORMAL, pointer);
    }

    public void coordinates(ArrayPointer pointer) {
        elementOfUnit().ifPresent(element -> pointers.put(element, pointer));
    }

    public Optional<Geometry> geometry(int mode, int first, int count, ArrayBuffers buffers) {
        ArrayPointer positions = read(VertexElement.POSITION);
        if (positions == null) {
            return Optional.empty();
        }
        Map<VertexElement, ArrayPointer> others = new EnumMap<>(VertexElement.class);
        for (VertexElement element : pointers.keySet()) {
            ArrayPointer pointer = read(element);
            if (element != VertexElement.POSITION && pointer != null) {
                others.put(element, pointer);
            }
        }
        return InterleavedArrays.read(positions, others, mode, first, count, buffers);
    }

    private ArrayPointer read(VertexElement element) {
        return Boolean.TRUE.equals(isEnabled.get(element)) ? pointers.get(element) : null;
    }

    private Optional<VertexElement> elementOf(int array) {
        switch (array) {
            case VERTEX_ARRAY:
                return Optional.of(VertexElement.POSITION);
            case NORMAL_ARRAY:
                return Optional.of(VertexElement.NORMAL);
            case COLOR_ARRAY:
                return Optional.of(VertexElement.COLOR);
            case COORDINATE_ARRAY:
                return elementOfUnit();
            default:
                return Optional.empty();
        }
    }

    private Optional<VertexElement> elementOfUnit() {
        return unit >= 0 && unit < ELEMENT_OF_UNIT.length ? Optional.of(ELEMENT_OF_UNIT[unit]) : Optional.empty();
    }
}

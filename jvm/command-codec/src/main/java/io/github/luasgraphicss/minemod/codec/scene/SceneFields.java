package io.github.luasgraphicss.minemod.codec.scene;

import io.github.luasgraphicss.minemod.contract.handle.ResourceHandle;
import io.github.luasgraphicss.minemod.contract.handle.ResourceKind;
import java.nio.ByteBuffer;

final class SceneFields {
    static final int MATRIX_VALUES = 16;
    static final int DIRECTION_VALUES = 3;
    static final long NO_TEXTURE = 0;

    private SceneFields() {}

    static float[] matrix(float[] values) {
        return copyOf(values, MATRIX_VALUES, "a matrix");
    }

    static float[] direction(float[] values) {
        return copyOf(values, DIRECTION_VALUES, "a direction");
    }

    static long texture(ResourceHandle handle) {
        if (handle.kind() != ResourceKind.TEXTURE) {
            throw new IllegalArgumentException("expected a texture handle, found " + handle);
        }
        return handle.bits();
    }

    static void putFloats(ByteBuffer payload, float[] values) {
        for (float value : values) {
            payload.putFloat(value);
        }
    }

    private static float[] copyOf(float[] values, int count, String meaning) {
        if (values.length != count) {
            throw new IllegalArgumentException(meaning + " has " + count + " values, found " + values.length);
        }
        return values.clone();
    }
}

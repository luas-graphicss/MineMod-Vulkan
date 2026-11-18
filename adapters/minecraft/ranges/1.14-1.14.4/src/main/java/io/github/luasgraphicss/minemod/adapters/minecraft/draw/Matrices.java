package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import com.mojang.math.Matrix4f;
import java.nio.FloatBuffer;

public final class Matrices {
    private static final int MATRIX_VALUES = 16;

    private Matrices() {}

    public static float[] columnMajor(Matrix4f matrix) {
        FloatBuffer values = FloatBuffer.allocate(MATRIX_VALUES);
        matrix.store(values);
        return values.array();
    }
}

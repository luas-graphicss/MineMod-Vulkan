package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

public final class Matrix {
    public static final int VALUES = 16;
    private static final int EDGE = 4;
    private static final int TRANSLATION_COLUMN = 3;

    private Matrix() {}

    public static float[] identity() {
        float[] matrix = new float[VALUES];
        for (int diagonal = 0; diagonal < EDGE; diagonal++) {
            matrix[at(diagonal, diagonal)] = 1.0f;
        }
        return matrix;
    }

    public static boolean isIdentity(float[] matrix) {
        for (int column = 0; column < EDGE; column++) {
            for (int row = 0; row < EDGE; row++) {
                if (matrix[at(column, row)] != (column == row ? 1.0f : 0.0f)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static float[] product(float[] left, float[] right) {
        float[] product = new float[VALUES];
        for (int column = 0; column < EDGE; column++) {
            for (int row = 0; row < EDGE; row++) {
                float sum = 0.0f;
                for (int inner = 0; inner < EDGE; inner++) {
                    sum += left[at(inner, row)] * right[at(column, inner)];
                }
                product[at(column, row)] = sum;
            }
        }
        return product;
    }

    public static float[] translation(float x, float y, float z) {
        float[] matrix = identity();
        matrix[at(TRANSLATION_COLUMN, 0)] = x;
        matrix[at(TRANSLATION_COLUMN, 1)] = y;
        matrix[at(TRANSLATION_COLUMN, 2)] = z;
        return matrix;
    }

    public static float[] scale(float x, float y, float z) {
        float[] matrix = identity();
        matrix[at(0, 0)] = x;
        matrix[at(1, 1)] = y;
        matrix[at(2, 2)] = z;
        return matrix;
    }

    public static float[] rotation(float degrees, float x, float y, float z) {
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0f) {
            return identity();
        }
        float axisX = x / length;
        float axisY = y / length;
        float axisZ = z / length;
        double radians = Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        float rest = 1.0f - cosine;
        float[] matrix = identity();
        matrix[at(0, 0)] = axisX * axisX * rest + cosine;
        matrix[at(0, 1)] = axisY * axisX * rest + axisZ * sine;
        matrix[at(0, 2)] = axisZ * axisX * rest - axisY * sine;
        matrix[at(1, 0)] = axisX * axisY * rest - axisZ * sine;
        matrix[at(1, 1)] = axisY * axisY * rest + cosine;
        matrix[at(1, 2)] = axisZ * axisY * rest + axisX * sine;
        matrix[at(2, 0)] = axisX * axisZ * rest + axisY * sine;
        matrix[at(2, 1)] = axisY * axisZ * rest - axisX * sine;
        matrix[at(2, 2)] = axisZ * axisZ * rest + cosine;
        return matrix;
    }

    public static float[] orthographic(double left, double right, double bottom, double top, double near, double far) {
        float[] matrix = identity();
        matrix[at(0, 0)] = (float) (2.0 / (right - left));
        matrix[at(1, 1)] = (float) (2.0 / (top - bottom));
        matrix[at(2, 2)] = (float) (-2.0 / (far - near));
        matrix[at(TRANSLATION_COLUMN, 0)] = (float) (-(right + left) / (right - left));
        matrix[at(TRANSLATION_COLUMN, 1)] = (float) (-(top + bottom) / (top - bottom));
        matrix[at(TRANSLATION_COLUMN, 2)] = (float) (-(far + near) / (far - near));
        return matrix;
    }

    static int at(int column, int row) {
        return column * EDGE + row;
    }
}

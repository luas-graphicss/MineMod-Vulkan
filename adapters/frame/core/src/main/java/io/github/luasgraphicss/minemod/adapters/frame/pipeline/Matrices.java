package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

public final class Matrices {
    public static final int VALUES = 16;
    private static final int COLUMN_VALUES = 4;
    private static final double HALF_CIRCLE_DEGREES = 180.0;

    private Matrices() {}

    public static int at(int column, int row) {
        return column * COLUMN_VALUES + row;
    }

    public static float[] identity() {
        float[] matrix = new float[VALUES];
        matrix[at(0, 0)] = 1.0f;
        matrix[at(1, 1)] = 1.0f;
        matrix[at(2, 2)] = 1.0f;
        matrix[at(3, 3)] = 1.0f;
        return matrix;
    }

    public static boolean isIdentity(float[] matrix) {
        for (int column = 0; column < COLUMN_VALUES; column++) {
            for (int row = 0; row < COLUMN_VALUES; row++) {
                if (matrix[at(column, row)] != (column == row ? 1.0f : 0.0f)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static float[] product(float[] left, float[] right) {
        float[] product = new float[VALUES];
        for (int column = 0; column < COLUMN_VALUES; column++) {
            for (int row = 0; row < COLUMN_VALUES; row++) {
                float sum = 0.0f;
                for (int inner = 0; inner < COLUMN_VALUES; inner++) {
                    sum += left[at(inner, row)] * right[at(column, inner)];
                }
                product[at(column, row)] = sum;
            }
        }
        return product;
    }

    public static float[] translation(float x, float y, float z) {
        float[] matrix = identity();
        matrix[at(3, 0)] = x;
        matrix[at(3, 1)] = y;
        matrix[at(3, 2)] = z;
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
        double radians = degrees * Math.PI / HALF_CIRCLE_DEGREES;
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        float rest = 1.0f - cosine;
        float[] matrix = identity();
        matrix[at(0, 0)] = axisX * axisX * rest + cosine;
        matrix[at(0, 1)] = axisY * axisX * rest + axisZ * sine;
        matrix[at(0, 2)] = axisX * axisZ * rest - axisY * sine;
        matrix[at(1, 0)] = axisX * axisY * rest - axisZ * sine;
        matrix[at(1, 1)] = axisY * axisY * rest + cosine;
        matrix[at(1, 2)] = axisY * axisZ * rest + axisX * sine;
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
        matrix[at(3, 0)] = (float) (-(right + left) / (right - left));
        matrix[at(3, 1)] = (float) (-(top + bottom) / (top - bottom));
        matrix[at(3, 2)] = (float) (-(far + near) / (far - near));
        return matrix;
    }

    public static float[] perspective(float verticalDegrees, float aspect, float near, float far) {
        double halfAngle = verticalDegrees / 2.0 * Math.PI / HALF_CIRCLE_DEGREES;
        float depth = far - near;
        float sine = (float) Math.sin(halfAngle);
        if (depth == 0.0f || sine == 0.0f || aspect == 0.0f) {
            return identity();
        }
        float cotangent = (float) Math.cos(halfAngle) / sine;
        float[] matrix = new float[VALUES];
        matrix[at(0, 0)] = cotangent / aspect;
        matrix[at(1, 1)] = cotangent;
        matrix[at(2, 2)] = -(far + near) / depth;
        matrix[at(2, 3)] = -1.0f;
        matrix[at(3, 2)] = -2.0f * near * far / depth;
        return matrix;
    }
}

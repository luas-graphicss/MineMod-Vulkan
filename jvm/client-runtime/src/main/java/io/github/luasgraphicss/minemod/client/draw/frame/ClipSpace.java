package io.github.luasgraphicss.minemod.client.draw.frame;

public final class ClipSpace {
    private static final int MATRIX_VALUES = 16;
    private static final int COLUMN_VALUES = 4;
    private static final int DEPTH_ROW = 2;
    private static final int W_ROW = 3;
    private static final float HALF = 0.5f;

    private ClipSpace() {}

    public static float[] fromOpenGl(float[] projection) {
        if (projection.length != MATRIX_VALUES) {
            throw new IllegalArgumentException(
                    "a matrix has " + MATRIX_VALUES + " values, found " + projection.length);
        }
        float[] converted = projection.clone();
        for (int column = 0; column < MATRIX_VALUES; column += COLUMN_VALUES) {
            converted[column + DEPTH_ROW] = HALF * (projection[column + DEPTH_ROW] + projection[column + W_ROW]);
        }
        return converted;
    }
}

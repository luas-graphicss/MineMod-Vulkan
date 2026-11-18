package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

final class Directions {
    private static final int AXES = 3;

    private Directions() {}

    static float[] turnedBy(float[] matrix, float x, float y, float z) {
        float[] turned = new float[AXES];
        for (int row = 0; row < AXES; row++) {
            turned[row] = matrix[Matrix.at(0, row)] * x + matrix[Matrix.at(1, row)] * y + matrix[Matrix.at(2, row)] * z;
        }
        return normalized(turned);
    }

    static float[] turnedBack(float[] matrix, float[] direction) {
        float[] turned = new float[AXES];
        for (int column = 0; column < AXES; column++) {
            turned[column] = matrix[Matrix.at(column, 0)] * direction[0]
                    + matrix[Matrix.at(column, 1)] * direction[1]
                    + matrix[Matrix.at(column, 2)] * direction[2];
        }
        return normalized(turned);
    }

    private static float[] normalized(float[] direction) {
        float length = (float) Math.sqrt(
                direction[0] * direction[0] + direction[1] * direction[1] + direction[2] * direction[2]);
        if (length == 0.0f) {
            return direction;
        }
        return new float[] {direction[0] / length, direction[1] / length, direction[2] / length};
    }
}

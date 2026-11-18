package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

import java.nio.FloatBuffer;

public final class DirectionalLights {
    public static final int COUNT = 2;
    private static final int FIRST_NAME = 0x4000;
    private static final int POSITION = 0x1203;
    private static final int POSITION_VALUES = 4;
    private static final int DISTANCE_VALUE = 3;
    private static final int AXES = 3;
    private static final float INFINITELY_FAR = 0.0f;

    private final float[][] eyeDirections = new float[COUNT][];
    private boolean isEnabled;

    public void enable(boolean isLit) {
        isEnabled = isLit;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void parameter(int lightName, int name, FloatBuffer values, float[] modelView) {
        int light = lightName - FIRST_NAME;
        if (name != POSITION || light < 0 || light >= COUNT || values.remaining() < POSITION_VALUES) {
            return;
        }
        int first = values.position();
        if (values.get(first + DISTANCE_VALUE) == INFINITELY_FAR) {
            eyeDirections[light] = turnedBy(modelView, values.get(first), values.get(first + 1), values.get(first + 2));
        }
    }

    public boolean areShining() {
        return isEnabled && eyeDirections[0] != null && eyeDirections[1] != null;
    }

    public float[] eyeDirection(int light) {
        return eyeDirections[light].clone();
    }

    private static float[] turnedBy(float[] matrix, float x, float y, float z) {
        float[] turned = new float[AXES];
        for (int row = 0; row < AXES; row++) {
            turned[row] = matrix[Matrices.at(0, row)] * x
                    + matrix[Matrices.at(1, row)] * y
                    + matrix[Matrices.at(2, row)] * z;
        }
        float length = (float) Math.sqrt(turned[0] * turned[0] + turned[1] * turned[1] + turned[2] * turned[2]);
        if (length == 0.0f) {
            return turned;
        }
        return new float[] {turned[0] / length, turned[1] / length, turned[2] / length};
    }
}

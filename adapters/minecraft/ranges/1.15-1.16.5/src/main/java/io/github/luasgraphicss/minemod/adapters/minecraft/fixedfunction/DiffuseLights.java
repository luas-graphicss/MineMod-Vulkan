package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

import java.nio.FloatBuffer;

public final class DiffuseLights {
    public static final int COUNT = 2;
    private static final int FIRST_NAME = 0x4000;
    private static final int POSITION = 0x1203;
    private static final int POSITION_VALUES = 4;
    private static final int DISTANCE_VALUE = 3;
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
            eyeDirections[light] =
                    Directions.turnedBy(modelView, values.get(first), values.get(first + 1), values.get(first + 2));
        }
    }

    public boolean areShining() {
        return isEnabled && eyeDirections[0] != null && eyeDirections[1] != null;
    }

    public float[] directionSeenFrom(float[] modelView, int light) {
        return Directions.turnedBack(modelView, eyeDirections[light]);
    }
}

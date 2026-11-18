package io.github.luasgraphicss.minemod.adapters.minecraft.fixedfunction;

public final class FogSettings {
    private static final int LINEAR = 0x2601;
    private static final int EXPONENTIAL = 0x0800;
    private static final int COLOR = 0x0B66;
    private static final int COLOR_VALUES = 4;
    private static final float EXPONENTIAL_REACH = 3.0f;
    private static final float SQUARED_REACH = 2.0f;
    private static final float NO_DISTANCE = 0.0f;

    private final float[] color = new float[COLOR_VALUES];
    private boolean isEnabled;
    private int mode = EXPONENTIAL;
    private float density = 1.0f;
    private float linearStart;
    private float linearEnd = 1.0f;

    public void enable(boolean isFogged) {
        isEnabled = isFogged;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void mode(int equation) {
        mode = equation;
    }

    public void density(float thickness) {
        density = thickness;
    }

    public void start(float distance) {
        linearStart = distance;
    }

    public void end(float distance) {
        linearEnd = distance;
    }

    public void parameter(int name, float[] values) {
        if (name == COLOR && values.length >= COLOR_VALUES) {
            System.arraycopy(values, 0, color, 0, COLOR_VALUES);
        }
    }

    public float[] color() {
        return color.clone();
    }

    public float start() {
        return mode == LINEAR ? linearStart : NO_DISTANCE;
    }

    public float end() {
        if (mode == LINEAR) {
            return linearEnd;
        }
        if (!(density > 0.0f)) {
            return NO_DISTANCE;
        }
        return (mode == EXPONENTIAL ? EXPONENTIAL_REACH : SQUARED_REACH) / density;
    }
}

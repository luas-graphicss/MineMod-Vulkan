package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

import java.nio.FloatBuffer;

public final class FogState {
    private static final int MODE = 0x0B65;
    private static final int DENSITY = 0x0B62;
    private static final int START = 0x0B63;
    private static final int END = 0x0B64;
    private static final int COLOR = 0x0B66;
    private static final int LINEAR = 0x2601;
    private static final int EXPONENTIAL = 0x0800;
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

    public void parameter(int name, int value) {
        if (name == MODE) {
            mode = value;
        }
    }

    public void parameter(int name, float value) {
        switch (name) {
            case DENSITY:
                density = value;
                break;
            case START:
                linearStart = value;
                break;
            case END:
                linearEnd = value;
                break;
            default:
                break;
        }
    }

    public void parameter(int name, FloatBuffer values) {
        if (name != COLOR || values.remaining() < COLOR_VALUES) {
            return;
        }
        for (int value = 0; value < COLOR_VALUES; value++) {
            color[value] = values.get(values.position() + value);
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

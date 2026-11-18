package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

import io.github.luasgraphicss.minemod.client.draw.state.RenderState;

public final class SurfaceState {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int LESS = 0x0201;
    private static final int ONE_MINUS_SOURCE_COLOR = 0x0301;
    private static final int ONE_MINUS_DESTINATION_COLOR = 0x0307;
    private static final int COLOR_VALUES = 4;

    private final float[] clearColor = new float[COLOR_VALUES];
    private boolean blends;
    private int sourceColor = ONE;
    private int destinationColor = ZERO;
    private boolean testsDepth;
    private int depthFunction = LESS;
    private boolean writesDepth = true;
    private boolean writesColor = true;
    private boolean cullsBackFaces;
    private boolean offsetsDepth;
    private float offsetFactor;
    private float offsetUnits;
    private boolean combinesByLogic;

    public void blend(boolean isEnabled) {
        blends = isEnabled;
    }

    public void blendFactors(int source, int destination) {
        sourceColor = source;
        destinationColor = destination;
    }

    public void colorLogic(boolean isEnabled) {
        combinesByLogic = isEnabled;
    }

    public void depthTest(boolean isEnabled) {
        testsDepth = isEnabled;
    }

    public void depthFunction(int function) {
        depthFunction = function;
    }

    public void depthWrite(boolean isWritten) {
        writesDepth = isWritten;
    }

    public void colorWrite(boolean isWritten) {
        writesColor = isWritten;
    }

    public void cullBackFaces(boolean isCulled) {
        cullsBackFaces = isCulled;
    }

    public void depthOffset(boolean isEnabled) {
        offsetsDepth = isEnabled;
    }

    public void depthOffset(float factor, float units) {
        offsetFactor = factor;
        offsetUnits = units;
    }

    public void clearColor(float red, float green, float blue, float alpha) {
        clearColor[0] = red;
        clearColor[1] = green;
        clearColor[2] = blue;
        clearColor[3] = alpha;
    }

    public float[] clearColor() {
        return clearColor.clone();
    }

    public RenderState renderState() {
        RenderState.Builder state = RenderState.builder()
                .depthWrite(writesDepth)
                .colorWrite(writesColor)
                .cullBackFaces(cullsBackFaces);
        if (combinesByLogic) {
            state.blend(ONE_MINUS_DESTINATION_COLOR, ONE_MINUS_SOURCE_COLOR);
        } else if (blends) {
            state.blend(sourceColor, destinationColor);
        }
        if (testsDepth) {
            state.depthTest(depthFunction);
        }
        if (offsetsDepth) {
            state.polygonOffset(offsetFactor, offsetUnits);
        }
        return state.build();
    }

    boolean[] switches() {
        return new boolean[] {blends, testsDepth, cullsBackFaces, offsetsDepth, combinesByLogic};
    }

    void restore(boolean[] switches) {
        blends = switches[0];
        testsDepth = switches[1];
        cullsBackFaces = switches[2];
        offsetsDepth = switches[3];
        combinesByLogic = switches[4];
    }
}

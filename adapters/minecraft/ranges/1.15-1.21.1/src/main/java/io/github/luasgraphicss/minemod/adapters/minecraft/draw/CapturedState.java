package io.github.luasgraphicss.minemod.adapters.minecraft.draw;

import io.github.luasgraphicss.minemod.client.draw.frame.GameDraw;
import io.github.luasgraphicss.minemod.client.draw.state.RenderState;

public final class CapturedState {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int LESS = 0x0201;
    private static final int ONE_MINUS_SOURCE_COLOR = 0x0301;
    private static final int ONE_MINUS_DESTINATION_COLOR = 0x0307;

    private final int[] clipBox = new int[4];
    private final float[] clearColor = new float[4];
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
    private boolean clips;
    private boolean combinesByLogic;
    private Object target;

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

    public void clip(boolean isEnabled) {
        clips = isEnabled;
    }

    public void clipBox(int left, int bottom, int width, int height) {
        clipBox[0] = left;
        clipBox[1] = bottom;
        clipBox[2] = Math.max(0, width);
        clipBox[3] = Math.max(0, height);
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

    public void drawInto(Object renderTarget) {
        target = renderTarget;
    }

    public Object target() {
        return target;
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

    public void clipInto(GameDraw.Builder draw) {
        if (clips) {
            draw.clip(clipBox[0], clipBox[1], clipBox[2], clipBox[3]);
        }
    }
}

package io.github.luasgraphicss.minemod.adapters.frame.pipeline;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.IntSupplier;

public final class FixedPipeline {
    private final TextureUnits units;
    private final MatrixStacks matrices;
    private final AlphaTest alphaTest = new AlphaTest();
    private final FogState fog = new FogState();
    private final DirectionalLights lights = new DirectionalLights();
    private final SurfaceState surface = new SurfaceState();
    private final Deque<EnabledSteps> savedSteps = new ArrayDeque<>();
    private final float[] color = {1.0f, 1.0f, 1.0f, 1.0f};
    private float lineWidth = 1.0f;

    public FixedPipeline(IntSupplier activeUnitName) {
        this.units = new TextureUnits(activeUnitName);
        this.matrices = new MatrixStacks(units);
    }

    public TextureUnits units() {
        return units;
    }

    public MatrixStacks matrices() {
        return matrices;
    }

    public AlphaTest alphaTest() {
        return alphaTest;
    }

    public FogState fog() {
        return fog;
    }

    public DirectionalLights lights() {
        return lights;
    }

    public SurfaceState surface() {
        return surface;
    }

    public void enable(int capability, boolean isEnabled) {
        EnableSwitches.set(this, capability, isEnabled);
    }

    public void color(float red, float green, float blue, float alpha) {
        color[0] = red;
        color[1] = green;
        color[2] = blue;
        color[3] = alpha;
    }

    public float[] color() {
        return color.clone();
    }

    public void lineWidth(float pixels) {
        lineWidth = pixels;
    }

    public float lineWidth() {
        return lineWidth;
    }

    public void saveSteps() {
        savedSteps.push(EnabledSteps.of(this));
    }

    public void restoreSteps() {
        if (!savedSteps.isEmpty()) {
            savedSteps.pop().restoreTo(this);
        }
    }
}
